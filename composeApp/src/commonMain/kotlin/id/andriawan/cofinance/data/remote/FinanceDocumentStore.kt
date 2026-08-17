package id.andriawan.cofinance.data.remote

import id.andriawan.cofinance.data.crypto.EncryptedEnvelopeDocument
import id.andriawan.cofinance.data.crypto.KeyMaterialDocument

/** The two synchronized finance collections beneath the authenticated user's document. */
enum class FinanceCollection(val path: String) {
    ACCOUNTS("accounts"),
    TRANSACTIONS("transactions")
}

/**
 * One stored finance document: its plaintext identifier and its encrypted payload.
 *
 * The identifier stays readable because it is a random value the app generates and is what addresses
 * the document; every field carrying a finance value is inside [document].
 */
data class StoredFinanceDocument(val id: String, val document: EncryptedEnvelopeDocument)

/**
 * The per-user document store, as the encrypted data sources need to see it.
 *
 * The port exists so the encryption behavior is exercisable against a fake rather than a backend —
 * the requirement that synchronized documents carry no readable finance value is only meaningful if
 * a test can read what was actually stored. It also confines every Firestore type to one
 * implementation, so nothing above it can reach for a server-side query.
 *
 * There is deliberately no ordering, filtering, or pagination on the read: the stored fields are
 * ciphertext, so a backend cannot sort them, and every user-facing ordering already comes from Room.
 * A future caller cannot ask for server-side ordering because there is nowhere to ask.
 */
interface FinanceDocumentStore {

    /** Returns the stored key material for this user, or null when setup has never run. */
    suspend fun readKeyMaterial(): KeyMaterialDocument?

    /** Writes [material] beneath this user's document, replacing what was there. */
    suspend fun writeKeyMaterial(material: KeyMaterialDocument)

    /** Returns every document in [collection], in whatever order the backend hands them back. */
    suspend fun readDocuments(collection: FinanceCollection): List<StoredFinanceDocument>

    /** Writes one encrypted [document] under [id] in [collection]. */
    suspend fun writeDocument(
        collection: FinanceCollection,
        id: String,
        document: EncryptedEnvelopeDocument
    )
}
