package id.andriawan.cofinance.data.lock

import id.andriawan.cofinance.data.crypto.FakeDeviceKeyVault
import id.andriawan.cofinance.data.crypto.KeyMaterialDocument
import id.andriawan.cofinance.data.crypto.KeyWrapType
import id.andriawan.cofinance.data.crypto.WrappedDataKey
import id.andriawan.cofinance.data.keyring.EncryptionSessionState
import id.andriawan.cofinance.data.keyring.InMemoryEncryptionSession
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * The counter, the delay it imposes, and the destruction it ends in.
 *
 * Time is a [MutableLockClock] here, so a five-minute wait costs the suite nothing and the
 * assertions are about the schedule rather than about how long the test took.
 */
class FailedAttemptGuardTest {

    private val attempts = FakeFailedAttemptStore()
    private val vault = FakeDeviceKeyVault()
    private val session = InMemoryEncryptionSession()
    private val clock = MutableLockClock(millis = 1_000_000)

    private val keyMaterial = FakeLocalKeyMaterialStore(documentWithPinWrap())

    private fun guard(): FailedAttemptGuard = FailedAttemptGuard(
        attempts = attempts,
        keyMaterial = keyMaterial,
        destroyer = LocalKeyMaterialDestroyer(keyMaterial, vault, session),
        clock = clock
    )

    @Test
    fun theFirstFourFailuresCostNothing() = runTest {
        val guard = guard()
        guard.arm()

        repeat(4) { index ->
            assertEquals(AttemptGate.Allowed, guard.beginAttempt(), "attempt ${index + 1}")
            val outcome = assertIs<FailedAttemptOutcome.Counted>(guard.recordFailure())
            assertEquals(index + 1, outcome.consecutiveFailures)
        }
    }

    @Test
    fun theFifthAttemptWaitsThirtySecondsAndProceedsAfterwards() = runTest {
        val guard = guard()
        guard.arm()
        repeat(4) { guard.recordFailure() }

        val throttled = assertIs<AttemptGate.Throttled>(guard.beginAttempt())
        assertEquals(30.seconds, throttled.remaining)
        assertEquals(4, throttled.consecutiveFailures)

        clock.advanceBy(29_000)
        assertIs<AttemptGate.Throttled>(guard.beginAttempt())

        clock.advanceBy(1_000)
        assertEquals(AttemptGate.Allowed, guard.beginAttempt())
    }

    @Test
    fun theDelayIsNotServedByRestartingTheApp() = runTest {
        guard().let { first ->
            first.arm()
            repeat(5) { first.recordFailure() }
        }

        // A second guard over the same stored record is what a relaunched process sees.
        val relaunched = guard()
        val throttled = assertIs<AttemptGate.Throttled>(relaunched.beginAttempt())
        assertEquals(1.minutes, throttled.remaining)
    }

    @Test
    fun aClockMovedBackwardsDoesNotServeTheDelay() = runTest {
        val guard = guard()
        guard.arm()
        repeat(4) { guard.recordFailure() }

        clock.advanceBy(-600_000)

        val throttled = assertIs<AttemptGate.Throttled>(guard.beginAttempt())
        assertEquals(30.seconds, throttled.remaining)
    }

    @Test
    fun aCorrectPinClearsTheRunOfFailures() = runTest {
        val guard = guard()
        guard.arm()
        repeat(4) { guard.recordFailure() }

        guard.recordSuccess()

        assertEquals(0, guard.consecutiveFailures())
        assertEquals(AttemptGate.Allowed, guard.beginAttempt())
    }

    @Test
    fun theTenthFailureDestroysLocalKeyMaterial() = runTest {
        val guard = guard()
        guard.arm()
        val deviceSecretBefore = vault.deviceSecret()

        repeat(9) { index ->
            assertIs<FailedAttemptOutcome.Counted>(
                guard.recordFailure(),
                "failure ${index + 1} should not destroy"
            )
            clock.advanceBy(10 * 60 * 1000)
        }

        assertEquals(FailedAttemptOutcome.KeyMaterialDestroyed, guard.recordFailure())
        assertEquals(1, keyMaterial.erases)
        assertEquals(null, keyMaterial.read())
        assertEquals(EncryptionSessionState.SetupIncomplete, session.state.value)
        assertTrue(
            !deviceSecretBefore.contentEquals(vault.deviceSecret()),
            "the device secret survived destruction, so the PIN wrap could still be derived"
        )
    }

    /**
     * After destruction the guard has nothing left to guard, and says so.
     *
     * There is no PIN wrap and no counter, which is the same state a device has before a PIN is
     * ever set, so the gate opens. What refuses the user is one level up: `AppLock.verifyPin`
     * finds no PIN wrap and answers [PinVerification.PinNotSet], and the session has been returned
     * to `SetupIncomplete`, so the app reaches restore rather than an unlock screen.
     */
    @Test
    fun afterDestructionTheGuardIsExemptBecauseThereIsNothingLeftToProtect() = runTest {
        val guard = guard()
        guard.arm()
        repeat(10) {
            guard.recordFailure()
            clock.advanceBy(10 * 60 * 1000)
        }

        assertEquals(AttemptGate.Allowed, guard.beginAttempt())
        assertEquals(null, keyMaterial.read())
        assertEquals(EncryptionSessionState.SetupIncomplete, session.state.value)
        assertEquals(StoredFailedAttempts.None, attempts.stored)
    }

    @Test
    fun aDeletedCounterOnADeviceHoldingAPinWrapIsTreatedAsTampering() = runTest {
        val guard = guard()
        guard.arm()
        repeat(4) { guard.recordFailure() }

        // What an attacker does to buy four more attempts: remove the record and try again.
        attempts.stored = StoredFailedAttempts.None

        assertEquals(AttemptGate.KeyMaterialDestroyed, guard.beginAttempt())
        assertEquals(1, keyMaterial.erases)
    }

    @Test
    fun anUnreadableCounterIsTreatedAsTampering() = runTest {
        val guard = guard()
        guard.arm()
        attempts.stored = StoredFailedAttempts.Unreadable

        assertEquals(AttemptGate.KeyMaterialDestroyed, guard.beginAttempt())
    }

    @Test
    fun aDeviceWithNoPinWrapIsExemptFromTheTamperRule() = runTest {
        val emptyMaterial = FakeLocalKeyMaterialStore(null)
        val guard = FailedAttemptGuard(
            attempts = attempts,
            keyMaterial = emptyMaterial,
            destroyer = LocalKeyMaterialDestroyer(emptyMaterial, vault, session),
            clock = clock
        )

        assertEquals(AttemptGate.Allowed, guard.beginAttempt())
        assertEquals(0, emptyMaterial.erases)
    }

    @Test
    fun anInterruptedDestructionIsFinishedOnTheNextAttempt() = runTest {
        // The count reached ten and the process died before the material was erased.
        attempts.stored = StoredFailedAttempts.Recorded(
            FailedAttemptRecord(consecutiveFailures = 10, lastFailureAtMillis = clock.nowMillis())
        )

        assertEquals(AttemptGate.KeyMaterialDestroyed, guard().beginAttempt())
        assertEquals(1, keyMaterial.erases)
    }

    private fun documentWithPinWrap(): KeyMaterialDocument = KeyMaterialDocument(
        keyMaterialVersion = KeyMaterialDocument.CURRENT_VERSION,
        wrappedKeys = listOf(
            WrappedDataKey.of(
                type = KeyWrapType.Pin,
                keyId = "test-key",
                wrappedKey = ByteArray(48) { it.toByte() }
            )
        )
    )
}
