package id.andriawan.cofinance.data.lock

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * The only source of "what time is it" the lock uses, so tests never sleep.
 *
 * Two behaviors depend on elapsed time — the escalating delay between failed PIN attempts and the
 * auto-lock timeout — and both have to be observable in a unit test without real waiting. Every
 * component here therefore takes a [LockClock] rather than reading a wall clock directly, and the
 * tests hand it the virtual time of a `TestScope`, which advances instantly and stays consistent
 * with the `delay` the same scope schedules.
 */
fun interface LockClock {
    fun nowMillis(): Long
}

/**
 * The wall clock, which is what the failed-attempt delay has to be measured against.
 *
 * A monotonic source would be immune to the user moving the device clock, but it also resets on
 * reboot, and a delay that a reboot clears is no delay at all against an attacker holding the
 * device. The wall clock is therefore the lesser evil, and the residual weakness is bounded: moving
 * the clock forward skips a delay but does not touch the counter, so the tenth consecutive failure
 * still destroys local key material on schedule. See [FailedAttemptGuard] for how a clock moved
 * *backwards* is handled.
 */
@OptIn(ExperimentalTime::class)
val systemLockClock = LockClock { Clock.System.now().toEpochMilliseconds() }
