package id.andriawan.cofinance.data.lock

import id.andriawan.cofinance.data.crypto.KeyWrapType
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Whether an unlock attempt may be made right now.
 *
 * [Throttled] and [KeyMaterialDestroyed] are both refusals, but they are different screens: one is
 * a countdown the user waits out, the other sends them to recovery-phrase restoration.
 */
sealed interface AttemptGate {

    /** Nothing stands in the way; the caller may derive from the PIN. */
    data object Allowed : AttemptGate

    /** The escalating delay has not elapsed. [remaining] is what is left of it. */
    data class Throttled(val remaining: Duration, val consecutiveFailures: Int) : AttemptGate

    /** Local key material is gone. Only the recovery phrase can restore access. */
    data object KeyMaterialDestroyed : AttemptGate
}

/** What counting a failure did. */
sealed interface FailedAttemptOutcome {

    /** The failure was counted and the threshold is not reached yet. */
    data class Counted(
        val consecutiveFailures: Int,
        val attemptsRemaining: Int,
        val nextAttemptDelay: Duration
    ) : FailedAttemptOutcome

    /** The threshold was reached and local key material has been destroyed. */
    data object KeyMaterialDestroyed : FailedAttemptOutcome
}

/**
 * The consecutive-failure counter, the delay it imposes, and the destruction it ends in.
 *
 * The counter is the only stateful part of PIN unlock. Unlock itself is derivation — see
 * `PinKeyWrapper`, where a wrong PIN simply derives a key that fails AES-GCM authentication — so
 * nothing here is, or could be, a comparison against a stored PIN. This class never sees a PIN. It
 * is told that an attempt failed, and it decides what that costs.
 *
 * ## The tamper rule
 *
 * A successful unlock writes a zero record rather than deleting the counter, so once a PIN wrap
 * exists a counter record exists too. That turns two otherwise ambiguous readings into evidence:
 * a record that will not open ([StoredFailedAttempts.Unreadable]) has been edited or separated from
 * the platform key that seals it, and an absent record ([StoredFailedAttempts.None]) *while a PIN
 * wrap is present* has been deleted. Both are treated as the threshold being reached, and local key
 * material is destroyed.
 *
 * That is deliberately severe, and it is the only response that closes the hole: an attacker who
 * can delete the counter and be met with a fresh set of attempts has no reason not to delete it
 * every four tries, and the whole schedule becomes decorative. The cost of a false positive is
 * bounded by the same property that makes destruction acceptable at ten failures — the data is
 * restorable from the recovery phrase — and a false positive is hard to produce accidentally,
 * because the paths that remove the record (clearing app data, uninstalling, a backup restored onto
 * a different device) remove or orphan the platform key material the PIN wrap needs as well, so the
 * wrap being destroyed was already unopenable.
 *
 * A device with no PIN wrap — a fresh install, or one restored from the phrase before a PIN is set
 * — is exempt, because there is nothing for the counter to protect and no record is expected.
 */
class FailedAttemptGuard(
    private val attempts: FailedAttemptStore,
    private val keyMaterial: LocalKeyMaterialStore,
    private val destroyer: LocalKeyMaterialDestroyer,
    private val clock: LockClock = systemLockClock
) {

    /**
     * Decides whether an attempt may proceed, destroying key material if the record was tampered
     * with.
     *
     * Call this before deriving anything from the PIN: the delay exists to slow guessing, so it has
     * to be paid before the guess is evaluated rather than after.
     */
    suspend fun beginAttempt(): AttemptGate = when (val stored = readCounter()) {
        is CounterReading.Tampered -> {
            destroyer.destroy()
            attempts.clear()
            AttemptGate.KeyMaterialDestroyed
        }

        is CounterReading.Exempt -> AttemptGate.Allowed

        is CounterReading.Counted -> {
            if (LockoutPolicy.destroys(stored.record.consecutiveFailures)) {
                // Reachable when destruction was interrupted after the count was written — a
                // process kill between the two writes. Finishing it is the safe interpretation.
                destroyer.destroy()
                attempts.clear()
                AttemptGate.KeyMaterialDestroyed
            } else {
                throttle(stored.record)
            }
        }
    }

    /**
     * Counts one failed attempt and returns what it cost.
     *
     * Destruction happens here rather than being left to the caller, so that no unlock path can
     * count a tenth failure and then decide not to act on it.
     */
    suspend fun recordFailure(): FailedAttemptOutcome {
        val previous = when (val stored = readCounter()) {
            is CounterReading.Counted -> stored.record.consecutiveFailures
            // A tampered or exempt reading still counts the failure it was handed: the tamper case
            // is destroyed by the next [beginAttempt] anyway, and the exempt case has no wrap to
            // destroy, so counting is harmless in both and losing the count would not be.
            else -> 0
        }
        val failures = previous + 1
        attempts.write(FailedAttemptRecord(failures, clock.nowMillis()))

        if (LockoutPolicy.destroys(failures)) {
            destroyer.destroy()
            attempts.clear()
            return FailedAttemptOutcome.KeyMaterialDestroyed
        }
        return FailedAttemptOutcome.Counted(
            consecutiveFailures = failures,
            attemptsRemaining = LockoutPolicy.attemptsRemaining(failures),
            nextAttemptDelay = LockoutPolicy.delayBeforeNextAttempt(failures)
        )
    }

    /**
     * Clears the run of failures after a correct PIN.
     *
     * A zero record is written rather than the record being removed, so that the absence of a
     * record stays meaningful. See the tamper rule on this class.
     */
    suspend fun recordSuccess() {
        attempts.write(FailedAttemptRecord(consecutiveFailures = 0, lastFailureAtMillis = 0))
    }

    /**
     * Arms the counter for a device that has just been given a PIN.
     *
     * Setup and PIN changes call this. It is the same zero record a success writes, and it is what
     * makes a later absence detectable.
     */
    suspend fun arm() = recordSuccess()

    /** The count as stored, for a caller that wants to show "N attempts remaining". */
    suspend fun consecutiveFailures(): Int = when (val stored = readCounter()) {
        is CounterReading.Counted -> stored.record.consecutiveFailures
        is CounterReading.Tampered -> LockoutPolicy.DESTRUCTION_THRESHOLD
        is CounterReading.Exempt -> 0
    }

    /**
     * Turns a stored record into the remaining wait.
     *
     * A clock moved backwards — by the user, or by a time-zone-agnostic NTP correction — produces a
     * negative elapsed time. It is read as "none of the delay has been served" rather than as a
     * completed wait, because the alternative hands an attacker a way to skip the schedule by
     * setting the clock forward, failing, and setting it back.
     */
    private fun throttle(record: FailedAttemptRecord): AttemptGate {
        val required = LockoutPolicy.delayBeforeNextAttempt(record.consecutiveFailures)
        if (required == Duration.ZERO) return AttemptGate.Allowed
        val elapsed = (clock.nowMillis() - record.lastFailureAtMillis).milliseconds
        val remaining = required - elapsed
        return if (remaining <= Duration.ZERO) {
            AttemptGate.Allowed
        } else {
            AttemptGate.Throttled(remaining.coerceAtMost(required), record.consecutiveFailures)
        }
    }

    private suspend fun readCounter(): CounterReading = when (val stored = attempts.read()) {
        is StoredFailedAttempts.Recorded -> CounterReading.Counted(stored.record)
        StoredFailedAttempts.Unreadable -> if (pinWrapPresent()) {
            CounterReading.Tampered
        } else {
            CounterReading.Exempt
        }

        StoredFailedAttempts.None -> if (pinWrapPresent()) {
            CounterReading.Tampered
        } else {
            CounterReading.Exempt
        }
    }

    private suspend fun pinWrapPresent(): Boolean =
        keyMaterial.read()?.wrapsOf(KeyWrapType.Pin)?.isNotEmpty() == true

    private sealed interface CounterReading {
        data class Counted(val record: FailedAttemptRecord) : CounterReading
        data object Tampered : CounterReading
        data object Exempt : CounterReading
    }
}
