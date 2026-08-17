package id.andriawan.cofinance.data.lock

import id.andriawan.cofinance.data.keyring.EncryptionSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Drops the data key once the app has been in the background past the auto-lock timeout.
 *
 * The lock is a property of held key material rather than of a screen: this class calls
 * [EncryptionSession.lock], which clears the only copy of the unwrapped key, so nothing the user
 * can navigate to gets around it. It needs nothing else from the session, which is why it takes the
 * read-only interface rather than the mutable implementation — it can lock and it cannot unlock.
 *
 * Two mechanisms cover the two ways time passes in the background, and they are both here because
 * neither alone is sufficient:
 *
 * - a scheduled job clears the key *while* the app is in the background, so a device left on a desk
 *   is not holding a decrypted key for hours waiting to be resumed. This is what makes the
 *   requirement's wording — cleared when backgrounded beyond the timeout — literally true.
 * - an elapsed-time check on return covers the case where the job never ran, which is the normal
 *   case on both platforms: a frozen or suspended process does not execute timers, and on iOS the
 *   app may be killed and rebuilt entirely.
 *
 * Both consult the same [LockClock], so a test advances one virtual clock and sees both paths agree.
 */
class AutoLockController(
    private val session: EncryptionSession,
    private val settings: AutoLockSettings,
    private val scope: CoroutineScope,
    private val clock: LockClock = SystemLockClock
) {

    private var backgroundedAtMillis: Long? = null
    private var scheduledLock: Job? = null

    /**
     * Call when the app leaves the foreground.
     *
     * The timeout is read here rather than when the timer fires, so a user who changes the setting
     * while the app is in the background — which they cannot do — could not extend a running wait.
     */
    fun onEnterBackground() {
        val timeout = settings.timeout.value.duration
        scheduledLock?.cancel()
        scheduledLock = null

        if (timeout == Duration.ZERO) {
            backgroundedAtMillis = null
            session.lock()
            return
        }

        backgroundedAtMillis = clock.nowMillis()
        scheduledLock = scope.launch {
            delay(timeout)
            session.lock()
            backgroundedAtMillis = null
        }
    }

    /**
     * Call when the app returns to the foreground.
     *
     * Locks when the elapsed background time reached the timeout, whether or not the scheduled job
     * got to run. Resuming inside the timeout cancels the job and leaves the key in memory, which is
     * the "resume without unlocking" case.
     */
    fun onEnterForeground() {
        scheduledLock?.cancel()
        scheduledLock = null

        val since = backgroundedAtMillis ?: return
        backgroundedAtMillis = null
        val elapsed = (clock.nowMillis() - since).milliseconds
        // A clock moved backwards yields a negative elapsed time, which fails this comparison and so
        // leaves the session unlocked. That is the safe direction: the user is present, and the
        // scheduled job above already locked the session if the wait genuinely elapsed.
        if (elapsed >= settings.timeout.value.duration) {
            session.lock()
        }
    }

    /** Drops any pending timer, for a caller tearing the controller down. */
    fun cancel() {
        scheduledLock?.cancel()
        scheduledLock = null
        backgroundedAtMillis = null
    }
}
