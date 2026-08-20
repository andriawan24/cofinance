package id.andriawan.cofinance.data.migration

import id.andriawan.cofinance.data.crypto.DataKey
import id.andriawan.cofinance.data.crypto.RecordCipher
import id.andriawan.cofinance.data.crypto.toDocument

/**
 * Converts one record at a time, always writing the ciphertext before removing the plaintext.
 *
 * The ordering in [convert] is the single correctness property of this whole module, and it is why
 * migration is per record rather than per collection. Removing plaintext ahead of writing ciphertext
 * — for any batch size, including one — makes a process death between the two steps destroy the
 * record. Doing it in this order makes the same death leave a document carrying both, which the next
 * scan finishes with a delete. Nothing is lost, and no progress record is needed to know where the
 * run stopped.
 *
 * Conversion is idempotent per record because the envelope marker, not a stored cursor, is what says
 * a record is done. Re-running touches only what still lacks it.
 */
class PlaintextRecordMigrator(
    private val store: PlaintextFinanceDocumentStore,
    private val cipher: RecordCipher = RecordCipher()
) {

    /** Reads both finance collections and classifies every document against the envelope marker. */
    suspend fun scan(): List<ScannedRecord> =
        LegacyFinanceCollections.ALL.flatMap { it.scan(store, cipher) }

    /**
     * Brings one scanned [record] to its migrated state, and does nothing when it is already there.
     *
     * Store failures propagate rather than being folded into an outcome. A write or delete that the
     * backend rejected is an I/O problem the next attempt may well survive, and swallowing it would
     * let the run report completion while plaintext is still up there. Only an unreadable record —
     * one whose stored plaintext no longer decodes, which no retry will fix — comes back as
     * [RecordConversion.NotConverted] so the rest of the run can proceed.
     */
    suspend fun convert(record: ScannedRecord, dataKey: DataKey): RecordConversion {
        if (record.isMigrated) return RecordConversion.AlreadyEncrypted

        if (record.needsConversion) {
            val seal = record.seal ?: return RecordConversion.NotConverted(
                "Stored plaintext could not be read as a finance record"
            )
            val envelope = seal(dataKey)
            // Write first. Everything below this line is safe to lose to a process death; nothing
            // above it may be reordered past it.
            store.writeEnvelope(record.collection, record.id, envelope.toDocument())
            store.removePlaintextFields(record.collection, record.id, record.plaintextFields)
            return RecordConversion.Converted
        }

        // Encrypted already, plaintext still beside it: an earlier run died between the two writes.
        // The ciphertext holds the values, so finishing is a delete and never a re-encryption — which
        // also means the record is never sealed twice under two different nonces.
        store.removePlaintextFields(record.collection, record.id, record.plaintextFields)
        return RecordConversion.PlaintextRemoved
    }
}

/** What happened to one record. */
sealed interface RecordConversion {

    /** Ciphertext written, then plaintext removed. */
    data object Converted : RecordConversion

    /** Ciphertext was already there from an interrupted run; the leftover plaintext is now gone. */
    data object PlaintextRemoved : RecordConversion

    /** Nothing to do. */
    data object AlreadyEncrypted : RecordConversion

    /** The record was left exactly as it was found, and the run continues past it. */
    data class NotConverted(val reason: String) : RecordConversion
}
