package id.andriawan.cofinance.data.lock

/**
 * One reading of the consecutive-failure counter.
 *
 * [lastFailureAtMillis] is wall-clock time from [LockClock], recorded at the moment the failure was
 * counted, and is what the escalating delay is measured from. It is stored alongside the count
 * rather than derived, because the delay has to survive process death: an attacker who kills the app
 * during a five-minute wait must come back to the same wait.
 */
data class FailedAttemptRecord(
    val consecutiveFailures: Int,
    val lastFailureAtMillis: Long
)

/**
 * What secure storage had to say about the counter.
 *
 * The three cases are kept distinct because they mean different things to [FailedAttemptGuard].
 * [None] is the ordinary state of a device that has never failed an unlock. [Unreadable] is not:
 * the record is sealed under a platform key, so a record that exists and will not open has been
 * edited, truncated, or separated from the key that seals it.
 */
sealed interface StoredFailedAttempts {

    /** Nothing has been written, which is the state before the first failure. */
    data object None : StoredFailedAttempts

    /** A well-formed record, as it was last written. */
    data class Recorded(val record: FailedAttemptRecord) : StoredFailedAttempts

    /** Something is stored under the counter's name and it does not open. */
    data object Unreadable : StoredFailedAttempts
}

/**
 * Where consecutive failed PIN attempts are counted.
 *
 * The specification requires the count to live in secure storage and to survive a reinstall, and
 * the two platforms deliver that differently enough that the difference is worth stating in the
 * port rather than discovering per implementation:
 *
 * - **iOS** gives it directly. A Keychain item outlives the application that wrote it, so
 *   deleting and reinstalling the app returns to the same counter.
 * - **Android** has no storage that outlives an uninstall. What it has instead is a coupling: the
 *   counter is sealed under a non-extractable Keystore key, and clearing app data or uninstalling
 *   destroys that key *together with* the device key material the PIN protects. A reinstall
 *   therefore does not hand the attacker a fresh set of attempts against the same wrapped key —
 *   there is no wrapped key left to attack, and access requires the recovery phrase. That is the
 *   property the requirement is after; "the number survives" is not literally true there, and
 *   [id.andriawan.cofinance.data.lock] documents it rather than claiming otherwise.
 *
 * Sealing also buys tamper evidence on both platforms: an attacker with the app's files cannot
 * rewrite the count to zero without the platform key, and a deleted record is visible to
 * [FailedAttemptGuard] as an absence where a PIN wrap exists.
 */
interface FailedAttemptStore {

    /** Reads the counter. Implementations never throw for an absent or damaged record. */
    suspend fun read(): StoredFailedAttempts

    /** Writes [record], replacing whatever was stored. */
    suspend fun write(record: FailedAttemptRecord)

    /** Removes the record, which a successful unlock and destruction both do. */
    suspend fun clear()
}

/** Returns the counter backed by this platform's secure storage. */
expect fun createFailedAttemptStore(): FailedAttemptStore
