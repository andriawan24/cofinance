package id.andriawan.cofinance.data.migration

import id.andriawan.cofinance.data.keyring.EncryptionSession
import id.andriawan.cofinance.data.remote.FinanceCollection
import id.andriawan.cofinance.data.remote.KeyMaterialGate
import id.andriawan.cofinance.data.session.SessionPolicy
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The one-time conversion of a signed-in user's plaintext cloud records to encrypted ones.
 *
 * Per Decision 5 this is blocking: a user whose records are plaintext does not reach normal use
 * until [state] reports [MigrationState.Complete]. The blocking itself belongs to the caller — this
 * type publishes states and converts records, and builds no UI — but the states are shaped so a
 * screen can show scanning, per-record progress, and a failure the user can retry, rather than the
 * flow failing silently behind a spinner.
 *
 * Resumption needs nothing stored. Per Decision 7 a record is migrated exactly when it carries the
 * envelope version, so an interrupted run resumes by scanning for records that lack it. There is no
 * cursor, no completion flag, and therefore no progress state that can disagree with the data.
 *
 * The order inside [run] is not incidental: key material is set up and then *verified through
 * [KeyMaterialGate]* before the first record is sealed. A record written before its key material
 * reaches the backend is undecryptable on any other device, so migration asks the same gate every
 * other encrypted write asks rather than deciding for itself that setup ran.
 */
class PlaintextMigration(
    private val migrator: PlaintextRecordMigrator,
    private val keyMaterialGate: KeyMaterialGate,
    private val encryptionSession: EncryptionSession,
    private val encryptionSetup: EncryptionSetup,
    private val sessionPolicy: SessionPolicy
) {

    private val mutableState = MutableStateFlow<MigrationState>(MigrationState.Idle)

    /** The current state, observable so a blocking screen can follow the run. */
    val state: StateFlow<MigrationState> = mutableState.asStateFlow()

    /**
     * Runs to completion, or to a failure the caller can offer to retry.
     *
     * Safe to call again at any point: a second run over converted records does nothing, which is
     * what makes retry after a failure and resume after a process death the same code path.
     */
    suspend fun run(): MigrationState {
        // A user who never signed in has nothing in the cloud to convert, and must not be sent
        // through encryption setup to protect records that do not exist.
        if (!sessionPolicy.isSignedIn()) {
            return settle(MigrationState.Complete(converted = 0, alreadyEncrypted = 0))
        }

        return try {
            mutableState.value = MigrationState.Scanning
            val scanned = migrator.scan()
            val pending = scanned.filterNot { it.isMigrated }
            val alreadyEncrypted = scanned.count { it.isMigrated }

            // Nothing plaintext up there. Notably this returns before touching [encryptionSetup]:
            // migration is not a second place that can force setup on a user who does not need it.
            if (pending.isEmpty()) {
                return settle(MigrationState.Complete(converted = 0, alreadyEncrypted = alreadyEncrypted))
            }

            encryptionSetup.ensureEncryptionReady()
            // Asserted, not assumed. If setup did not actually publish key material this throws
            // before the first ciphertext is written rather than after.
            keyMaterialGate.requireKeyMaterial()
            val dataKey = encryptionSession.requireDataKey()

            var converted = 0
            val unconverted = mutableListOf<UnconvertedRecord>()
            mutableState.value = MigrationState.Converting(finished = 0, total = pending.size)

            pending.forEach { record ->
                when (val outcome = migrator.convert(record, dataKey)) {
                    RecordConversion.Converted,
                    RecordConversion.PlaintextRemoved -> converted++

                    RecordConversion.AlreadyEncrypted -> Unit

                    is RecordConversion.NotConverted -> unconverted += UnconvertedRecord(
                        collection = record.collection,
                        id = record.id,
                        reason = outcome.reason
                    )
                }
                mutableState.value = MigrationState.Converting(
                    finished = converted + unconverted.size,
                    total = pending.size
                )
            }

            settle(
                MigrationState.Complete(
                    converted = converted,
                    alreadyEncrypted = alreadyEncrypted,
                    unconverted = unconverted
                )
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (cause: Throwable) {
            settle(MigrationState.Failed(cause))
        }
    }

    private fun settle(state: MigrationState): MigrationState {
        mutableState.value = state
        return state
    }
}

/**
 * Encryption setup, as migration needs to invoke it.
 *
 * A signed-in user with plaintext records may have no key material at all — they synchronized under
 * a build that had none. Migration cannot generate a data key and a recovery phrase itself without
 * duplicating the setup flow and its mandatory phrase confirmation, so it asks for setup through
 * this seam and then verifies the outcome through [KeyMaterialGate]. Implementations must be
 * idempotent: migration calls this on every run that has work, including a resumed one.
 */
fun interface EncryptionSetup {

    /** Ensures this installation holds usable key material, running setup when it holds none. */
    suspend fun ensureEncryptionReady()
}

/** Where the run is, in the terms a blocking screen needs. */
sealed interface MigrationState {

    /** Not started. */
    data object Idle : MigrationState

    /** Reading both collections to find what still lacks the envelope version. */
    data object Scanning : MigrationState

    /** Converting, with [finished] of [total] records dealt with. */
    data class Converting(val finished: Int, val total: Int) : MigrationState

    /**
     * The run finished. [unconverted] is empty for a clean finish and lists what was left otherwise.
     *
     * Completion with residue is deliberately still completion: the alternative is a user permanently
     * unable to reach the app because one stored record no longer decodes. The residue is enumerated
     * rather than swallowed so the caller can report it, and the next run retries those records for
     * free because they still lack the marker.
     */
    data class Complete(
        val converted: Int,
        val alreadyEncrypted: Int,
        val unconverted: List<UnconvertedRecord> = emptyList()
    ) : MigrationState {

        /** True when every scanned record ended up encrypted with no plaintext beside it. */
        val isClean: Boolean get() = unconverted.isEmpty()
    }

    /**
     * The run stopped and converted nothing further. Records already converted stay converted, and a
     * retry resumes from the scan.
     */
    data class Failed(val cause: Throwable) : MigrationState
}

/** A record migration left exactly as it found it, named so the caller can say which. */
data class UnconvertedRecord(
    val collection: FinanceCollection,
    val id: String,
    val reason: String
)
