package id.andriawan.cofinance.data.migration

import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.FieldValueSerializer
import dev.gitlive.firebase.firestore.FirebaseFirestore
import id.andriawan.cofinance.data.crypto.EncryptedEnvelopeDocument
import id.andriawan.cofinance.data.remote.FinanceCollection
import id.andriawan.cofinance.data.session.SessionPolicy
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.DeserializationStrategy

/**
 * The Firestore side of migration: field-level reads, a merging write, and a field delete.
 *
 * It sits beside `FirestoreFinanceDocumentStore` rather than inside it because the two need opposite
 * things from the same documents. That store reads and replaces whole encrypted documents, which is
 * right for every write after this change; migration has to see the plaintext an older build left,
 * add ciphertext without disturbing it, and only then remove it. Merging those into one class would
 * mean handing the ordinary synchronization path a way to delete individual finance fields, which
 * nothing above it should be able to do.
 *
 * This class is expected to be deleted once every user has migrated. It is the only remaining reader
 * of the legacy `AccountDocument` and `TransactionDocument` shapes.
 */
class FirestorePlaintextFinanceDocumentStore(
    private val firestore: FirebaseFirestore,
    private val sessionPolicy: SessionPolicy
) : PlaintextFinanceDocumentStore {

    override suspend fun <T> readDocuments(
        collection: FinanceCollection,
        legacy: DeserializationStrategy<T>
    ): List<ScannedFinanceDocument<T>> {
        val plaintextFields = legacy.descriptor.fieldNames()
        return collection(collection).get().documents.map { snapshot ->
            val carriesPlaintext = plaintextFields.any { field -> snapshot.contains(field) }
            ScannedFinanceDocument(
                id = snapshot.id,
                carriesEnvelope = snapshot.contains(EncryptedEnvelopeDocument.ENVELOPE_VERSION_FIELD),
                carriesPlaintext = carriesPlaintext,
                // Only decoded when something is actually there to decode, so an already-migrated
                // document is never asked to produce a legacy payload it does not have.
                plaintext = if (carriesPlaintext) readPlaintext(snapshot, legacy) else null
            )
        }
    }

    override suspend fun writeEnvelope(
        collection: FinanceCollection,
        id: String,
        document: EncryptedEnvelopeDocument
    ) {
        // merge = true, never a plain set: a replacing write would delete the plaintext in the same
        // operation that adds the ciphertext, and a half-applied one would leave nothing behind.
        document(collection, id).set(EncryptedEnvelopeDocument.serializer(), document, merge = true)
    }

    override suspend fun removePlaintextFields(
        collection: FinanceCollection,
        id: String,
        fields: Set<String>
    ) {
        if (fields.isEmpty()) return
        document(collection, id).updateFields {
            fields.forEach { field -> field.to(FieldValueSerializer, FieldValue.delete) }
        }
    }

    /**
     * Returns the legacy payload, or null when the stored fields no longer read as one.
     *
     * Returning null rather than throwing is what keeps one unreadable document from stopping the
     * run: the migrator reports it and leaves it exactly as it found it.
     */
    private fun <T> readPlaintext(
        snapshot: DocumentSnapshot,
        legacy: DeserializationStrategy<T>
    ): T? = try {
        snapshot.data(strategy = legacy)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Exception) {
        null
    }

    private fun document(collection: FinanceCollection, id: String) =
        collection(collection).document(id)

    private fun collection(collection: FinanceCollection) =
        firestore.collection(USERS_COLLECTION)
            .document(sessionPolicy.requireUserId())
            .collection(collection.path)

    private companion object {
        const val USERS_COLLECTION = "users"
    }
}
