package id.andriawan.cofinance.pages.lock

import id.andriawan.cofinance.data.crypto.DataKey
import id.andriawan.cofinance.data.crypto.FakeDeviceKeyVault
import id.andriawan.cofinance.data.crypto.InMemoryRecoveryPhraseVault
import id.andriawan.cofinance.data.crypto.KeyMaterialDocument
import id.andriawan.cofinance.data.crypto.PinKeyWrapper
import id.andriawan.cofinance.data.crypto.RecoveryPhrase
import id.andriawan.cofinance.data.crypto.RecoveryPhraseKeyWrapper
import id.andriawan.cofinance.data.keyring.InMemoryEncryptionSession
import id.andriawan.cofinance.data.lock.AppLock
import id.andriawan.cofinance.data.lock.BiometricPromptText
import id.andriawan.cofinance.data.lock.BiometricUnlock
import id.andriawan.cofinance.data.lock.FailedAttemptGuard
import id.andriawan.cofinance.data.lock.FakeBiometricKeyBox
import id.andriawan.cofinance.data.lock.FakeFailedAttemptStore
import id.andriawan.cofinance.data.lock.FakeLocalKeyMaterialStore
import id.andriawan.cofinance.data.lock.KeyValueAutoLockSettings
import id.andriawan.cofinance.data.lock.LocalKeyMaterialDestroyer
import id.andriawan.cofinance.data.lock.MutableLockClock

/**
 * A device, wired the way the app wires one, for the two lock screens to be driven against.
 *
 * [AppLock] is the real class over the real PIN derivation, because everything these screens are
 * judged on — that a wrong PIN reports a real attempt count, that the fifth failure carries a real
 * delay, that the tenth destroys the local key material, that re-display needs the PIN even while
 * unlocked — is a property of the lock rather than of the screen. Faking it would leave the screens
 * asserting against numbers the test itself made up.
 *
 * Only the three platform ports are doubles: where the counter lives, where the key material lives,
 * and what the biometric hardware does. Those are exactly the things a host test cannot have.
 */
class LockFixture {

    val clock: MutableLockClock = MutableLockClock(millis = 5_000_000)
    val deviceVault: FakeDeviceKeyVault = FakeDeviceKeyVault()
    val pinKeyWrapper: PinKeyWrapper = PinKeyWrapper(deviceVault)
    val phraseKeyWrapper: RecoveryPhraseKeyWrapper = RecoveryPhraseKeyWrapper()
    val attemptStore: FakeFailedAttemptStore = FakeFailedAttemptStore()
    val keyMaterial: FakeLocalKeyMaterialStore = FakeLocalKeyMaterialStore()
    val biometricBox: FakeBiometricKeyBox = FakeBiometricKeyBox()
    val session: InMemoryEncryptionSession = InMemoryEncryptionSession()
    val phraseVault: InMemoryRecoveryPhraseVault = InMemoryRecoveryPhraseVault()

    private val storedSettings: MutableMap<String, String> = mutableMapOf()

    /** The real settings implementation over a map, so a chosen timeout actually persists. */
    val autoLockSettings: KeyValueAutoLockSettings = KeyValueAutoLockSettings(
        readStoredId = { storedSettings[TIMEOUT_KEY] },
        writeStoredId = { storedSettings[TIMEOUT_KEY] = it }
    )

    val appLock: AppLock = AppLock(
        session = session,
        keyMaterial = keyMaterial,
        pinKeyWrapper = pinKeyWrapper,
        attempts = guard(),
        biometrics = BiometricUnlock(biometricBox),
        autoLockSettings = autoLockSettings
    )

    val prompt: BiometricPromptText =
        BiometricPromptText(title = "Unlock", negativeButtonLabel = "Use PIN")

    /**
     * Brings this device to the state the screens are shown in: setup completed, and locked.
     *
     * [pin] of null is the device that finished setup but has not chosen a PIN, which is what
     * restore leaves behind and what the settings section offers to fix.
     */
    suspend fun completeSetup(pin: String? = PIN): SetUpDevice {
        val dataKey = DataKey.generate()
        val phrase = RecoveryPhrase.generate()
        val wraps = buildList {
            add(phraseKeyWrapper.wrap(dataKey, phrase))
            if (pin != null) add(pinKeyWrapper.wrap(dataKey, pin))
        }
        keyMaterial.write(
            KeyMaterialDocument(
                keyMaterialVersion = KeyMaterialDocument.CURRENT_VERSION,
                wrappedKeys = wraps
            )
        )
        phraseVault.store(phrase, dataKey)
        session.markSetUp()
        // Setup arms the counter, which is what makes a later absence of one detectable.
        if (pin != null) guard().arm()
        return SetUpDevice(dataKey, phrase)
    }

    /** Unlocks the session without going through the screens, for "already unlocked" cases. */
    fun unlockSessionDirectly(dataKey: DataKey) = session.unlock(dataKey)

    private fun guard(): FailedAttemptGuard = FailedAttemptGuard(
        attempts = attemptStore,
        keyMaterial = keyMaterial,
        destroyer = LocalKeyMaterialDestroyer(keyMaterial, deviceVault, session),
        clock = clock
    )

    class SetUpDevice(val dataKey: DataKey, val phrase: RecoveryPhrase)

    companion object {
        const val PIN: String = "246813"
        const val OTHER_PIN: String = "135790"
        const val WRONG_PIN: String = "999999"
        private const val TIMEOUT_KEY = "auto_lock_timeout"
    }
}
