package id.andriawan.cofinance.di

import id.andriawan.cofinance.data.crypto.PinKeyWrapper
import id.andriawan.cofinance.data.crypto.RecoveryPhraseVault
import id.andriawan.cofinance.data.crypto.createRecoveryPhraseVault
import id.andriawan.cofinance.data.keyring.EncryptionSession
import id.andriawan.cofinance.data.lock.AppLock
import id.andriawan.cofinance.data.lock.AutoLockController
import id.andriawan.cofinance.data.lock.AutoLockSettings
import id.andriawan.cofinance.data.lock.BiometricKeyBox
import id.andriawan.cofinance.data.lock.BiometricUnlock
import id.andriawan.cofinance.data.lock.FailedAttemptGuard
import id.andriawan.cofinance.data.lock.FailedAttemptStore
import id.andriawan.cofinance.data.lock.LocalKeyMaterialDestroyer
import id.andriawan.cofinance.data.lock.LocalKeyMaterialStore
import id.andriawan.cofinance.data.lock.StoredLocalKeyMaterialStore
import id.andriawan.cofinance.data.lock.createAutoLockSettings
import id.andriawan.cofinance.data.lock.createBiometricKeyBox
import id.andriawan.cofinance.data.lock.createFailedAttemptStore
import id.andriawan.cofinance.data.lock.createKeyMaterialStorage
import id.andriawan.cofinance.data.migration.EncryptionSetup
import id.andriawan.cofinance.data.migration.FirestorePlaintextFinanceDocumentStore
import id.andriawan.cofinance.data.migration.PlaintextFinanceDocumentStore
import id.andriawan.cofinance.data.migration.PlaintextMigration
import id.andriawan.cofinance.data.migration.PlaintextRecordMigrator
import id.andriawan.cofinance.data.sync.FirebaseSyncCoordinator
import id.andriawan.cofinance.domain.usecases.authentications.FetchUserUseCase
import id.andriawan.cofinance.pages.splash.LaunchSynchronizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

/**
 * The app lock, the local key material it protects, and the one-time plaintext migration.
 *
 * These are one module rather than three because they form a single chain at launch: the durable
 * key material store is what tells a relaunching device that setup already happened, the lock is
 * what stands between that device and its finance data, and migration is what a signed-in user's
 * first encrypted launch has to finish before anything else is reachable.
 *
 * Everything platform-specific is reached through a `create…` function rather than constructed
 * here, so this module says nothing about Keystore or Keychain and stays readable as wiring.
 */
val securityModule = module {

    // ------------------------------------------------------------------------------------------
    // Local key material
    // ------------------------------------------------------------------------------------------

    // Durable, and the only local key material store there is. The device wrap has to outlive the
    // process: the backend holds the recovery-phrase wrap alone, so if this were per-process every
    // relaunch would present setup or restore.
    single<LocalKeyMaterialStore> { StoredLocalKeyMaterialStore(createKeyMaterialStorage()) }

    // ------------------------------------------------------------------------------------------
    // The lock
    // ------------------------------------------------------------------------------------------

    single<FailedAttemptStore> { createFailedAttemptStore() }
    single<BiometricKeyBox> { createBiometricKeyBox() }
    single<AutoLockSettings> { createAutoLockSettings() }
    single<RecoveryPhraseVault> { createRecoveryPhraseVault() }
    single { PinKeyWrapper(get()) }
    singleOf(::BiometricUnlock)
    singleOf(::LocalKeyMaterialDestroyer)
    // Constructed explicitly: the trailing LockClock has a default that tests replace and that the
    // graph has no business resolving.
    single { FailedAttemptGuard(get(), get(), get()) }
    singleOf(::AppLock)

    // The controller owns a timer that must outlive any one screen, so it gets a scope of its own
    // rather than a composition's or a view model's.
    single {
        AutoLockController(
            session = get<EncryptionSession>(),
            settings = get(),
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        )
    }

    // ------------------------------------------------------------------------------------------
    // Plaintext migration
    // ------------------------------------------------------------------------------------------

    singleOf(::FirestorePlaintextFinanceDocumentStore) { bind<PlaintextFinanceDocumentStore>() }
    single { PlaintextRecordMigrator(get(), get()) }

    /**
     * Migration's seam onto encryption setup.
     *
     * Setup is a screen with a mandatory phrase confirmation, and migration cannot present one, so
     * the launch path runs setup *before* migration and this asserts the outcome rather than
     * performing it. Asking the session for the data key is the assertion: it throws when setup did
     * not complete, which surfaces as a retryable migration failure instead of records being sealed
     * under key material that was never published.
     */
    single<EncryptionSetup> {
        val session = get<EncryptionSession>()
        EncryptionSetup { session.requireDataKey() }
    }

    single { PlaintextMigration(get(), get(), get<EncryptionSession>(), get(), get()) }

    // ------------------------------------------------------------------------------------------
    // Launch
    // ------------------------------------------------------------------------------------------

    // The signed-in work the launch sequence runs once nothing is left to unlock or convert. It is
    // a seam so the sequence stays testable against fakes; both collaborators reach Firebase.
    single<LaunchSynchronizer> {
        val syncCoordinator = get<FirebaseSyncCoordinator>()
        val fetchUser = get<FetchUserUseCase>()
        LaunchSynchronizer {
            syncCoordinator.syncDataAfterSignIn()
            fetchUser.execute().collect { /* the account lands in the repository */ }
        }
    }
}
