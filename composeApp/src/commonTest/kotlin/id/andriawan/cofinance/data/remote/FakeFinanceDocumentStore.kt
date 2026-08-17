package id.andriawan.cofinance.data.remote

import id.andriawan.cofinance.data.crypto.EncryptedEnvelopeDocument
import id.andriawan.cofinance.data.crypto.KeyMaterialDocument
import kotlinx.serialization.json.Json

/**
 * An in-memory [FinanceDocumentStore] that keeps what was stored and the order it was stored in.
 *
 * Two things make this the right shape for these tests. It exposes documents as the serialized text
 * a backend would hold, so "the stored document carries no readable finance value" is asserted
 * against what would actually be exported rather than against an object graph. And it records an
 * ordered write log across both key material and records, which is the only way to assert that key
 * material was written before the first encrypted record rather than merely alongside it.
 */
class FakeFinanceDocumentStore : FinanceDocumentStore {

    private val documents = mutableMapOf<FinanceCollection, MutableMap<String, EncryptedEnvelopeDocument>>()
    private var keyMaterial: KeyMaterialDocument? = null

    /** Every write in order: `key_material` for key material, `<collection>/<id>` for a record. */
    val writeLog: MutableList<String> = mutableListOf()

    override suspend fun readKeyMaterial(): KeyMaterialDocument? = keyMaterial

    override suspend fun writeKeyMaterial(material: KeyMaterialDocument) {
        keyMaterial = material
        writeLog += KEY_MATERIAL_ENTRY
    }

    override suspend fun readDocuments(collection: FinanceCollection): List<StoredFinanceDocument> =
        documents[collection].orEmpty().map { (id, document) -> StoredFinanceDocument(id, document) }

    override suspend fun writeDocument(
        collection: FinanceCollection,
        id: String,
        document: EncryptedEnvelopeDocument
    ) {
        documents.getOrPut(collection) { mutableMapOf() }[id] = document
        writeLog += "${collection.path}/$id"
    }

    /** Places [documents] in [collection] without logging a write, standing in for cloud state. */
    fun seed(collection: FinanceCollection, documents: List<StoredFinanceDocument>) {
        val target = this.documents.getOrPut(collection) { mutableMapOf() }
        documents.forEach { target[it.id] = it.document }
    }

    /** Places [material] without logging a write, standing in for a completed earlier setup. */
    fun seedKeyMaterial(material: KeyMaterialDocument) {
        keyMaterial = material
    }

    /** Empties the key material, standing in for a scope that has never had setup run against it. */
    fun clearKeyMaterial() {
        keyMaterial = null
    }

    fun documentIds(collection: FinanceCollection): Set<String> =
        documents[collection].orEmpty().keys.toSet()

    fun storedDocuments(collection: FinanceCollection): List<StoredFinanceDocument> =
        documents[collection].orEmpty().map { (id, document) -> StoredFinanceDocument(id, document) }

    /** The stored document as serialized text, which is the form a backend export would show. */
    fun storedText(collection: FinanceCollection, id: String): String = Json.encodeToString(
        EncryptedEnvelopeDocument.serializer(),
        requireNotNull(documents[collection]?.get(id)) { "No document $id in ${collection.path}" }
    )

    fun storedKeyMaterialText(): String = Json.encodeToString(
        KeyMaterialDocument.serializer(),
        requireNotNull(keyMaterial) { "No key material stored" }
    )

    companion object {
        const val KEY_MATERIAL_ENTRY = "key_material"
    }
}
