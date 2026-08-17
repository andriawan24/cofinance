package id.andriawan.cofinance.data.lock

import id.andriawan.cofinance.data.crypto.DataKey
import id.andriawan.cofinance.data.crypto.FakeDeviceKeyVault
import id.andriawan.cofinance.data.crypto.KeyMaterialDocument
import id.andriawan.cofinance.data.crypto.KeyWrapType
import id.andriawan.cofinance.data.crypto.PinKeyWrapper
import id.andriawan.cofinance.data.crypto.RecoveryPhrase
import id.andriawan.cofinance.data.crypto.RecoveryPhraseKeyWrapper
import id.andriawan.cofinance.data.keyring.EncryptionSessionState
import id.andriawan.cofinance.data.keyring.InMemoryEncryptionSession
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The lock end to end, over the real PIN derivation and a software device vault.
 *
 * `PinKeyWrapper` and `RecoveryPhraseKeyWrapper` are the production classes here rather than fakes,
 * because the claims under test are about what actually opens the data key: that a wrong PIN
 * produces nothing, that ten of them leave the recovery phrase working, and that biometric unlock
 * is a second door onto the same key rather than a way around the PIN.
 */
class AppLockTest {

    private val vault = FakeDeviceKeyVault()
    private val pinKeyWrapper = PinKeyWrapper(vault)
    private val phraseKeyWrapper = RecoveryPhraseKeyWrapper()
    private val attemptStore = FakeFailedAttemptStore()
    private val keyMaterial = FakeLocalKeyMaterialStore()
    private val biometricBox = FakeBiometricKeyBox()
    private val session = InMemoryEncryptionSession()
    private val clock = MutableLockClock(millis = 5_000_000)

    private val prompt = BiometricPromptText(title = "Unlock", negativeButtonLabel = "Use PIN")

    private fun appLock(): AppLock = AppLock(
        session = session,
        keyMaterial = keyMaterial,
        pinKeyWrapper = pinKeyWrapper,
        attempts = FailedAttemptGuard(
            attempts = attemptStore,
            keyMaterial = keyMaterial,
            destroyer = LocalKeyMaterialDestroyer(keyMaterial, vault, session),
            clock = clock
        ),
        biometrics = BiometricUnlock(biometricBox),
        autoLockSettings = KeyValueAutoLockSettings({ null }, {})
    )

    /** Sets up a device that has completed setup and has [pin] in effect. */
    private suspend fun setUpDevice(pin: String = PIN): Fixture {
        val dataKey = DataKey.generate()
        val phrase = RecoveryPhrase.generate()
        val document = KeyMaterialDocument(
            keyMaterialVersion = KeyMaterialDocument.CURRENT_VERSION,
            wrappedKeys = listOf(
                phraseKeyWrapper.wrap(dataKey, phrase),
                pinKeyWrapper.wrap(dataKey, pin)
            )
        )
        keyMaterial.write(document)
        session.markSetUp()
        val lock = appLock()
        // Setup arms the counter, which is what makes a later absence of one detectable.
        FailedAttemptGuard(
            attempts = attemptStore,
            keyMaterial = keyMaterial,
            destroyer = LocalKeyMaterialDestroyer(keyMaterial, vault, session),
            clock = clock
        ).arm()
        return Fixture(lock, dataKey, phrase, document)
    }

    private class Fixture(
        val lock: AppLock,
        val dataKey: DataKey,
        val phrase: RecoveryPhrase,
        val document: KeyMaterialDocument
    )

    @Test
    fun theCorrectPinUnlocksTheSession() = runTest {
        val fixture = setUpDevice()

        assertEquals(PinUnlockResult.Unlocked, fixture.lock.unlockWithPin(PIN))

        assertEquals(EncryptionSessionState.Unlocked, session.state.value)
        assertEquals(fixture.dataKey.id, session.dataKeyOrNull()?.id)
    }

    @Test
    fun anIncorrectPinYieldsNoKeyAndCountsAgainstTheThreshold() = runTest {
        val fixture = setUpDevice()

        val result = assertIs<PinUnlockResult.Incorrect>(fixture.lock.unlockWithPin("000000"))

        assertEquals(9, result.attemptsRemaining)
        assertEquals(EncryptionSessionState.Locked, session.state.value)
        assertNull(session.dataKeyOrNull())
    }

    @Test
    fun aCorrectPinAfterFailuresResetsTheCounter() = runTest {
        val fixture = setUpDevice()
        repeat(3) { fixture.lock.unlockWithPin("000000") }

        assertEquals(PinUnlockResult.Unlocked, fixture.lock.unlockWithPin(PIN))

        val stored = assertIs<StoredFailedAttempts.Recorded>(attemptStore.stored)
        assertEquals(0, stored.record.consecutiveFailures)
    }

    @Test
    fun theFifthWrongPinIsThrottledBeforeAnythingIsDerived() = runTest {
        val fixture = setUpDevice()
        repeat(4) { fixture.lock.unlockWithPin("000000") }

        val throttled = assertIs<PinUnlockResult.Throttled>(fixture.lock.unlockWithPin(PIN))

        // Even the correct PIN is refused during the wait, and the session stays locked.
        assertTrue(throttled.remaining.inWholeSeconds in 1..30)
        assertEquals(EncryptionSessionState.Locked, session.state.value)
    }

    @Test
    fun tenWrongPinsDestroyLocalKeyMaterialAndThePhraseStillRestoresTheData() = runTest {
        val fixture = setUpDevice()
        // The uploaded copy, captured as the backend holds it: the phrase wrap and nothing else.
        val uploaded = fixture.document.uploadableKeyMaterial()

        repeat(9) {
            assertIs<PinUnlockResult.Incorrect>(fixture.lock.unlockWithPin("000000"))
            clock.advanceBy(10 * 60 * 1000)
        }
        assertEquals(PinUnlockResult.KeyMaterialDestroyed, fixture.lock.unlockWithPin("000000"))

        assertNull(keyMaterial.read())
        assertEquals(EncryptionSessionState.SetupIncomplete, session.state.value)
        // There is no PIN left to try, which is what sends the app to restore rather than to an
        // unlock screen it could never satisfy.
        assertEquals(PinUnlockResult.PinNotSet, fixture.lock.unlockWithPin(PIN))

        // The whole point of two independent wraps: the backend copy is untouched and opens.
        assertEquals(1, uploaded.wrappedKeys.size)
        assertEquals(KeyWrapType.RecoveryPhrase, uploaded.wrappedKeys.single().type)
        val restored = phraseKeyWrapper.unwrap(uploaded.wrappedKeys.single(), fixture.phrase)
        assertEquals(fixture.dataKey.id, restored.id)
        assertTrue(restored.exportRawBytes().contentEquals(fixture.dataKey.exportRawBytes()))
    }

    @Test
    fun enablingBiometricWithoutAPinIsRefused() = runTest {
        // A device restored from the phrase: key material exists, no PIN has been set.
        val dataKey = DataKey.generate()
        keyMaterial.write(
            KeyMaterialDocument(
                keyMaterialVersion = KeyMaterialDocument.CURRENT_VERSION,
                wrappedKeys = listOf(phraseKeyWrapper.wrap(dataKey, RecoveryPhrase.generate()))
            )
        )
        session.unlock(dataKey)
        val lock = appLock()

        assertEquals(BiometricEnableResult.PinRequired, lock.enableBiometric("123456", prompt))
        assertFalse(lock.isBiometricEnabled())
        assertNull(biometricBox.sealed)
    }

    @Test
    fun enablingBiometricRequiresTheCurrentPinEvenWhileUnlocked() = runTest {
        val fixture = setUpDevice()
        fixture.lock.unlockWithPin(PIN)

        assertEquals(
            BiometricEnableResult.IncorrectPin,
            fixture.lock.enableBiometric("000000", prompt)
        )
        assertFalse(fixture.lock.isBiometricEnabled())
    }

    @Test
    fun aSuccessfulBiometricUnlockYieldsTheKeyWithoutThePin() = runTest {
        val fixture = setUpDevice()
        assertEquals(BiometricEnableResult.Enabled, fixture.lock.enableBiometric(PIN, prompt))
        session.lock()

        val result = assertIs<BiometricUnlockResult.Unlocked>(fixture.lock.unlockWithBiometric(prompt))

        assertEquals(fixture.dataKey.id, result.dataKey.id)
        assertEquals(EncryptionSessionState.Unlocked, session.state.value)
        assertEquals(fixture.dataKey.id, session.dataKeyOrNull()?.id)
    }

    @Test
    fun aCancelledBiometricPromptFallsBackToThePinAndCostsNoAttempt() = runTest {
        val fixture = setUpDevice()
        fixture.lock.enableBiometric(PIN, prompt)
        session.lock()
        biometricBox.openResult = BiometricOpenResult.Cancelled

        val cancelled = assertIs<BiometricUnlockResult.FellBackToPin>(
            fixture.lock.unlockWithBiometric(prompt)
        )

        assertEquals(PinFallbackReason.Cancelled, cancelled.reason)
        assertEquals(EncryptionSessionState.Locked, session.state.value)
        // A dismissed prompt is not a failed PIN attempt: the counter is where it was.
        assertEquals(0, assertIs<StoredFailedAttempts.Recorded>(attemptStore.stored).record.consecutiveFailures)

        assertEquals(PinUnlockResult.Unlocked, fixture.lock.unlockWithPin(PIN))
    }

    @Test
    fun anEnrollmentChangeInvalidatesTheBiometricPathAndLeavesThePinWorking() = runTest {
        val fixture = setUpDevice()
        fixture.lock.enableBiometric(PIN, prompt)
        session.lock()

        // What the platform reports once the enrolled biometric set has changed.
        biometricBox.openResult = BiometricOpenResult.Invalidated

        val invalidated = assertIs<BiometricUnlockResult.FellBackToPin>(
            fixture.lock.unlockWithBiometric(prompt)
        )
        assertEquals(PinFallbackReason.Invalidated, invalidated.reason)
        assertFalse(fixture.lock.isBiometricEnabled())

        // Six digits, not twelve words — the whole reason Decision 4 makes the PIN mandatory.
        assertEquals(PinUnlockResult.Unlocked, fixture.lock.unlockWithPin(PIN))
        assertEquals(fixture.dataKey.id, session.dataKeyOrNull()?.id)
    }

    @Test
    fun changingThePinRequiresTheCurrentOneAndTheNewOneThenUnlocks() = runTest {
        val fixture = setUpDevice()

        assertIs<PinChangeResult.CurrentPinRejected>(fixture.lock.setPin("999999", "000000"))
        assertEquals(PinChangeResult.Changed, fixture.lock.setPin("999999", PIN))

        assertIs<PinUnlockResult.Incorrect>(fixture.lock.unlockWithPin(PIN))
        assertEquals(PinUnlockResult.Unlocked, fixture.lock.unlockWithPin("999999"))
    }

    @Test
    fun removingThePinRequiresItAndTurnsBiometricOff() = runTest {
        val fixture = setUpDevice()
        fixture.lock.enableBiometric(PIN, prompt)

        assertIs<PinChangeResult.CurrentPinRejected>(fixture.lock.removePin("000000"))
        assertEquals(PinChangeResult.Changed, fixture.lock.removePin(PIN))

        assertFalse(fixture.lock.isPinSet())
        assertFalse(fixture.lock.isBiometricEnabled())
    }

    /**
     * Nothing that is written down while the session is unlocked contains the data key.
     *
     * The assertion is made against the serialized form of everything the local key material store
     * was ever handed, plus the biometrically sealed blob, searched for the raw key bytes in both
     * Base64 and raw form. The session's own copy is a field on an object, which is what "process
     * memory only" means.
     */
    @OptIn(ExperimentalEncodingApi::class)
    @Test
    fun noUnwrappedKeyIsPersistedWhileTheSessionIsUnlocked() = runTest {
        val fixture = setUpDevice()
        fixture.lock.enableBiometric(PIN, prompt)
        assertEquals(PinUnlockResult.Unlocked, fixture.lock.unlockWithPin(PIN))
        assertEquals(EncryptionSessionState.Unlocked, session.state.value)

        val raw = fixture.dataKey.exportRawBytes()
        val encoded = Base64.encode(raw)

        val persisted = keyMaterial.writtenDocuments.joinToString("\n") { document ->
            Json.encodeToString(KeyMaterialDocument.serializer(), document)
        }
        assertFalse(persisted.contains(encoded), "the data key was written to local key material")
        assertFalse(
            persisted.encodeToByteArray().containsSequence(raw),
            "the data key's raw bytes were written to local key material"
        )

        // The biometric copy is sealed by the platform in production; the fake holds what it was
        // handed, and the assertion that matters here is that only the box ever saw those bytes.
        assertTrue(biometricBox.sealed?.containsSequence(raw) == true)
        assertFalse(
            attemptStore.stored.toString().contains(encoded),
            "the failed-attempt counter carries key material"
        )
    }

    private fun ByteArray.containsSequence(needle: ByteArray): Boolean {
        if (needle.isEmpty() || needle.size > size) return false
        outer@ for (start in 0..size - needle.size) {
            for (index in needle.indices) {
                if (this[start + index] != needle[index]) continue@outer
            }
            return true
        }
        return false
    }

    private companion object {
        const val PIN = "246813"
    }
}
