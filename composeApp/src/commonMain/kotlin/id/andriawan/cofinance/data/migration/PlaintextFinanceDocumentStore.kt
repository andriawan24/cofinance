package id.andriawan.cofinance.data.migration

import id.andriawan.cofinance.data.crypto.EncryptedEnvelopeDocument
import id.andriawan.cofinance.data.remote.FinanceCollection
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor

/**
 * The store as migration needs to see it: field-level, not document-level.
 *
 * `FinanceDocumentStore` cannot serve migration, and the difference is the whole point of Decision 6.
 * That port reads a document as an [EncryptedEnvelopeDocument] and writes one with replace
 * semantics, which can neither recover the plaintext fields an earlier build left behind nor express
 * "add the ciphertext while the plaintext is still there". Migration needs exactly those two things,
 * because the encrypted fields have to land *before* the plaintext ones are removed — a process
 * death between the two steps must leave a readable record rather than a destroyed one.
 *
 * So [writeEnvelope] merges rather than replaces, [removePlaintextFields] is a separate call, and
 * nothing here offers a way to do both at once. A caller cannot accidentally delete first, because
 * there is no call that deletes and writes together and no call that deletes a whole document.
 */
interface PlaintextFinanceDocumentStore {

    /**
     * Returns every document in [collection], each reporting whether it carries the envelope marker,
     * whether any of [legacy]'s fields are still present, and the legacy payload when it can be read.
     *
     * The two flags are answered from field presence rather than from decoding, so a document whose
     * stored values have gone bad is still correctly classified as unmigrated instead of vanishing
     * from the scan.
     */
    suspend fun <T> readDocuments(
        collection: FinanceCollection,
        legacy: DeserializationStrategy<T>
    ): List<ScannedFinanceDocument<T>>

    /**
     * Adds [document]'s encrypted fields to the document at [id], leaving every other field in place.
     *
     * Merge rather than replace is deliberate. Replacing would delete the plaintext in the same
     * operation that writes the ciphertext, which reads as safe but removes the intermediate state
     * that makes an interrupted run recoverable: a record carrying both is one [removePlaintextFields]
     * call from being finished, whereas a record whose write half-failed under replace semantics is
     * gone.
     */
    suspend fun writeEnvelope(
        collection: FinanceCollection,
        id: String,
        document: EncryptedEnvelopeDocument
    )

    /**
     * Removes [fields] from the document at [id], leaving the document and its other fields in place.
     *
     * Only ever called once that document's ciphertext is stored.
     */
    suspend fun removePlaintextFields(
        collection: FinanceCollection,
        id: String,
        fields: Set<String>
    )
}

/**
 * One stored document as the migration scan sees it.
 *
 * The three states below are exhaustive and each has a distinct remedy, which is why they are
 * separate flags rather than a single boolean: an untouched plaintext document has to be encrypted
 * and then cleaned, a document carrying both has already been encrypted and only needs cleaning, and
 * a document carrying only the envelope needs nothing.
 */
data class ScannedFinanceDocument<T>(
    val id: String,
    /** True when [EncryptedEnvelopeDocument.ENVELOPE_VERSION_FIELD] is present. */
    val carriesEnvelope: Boolean,
    /** True when any legacy plaintext field is still present. */
    val carriesPlaintext: Boolean,
    /** The legacy payload, or null when the stored fields cannot be read as one. */
    val plaintext: T?
) {

    /** Nothing to do: the record is encrypted and no plaintext field survives beside it. */
    val isMigrated: Boolean get() = carriesEnvelope && !carriesPlaintext

    /**
     * Never encrypted. Per Decision 7 the absence of the envelope version is the marker, so this is
     * the only question the resume path asks — there is no separate progress record to corrupt.
     */
    val needsConversion: Boolean get() = !carriesEnvelope

    /**
     * Encrypted, but its plaintext is still there — the exact state a process death between the two
     * write steps leaves behind. Finishing it is a delete, never a re-encryption.
     */
    val needsPlaintextRemoval: Boolean get() = carriesEnvelope && carriesPlaintext
}

/**
 * The field names a serializer reads, which is where migration takes the plaintext field list from.
 *
 * Deriving it beats a hand-written list: the legacy shapes in `data/model/document` are the
 * definition of what an earlier build wrote, so a field added to or removed from one of them cannot
 * fall out of step with what migration deletes.
 */
fun SerialDescriptor.fieldNames(): Set<String> =
    (0 until elementsCount).mapTo(LinkedHashSet(elementsCount)) { getElementName(it) }
