package id.andriawan.cofinance.data.lock

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * The delay schedule as a table, which is the only way to see it whole.
 *
 * Decision 9 states it in prose — free through the fourth, 30 seconds on the fifth, doubling to a
 * five-minute cap, destruction at ten — and prose is where an off-by-one hides. Every attempt from
 * the first to the tenth is asserted here rather than a sample of them.
 */
class LockoutPolicyTest {

    @Test
    fun theFirstFourAttemptsAreFree() {
        for (failures in 0 until LockoutPolicy.FREE_ATTEMPTS) {
            assertEquals(
                Duration.ZERO,
                LockoutPolicy.delayBeforeNextAttempt(failures),
                "attempt ${failures + 1} should not be delayed"
            )
        }
    }

    @Test
    fun theScheduleDoublesFromThirtySecondsAndCapsAtFiveMinutes() {
        // Read the left column as "failures already recorded", so the row is the delay paid by the
        // attempt after them: four failures means the fifth attempt waits 30 seconds.
        val expected = mapOf(
            4 to 30.seconds,
            5 to 1.minutes,
            6 to 2.minutes,
            7 to 4.minutes,
            8 to 5.minutes,
            9 to 5.minutes
        )

        for ((failures, delay) in expected) {
            assertEquals(
                delay,
                LockoutPolicy.delayBeforeNextAttempt(failures),
                "attempt ${failures + 1} waits the wrong time"
            )
        }
    }

    @Test
    fun theDelayNeverExceedsTheCapEvenForANonsenseCount() {
        assertEquals(5.minutes, LockoutPolicy.delayBeforeNextAttempt(Int.MAX_VALUE))
        assertEquals(5.minutes, LockoutPolicy.delayBeforeNextAttempt(64))
    }

    @Test
    fun destructionHappensAtTenAndNotBefore() {
        for (failures in 0 until LockoutPolicy.DESTRUCTION_THRESHOLD) {
            assertFalse(LockoutPolicy.destroys(failures), "$failures failures should not destroy")
        }
        assertTrue(LockoutPolicy.destroys(LockoutPolicy.DESTRUCTION_THRESHOLD))
        assertTrue(LockoutPolicy.destroys(LockoutPolicy.DESTRUCTION_THRESHOLD + 1))
    }

    @Test
    fun remainingAttemptsCountDownToZeroAndStop() {
        assertEquals(10, LockoutPolicy.attemptsRemaining(0))
        assertEquals(1, LockoutPolicy.attemptsRemaining(9))
        assertEquals(0, LockoutPolicy.attemptsRemaining(10))
        assertEquals(0, LockoutPolicy.attemptsRemaining(99))
    }
}
