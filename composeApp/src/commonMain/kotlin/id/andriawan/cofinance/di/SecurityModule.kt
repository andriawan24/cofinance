package id.andriawan.cofinance.di

import id.andriawan.cofinance.data.crypto.PinKeyWrapper
import id.andriawan.cofinance.data.crypto.RecoveryPhraseExporter
import id.andriawan.cofinance.data.crypto.RecoveryPhraseVault
import id.andriawan.cofinance.data.crypto.createRecoveryPhraseExporter
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
 * The app lock and the local key material it protects.
 *
 * These are one module rather than two because they form a single chain at launch: the durable key
 * material store is what tells a relaunching device that setup already happened, and the lock is
 * what stands between that device and its finance data.
 *
 * Everything platform-specific is reached through a `create…` function rather than constructed
 * here, so this module says nothing about Keystore or Keychain and stays readable as wiring.
 */
val securityModule = module {

    // ------------------------------------------------------------------------------------------
    // Local key material
    // ------------------------------------------------------------------------------------------
    single<LocalKeyMaterialStore> { StoredLocalKeyMaterialStore(createKeyMaterialStorage()) }

    // ------------------------------------------------------------------------------------------
    // App Lock
    // ------------------------------------------------------------------------------------------
    single<FailedAttemptStore> { createFailedAttemptStore() }
    single<BiometricKeyBox> { createBiometricKeyBox() }
    single<AutoLockSettings> { createAutoLockSettings() }
    single<RecoveryPhraseVault> { createRecoveryPhraseVault() }
    single<RecoveryPhraseExporter> { createRecoveryPhraseExporter() }
    single { PinKeyWrapper(get()) }
    singleOf(::BiometricUnlock)
    singleOf(::LocalKeyMaterialDestroyer)
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
