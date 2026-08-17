package id.andriawan.cofinance.data.remote

import dev.gitlive.firebase.firestore.FirebaseFirestore
import id.andriawan.cofinance.data.crypto.EncryptedEnvelopeDocument
import id.andriawan.cofinance.data.crypto.KeyMaterialDocument
import id.andriawan.cofinance.data.session.SessionPolicy

/**
 * The Firestore-backed [FinanceDocumentStore], and the only place in the synchronization path that
 * knows Firestore exists.
 *
 * Everything is addressed beneath `users/{uid}`, with the user identifier taken from the session on
 * every call rather than captured once, so a signed-out or switched session cannot write into the
 * previous user's documents. Key material lives in its own document rather than as fields on the
 * user profile, so profile writes and key material writes cannot overwrite one another.
 *
 * Reads carry no `orderBy`: the stored finance fields are ciphertext and a backend cannot order
 * them. Ordering comes from Room, which is where it already came from.
 */
class FirestoreFinanceDocumentStore(
    private val firestore: FirebaseFirestore,
    private val sessionPolicy: SessionPolicy
) : FinanceDocumentStore {

    override suspend fun readKeyMaterial(): KeyMaterialDocument? {
        val snapshot = keyMaterialDocument().get()
        return if (snapshot.exists) snapshot.data<KeyMaterialDocument>() else null
    }

    override suspend fun writeKeyMaterial(material: KeyMaterialDocument) {
        keyMaterialDocument().set(material)
    }

    override suspend fun readDocuments(collection: FinanceCollection): List<StoredFinanceDocument> =
        collection(collection).get().documents.map { snapshot ->
            StoredFinanceDocument(snapshot.id, snapshot.data<EncryptedEnvelopeDocument>())
        }

    override suspend fun writeDocument(
        collection: FinanceCollection,
        id: String,
        document: EncryptedEnvelopeDocument
    ) {
        collection(collection).document(id).set(document)
    }

    private fun collection(collection: FinanceCollection) =
        userDocument().collection(collection.path)

    private fun keyMaterialDocument() =
        userDocument().collection(KEY_MATERIAL_COLLECTION).document(KEY_MATERIAL_DOCUMENT)

    private fun userDocument() =
        firestore.collection(USERS_COLLECTION).document(sessionPolicy.requireUserId())

    private companion object {
        const val USERS_COLLECTION = "users"
        const val KEY_MATERIAL_COLLECTION = "key_material"
        const val KEY_MATERIAL_DOCUMENT = "current"
    }
}
