package id.andriawan.cofinance.pages.splash

import id.andriawan.cofinance.data.crypto.DataKey
import id.andriawan.cofinance.data.crypto.DeviceKeyWrapper
import id.andriawan.cofinance.data.crypto.FakeDeviceKeyVault
import id.andriawan.cofinance.data.crypto.KeyMaterialDocument
import id.andriawan.cofinance.data.crypto.KeyWrapType
import id.andriawan.cofinance.data.crypto.PinKeyWrapper
import id.andriawan.cofinance.data.keyring.EncryptionSessionState
import id.andriawan.cofinance.data.keyring.InMemoryEncryptionSession
import id.andriawan.cofinance.data.lock.AppLock
import id.andriawan.cofinance.data.lock.BiometricUnlock
import id.andriawan.cofinance.data.lock.FailedAttemptGuard
import id.andriawan.cofinance.data.lock.FakeBiometricKeyBox
import id.andriawan.cofinance.data.lock.KeyValueAutoLockSettings
import id.andriawan.cofinance.data.lock.FakeFailedAttemptStore
import id.andriawan.cofinance.data.lock.FakeLocalKeyMaterialStore
import id.andriawan.cofinance.data.lock.LocalKeyMaterialDestroyer
import id.andriawan.cofinance.data.migration.EncryptionSetup
import id.andriawan.cofinance.data.migration.FakePlaintextFinanceDocumentStore
import id.andriawan.cofinance.data.migration.PlaintextMigration
import id.andriawan.cofinance.data.migration.PlaintextRecordMigrator
import id.andriawan.cofinance.data.model.document.AccountDocument
import id.andriawan.cofinance.data.remote.FinanceCollection
import id.andriawan.cofinance.data.remote.KeyMaterialGate
import id.andriawan.cofinance.data.remote.MutableSessionPolicy
import id.andriawan.cofinance.data.remote.keyMaterialWith
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

/**
 * What the launch sequence decides, which is the seam every other piece of this change hangs off.
 *
 * The three properties under test here are the ones that are only visible at this level. Each
 * component below already works in isolation, and the failures this file is written against are
 * failures of composition: a local-only user who never signed in being shown a phrase to write
 * down, a relaunching device being sent through setup a second time because nothing read the
 * durable key material, and migration never being invoked because no caller owned it.
 */
class LaunchRoutingTest {

    private val vault = FakeDeviceKeyVault()
    private val deviceKeyWrapper = DeviceKeyWrapper(vault)
    private val pinKeyWrapper = PinKeyWrapper(vault)

    private val sessionPolicy = MutableSessionPolicy()
    private val session = InMemoryEncryptionSession()
    private val keyMaterial = FakeLocalKeyMaterialStore()
    private val attemptStore = FakeFailedAttemptStore()
    private val store = FakePlaintextFinanceDocumentStore()

    private var synchronizations = 0

    // ------------------------------------------------------------------ a local-only user

    @Test
    fun aUserWhoNeverSignedInGoesStraightToTheMainExperience() = runTest {
        sessionPolicy.userId = null

        viewModel.launch()

        assertEquals(LaunchRoute.Main, route())
        assertEquals(EncryptionSessionState.SetupIncomplete, session.state.value)
    }

    @Test
    fun aLocalOnlyUserIsNeverShownSetupOrUnlockEvenWithKeyMaterialOnTheDevice() = runTest {
        // Key material from an earlier signed-in period, left on the device after signing out. Even
        // this must not produce an unlock prompt: with no account there is no synchronized copy to
        // protect, and no way to reach one.
        sessionPolicy.userId = null
        keyMaterial.write(documentWith(DataKey.generate(), pin = "123456"))

        viewModel.launch()

        assertEquals(LaunchRoute.Main, route())
    }

    @Test
    fun aLocalOnlyUserIsNotBlockedByMigration() = runTest {
        sessionPolicy.userId = null
        store.seedPlaintext(FinanceCollection.ACCOUNTS, "account-cash", AccountDocument.serializer(), CASH)

        viewModel.launch()

        assertEquals(LaunchRoute.Main, route())
        assertTrue(store.operations.isEmpty(), "migration touched a signed-out user's documents")
    }

    // ------------------------------------------------------------------ a signed-in user

    @Test
    fun aSignedInDeviceWithNoKeyMaterialGoesToSetup() = runTest {
        viewModel.launch()

        assertEquals(LaunchRoute.EncryptionSetup, route())
        assertEquals(0, synchronizations, "the account was synchronized before setup ran")
    }

    @Test
    fun aRelaunchWithAPinGoesToUnlockRatherThanIntoTheApp() = runTest {
        val dataKey = DataKey.generate()
        keyMaterial.write(documentWith(dataKey, pin = "123456"))

        viewModel.launch()

        assertEquals(LaunchRoute.Unlock, route())
        // Locked rather than unlocked: the device wrap is present and would have opened the
        // session, and the PIN is what stops it from being used without asking.
        assertEquals(EncryptionSessionState.Locked, session.state.value)
        assertNull(session.dataKeyOrNull())
        assertEquals(0, synchronizations, "finance data was synchronized before the unlock")
    }

    @Test
    fun aRelaunchWithNoPinOpensTheSessionFromTheDeviceWrapAndReachesMain() = runTest {
        val dataKey = DataKey.generate()
        keyMaterial.write(documentWith(dataKey))

        viewModel.launch()

        assertEquals(LaunchRoute.Main, route())
        assertEquals(EncryptionSessionState.Unlocked, session.state.value)
        assertEquals(dataKey.exportRawBytes().toList(), session.requireDataKey().exportRawBytes().toList())
    }

    @Test
    fun anAlreadyUnlockedSessionReachesMainWithoutRereadingAnything() = runTest {
        val dataKey = DataKey.generate()
        session.markSetUp()
        session.unlock(dataKey)

        viewModel.launch()

        assertEquals(LaunchRoute.Main, route())
        assertEquals(1, synchronizations)
    }

    @Test
    fun aDeviceWrapThatNoLongerOpensAndNoPinGoesToSetupRatherThanADeadEnd() = runTest {
        keyMaterial.write(documentWith(DataKey.generate()))
        // The platform key material behind the device wrap is gone, which is what an OS-level key
        // invalidation leaves. Without this branch the session would sit at Locked with no PIN and
        // no route out of it.
        vault.destroyKeyMaterial()

        viewModel.launch()

        assertEquals(LaunchRoute.EncryptionSetup, route())
        assertEquals(EncryptionSessionState.SetupIncomplete, session.state.value)
    }

    // ------------------------------------------------------------------ migration

    @Test
    fun migrationRunsBeforeFinanceDataIsReachable() = runTest {
        val dataKey = DataKey.generate()
        session.markSetUp()
        session.unlock(dataKey)
        store.seedKeyMaterial(keyMaterialWith(dataKey.id, KeyWrapType.RecoveryPhrase))
        store.seedPlaintext(FinanceCollection.ACCOUNTS, "account-cash", AccountDocument.serializer(), CASH)

        viewModel.launch()

        assertEquals(LaunchRoute.Main, route())
        assertEquals(
            listOf("write accounts/account-cash", "delete accounts/account-cash"),
            store.operations,
            "the record was not converted, or not in the encrypt-then-delete order"
        )
        assertEquals(1, synchronizations)
    }

    @Test
    fun aUserWithNoPlaintextRecordsIsNotBlocked() = runTest {
        val dataKey = DataKey.generate()
        session.markSetUp()
        session.unlock(dataKey)
        store.seedKeyMaterial(keyMaterialWith(dataKey.id, KeyWrapType.RecoveryPhrase))

        viewModel.launch()

        assertEquals(LaunchRoute.Main, route())
        assertTrue(store.operations.isEmpty(), "an account with nothing to convert was written to")
        assertEquals(1, synchronizations)
    }

    @Test
    fun aFailedMigrationHoldsTheLaunchAndOffersARetryThatFinishesIt() = runTest {
        val dataKey = DataKey.generate()
        session.markSetUp()
        session.unlock(dataKey)
        store.seedKeyMaterial(keyMaterialWith(dataKey.id, KeyWrapType.RecoveryPhrase))
        store.seedPlaintext(FinanceCollection.ACCOUNTS, "account-cash", AccountDocument.serializer(), CASH)
        store.interruptAfter(0)

        viewModel.launch()

        assertNull(route(), "a failed migration let the user through to finance data")
        assertEquals(LaunchPhase.MigrationFailed, viewModel.uiState.value.phase)
        assertEquals(0, synchronizations)

        store.interruptAfter(Int.MAX_VALUE)
        viewModel.launch()

        assertEquals(LaunchRoute.Main, route())
        assertEquals(
            listOf("write accounts/account-cash", "delete accounts/account-cash"),
            store.operations
        )
    }

    // ------------------------------------------------------------------ harness

    /**
     * The launch sequence over the real lock and the real migration, with only the three things a
     * host test cannot have faked: platform key storage, the failed-attempt file, and Firestore.
     *
     * The lock and the migration are the real classes on purpose. The decisions under test are
     * about how they compose — whether a PIN wrap stops the device wrap from being used, whether a
     * scan that finds nothing lets the user through — and a stubbed lock or a stubbed migration
     * would assert the test's own beliefs about them rather than their behaviour.
     */
    private val viewModel: SplashViewModel by lazy {
        val lock = AppLock(
            session = session,
            keyMaterial = keyMaterial,
            pinKeyWrapper = pinKeyWrapper,
            attempts = FailedAttemptGuard(
                attempts = attemptStore,
                keyMaterial = keyMaterial,
                destroyer = LocalKeyMaterialDestroyer(keyMaterial, vault, session)
            ),
            biometrics = BiometricUnlock(FakeBiometricKeyBox()),
            autoLockSettings = KeyValueAutoLockSettings(readStoredId = { null }, writeStoredId = {})
        )

        SplashViewModel(
            sessionPolicy = sessionPolicy,
            synchronizer = { synchronizations++ },
            encryptionSession = session,
            localKeyMaterialStore = keyMaterial,
            deviceKeyWrapper = deviceKeyWrapper,
            appLock = lock,
            migration = PlaintextMigration(
                migrator = PlaintextRecordMigrator(store),
                keyMaterialGate = KeyMaterialGate(store, sessionPolicy),
                encryptionSession = session,
                encryptionSetup = EncryptionSetup { session.requireDataKey() },
                sessionPolicy = sessionPolicy
            )
        )
    }

    private fun route(): LaunchRoute? = viewModel.uiState.value.route

    /** The key material an earlier setup left on this device, optionally behind a PIN. */
    private suspend fun documentWith(dataKey: DataKey, pin: String? = null) = KeyMaterialDocument(
        keyMaterialVersion = KeyMaterialDocument.CURRENT_VERSION,
        wrappedKeys = buildList {
            add(deviceKeyWrapper.wrap(dataKey))
            if (pin != null) add(pinKeyWrapper.wrap(dataKey, pin))
        }
    )

    private companion object {
        val CASH = AccountDocument(
            name = "Dompet Tunai",
            group = "Cash",
            balance = 4_250_000,
            accountType = "CASH",
            createdAt = "2026-01-05T09:00:00Z"
        )
    }
}
