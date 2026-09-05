package id.andriawan.cofinance.data.lock

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * The failed-attempt schedule, as arithmetic with no storage and no side effects.
 *
 * Decision 9 fixes every number here: four free attempts, a 30-second delay on the fifth that
 * doubles with each further failure to a 5-minute cap, and destruction of local key material at the
 * tenth consecutive failure. They live in one object so the schedule can be tested as a table and so
 * that nothing else in the lock has to restate them.
 *
 * The threshold is deliberately not a setting and is not reachable through any constructor: Decision
 * 9 rejects exposing it, because its only interesting values are self-destructive or self-defeating.
 */
object LockoutPolicy {
    const val FREE_ATTEMPTS: Int = 4
    const val DESTRUCTION_THRESHOLD: Int = 10
    val FIRST_DELAY: Duration = 30.seconds
    val MAXIMUM_DELAY: Duration = 5.minutes

    /**
     * How long the next attempt must wait, given [consecutiveFailures] already recorded.
     *
     * Read it as a property of the attempt about to be made rather than of the failure just
     * recorded: with four failures behind it, the fifth attempt waits [FIRST_DELAY], which is what
     * the app-lock specification asks for when it says the fifth attempt is delayed. Doubling then
     * continues per further failure — 30s, 60s, 120s, 240s — and saturates at [MAXIMUM_DELAY]
     * rather than continuing to double, so the ninth and tenth attempts each wait five minutes.
     *
     * The multiplication is done on an `Int` count of doublings that is clamped first, so no
     * arithmetic here can overflow even if a corrupted record claims a huge failure count.
     */
    fun delayBeforeNextAttempt(consecutiveFailures: Int): Duration {
        if (consecutiveFailures < FREE_ATTEMPTS) return Duration.ZERO
        val doubling = (consecutiveFailures - FREE_ATTEMPTS).coerceAtMost(MAXIMUM_DOUBLING)
        val delay = FIRST_DELAY * (1 shl doubling)
        return if (delay > MAXIMUM_DELAY) MAXIMUM_DELAY else delay
    }

    /** Attempts left before [DESTRUCTION_THRESHOLD] is reached, never negative. */
    fun attemptsRemaining(consecutiveFailures: Int): Int =
        (DESTRUCTION_THRESHOLD - consecutiveFailures).coerceAtLeast(0)

    /** True once [consecutiveFailures] has reached the destruction threshold. */
    fun destroys(consecutiveFailures: Int): Boolean = consecutiveFailures >= DESTRUCTION_THRESHOLD

    private const val MAXIMUM_DOUBLING = 4
}
