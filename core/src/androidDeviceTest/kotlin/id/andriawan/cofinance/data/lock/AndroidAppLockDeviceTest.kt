package id.andriawan.cofinance.data.lock

import id.andriawan.cofinance.data.crypto.AndroidDeviceKeyVault
import id.andriawan.cofinance.data.crypto.DataKey
import id.andriawan.cofinance.data.crypto.KeyMaterialDocument
import id.andriawan.cofinance.data.crypto.KeyWrapType
import id.andriawan.cofinance.data.crypto.PinKeyWrapper
import id.andriawan.cofinance.data.crypto.RecoveryPhrase
import id.andriawan.cofinance.data.crypto.RecoveryPhraseKeyWrapper
import id.andriawan.cofinance.data.crypto.createDeviceKeyVault
import id.andriawan.cofinance.data.keyring.EncryptionSessionState
import id.andriawan.cofinance.data.keyring.InMemoryEncryptionSession
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The lock over real Android secure storage: the Keystore-backed vault and the sealed counter.
 *
 * The state machine itself is covered in `commonTest` against fakes. What this adds is that the
 * same behaviour holds when the device secret comes from the Keystore and the counter comes from a
 * sealed file — in particular that ten wrong PINs destroy local key material *and* that the
 * recovery-phrase wrap, which never left this device's memory during any of it, still opens the
 * same data key afterwards.
 *
 * Time is injected, so the five-minute waits in the schedule cost the run nothing.
 */
class AndroidAppLockDeviceTest {

    private val session = InMemoryEncryptionSession()
    private val clock = MovableClock()
    private val keyMaterial = InMemoryKeyMaterialStore()

    private lateinit var vault: AndroidDeviceKeyVault
    private lateinit var attemptStore: KeystoreSealedFailedAttemptStore
    private lateinit var pinKeyWrapper: PinKeyWrapper

    @Before
    fun setUp() = runBlocking {
        vault = createDeviceKeyVault() as AndroidDeviceKeyVault
        vault.destroyKeyMaterial()
        attemptStore = createFailedAttemptStore() as KeystoreSealedFailedAttemptStore
        attemptStore.clear()
        pinKeyWrapper = PinKeyWrapper(vault)
    }

    @After
    fun tearDown() = runBlocking {
        attemptStore.clear()
        vault.destroyKeyMaterial()
    }

    @Test
    fun theCorrectPinUnlocksAndAWrongOneDoesNot() = runBlocking {
        val fixture = setUpDevice()

        assertEquals(
            PinUnlockResult.Unlocked,
            fixture.lock.unlockWithPin(PIN)
        )
        assertEquals(fixture.dataKey.id, session.dataKeyOrNull()?.id)

        session.lock()
        assertTrue(fixture.lock.unlockWithPin("111111") is PinUnlockResult.Incorrect)
        assertNull(session.dataKeyOrNull())
    }

    @Test
    fun theFifthAttemptIsDelayedAndACorrectPinClearsTheCount() = runBlocking {
        val fixture = setUpDevice()

        repeat(4) { assertTrue(fixture.lock.unlockWithPin("111111") is PinUnlockResult.Incorrect) }

        val throttled = fixture.lock.unlockWithPin(PIN)
        assertTrue("expected a throttled attempt, got $throttled", throttled is PinUnlockResult.Throttled)

        clock.advanceBy(30_000)
        assertEquals(PinUnlockResult.Unlocked, fixture.lock.unlockWithPin(PIN))

        val stored = attemptStore.read()
        assertTrue(stored is StoredFailedAttempts.Recorded)
        assertEquals(0, (stored as StoredFailedAttempts.Recorded).record.consecutiveFailures)
    }

    @Test
    fun tenWrongPinsDestroyLocalKeyMaterialAndThePhraseStillOpensTheDataKey() = runBlocking {
        val fixture = setUpDevice()
        val uploaded = keyMaterial.read()!!.uploadableKeyMaterial()

        repeat(9) {
            assertTrue(fixture.lock.unlockWithPin("111111") is PinUnlockResult.Incorrect)
            clock.advanceBy(10 * 60 * 1000)
        }
        assertEquals(
            PinUnlockResult.KeyMaterialDestroyed,
            fixture.lock.unlockWithPin("111111")
        )

        assertNull("local key material survived destruction", keyMaterial.read())
        assertEquals(EncryptionSessionState.SetupIncomplete, session.state.value)

        assertEquals(1, uploaded.wrappedKeys.size)
        assertEquals(KeyWrapType.RecoveryPhrase, uploaded.wrappedKeys.single().type)
        val restored = RecoveryPhraseKeyWrapper()
            .unwrap(uploaded.wrappedKeys.single(), fixture.phrase)
        assertEquals(fixture.dataKey.id, restored.id)
        assertTrue(restored.exportRawBytes().contentEquals(fixture.dataKey.exportRawBytes()))
    }

    @Test
    fun thePinWrapNoLongerOpensAfterTheDeviceKeyMaterialIsDestroyed() = runBlocking {
        setUpDevice()
        val pinWrap = keyMaterial.read()!!.wrapsOf(KeyWrapType.Pin).single()

        vault.destroyKeyMaterial()

        // A copy of the wrap taken before destruction is inert afterwards, because the device
        // secret it was composed with is gone.
        try {
            PinKeyWrapper(vault).unwrap(pinWrap, PIN)
            throw AssertionError("the PIN wrap opened after the device secret was destroyed")
        } catch (expected: Exception) {
            assertTrue(
                "expected an authentication failure, got ${expected::class.simpleName}",
                expected.message != null
            )
        }
    }

    private suspend fun setUpDevice(): Fixture {
        val dataKey = DataKey.generate()
        val phrase = RecoveryPhrase.generate()
        keyMaterial.write(
            KeyMaterialDocument(
                keyMaterialVersion = KeyMaterialDocument.CURRENT_VERSION,
                wrappedKeys = listOf(
                    RecoveryPhraseKeyWrapper().wrap(dataKey, phrase),
                    pinKeyWrapper.wrap(dataKey, PIN)
                )
            )
        )
        session.markSetUp()
        guard().arm()
        return Fixture(appLock(), dataKey, phrase)
    }

    private fun guard(): FailedAttemptGuard = FailedAttemptGuard(
        attempts = attemptStore,
        keyMaterial = keyMaterial,
        destroyer = LocalKeyMaterialDestroyer(keyMaterial, vault, session),
        clock = clock
    )

    private fun appLock(): AppLock = AppLock(
        session = session,
        keyMaterial = keyMaterial,
        pinKeyWrapper = pinKeyWrapper,
        attempts = guard(),
        biometrics = BiometricUnlock(createBiometricKeyBox()),
        autoLockSettings = createAutoLockSettings()
    )

    private class Fixture(
        val lock: AppLock,
        val dataKey: DataKey,
        val phrase: RecoveryPhrase
    )

    /** The local key material store setup will own; in-process here, which is all this test needs. */
    private class InMemoryKeyMaterialStore : LocalKeyMaterialStore {
        private var document: KeyMaterialDocument? = null
        override suspend fun read(): KeyMaterialDocument? = document
        override suspend fun write(document: KeyMaterialDocument) {
            this.document = document
        }

        override suspend fun erase() {
            document = null
        }
    }

    private class MovableClock : LockClock {
        private var millis = 1_700_000_000_000L
        override fun nowMillis(): Long = millis
        fun advanceBy(delta: Long) {
            millis += delta
        }
    }

    private companion object {
        const val PIN = "135790"
    }
}
