package id.andriawan.cofinance.data.migration

import id.andriawan.cofinance.data.crypto.EncryptedEnvelopeDocument
import id.andriawan.cofinance.data.crypto.KeyMaterialDocument
import id.andriawan.cofinance.data.remote.FinanceCollection
import id.andriawan.cofinance.data.remote.FinanceDocumentStore
import id.andriawan.cofinance.data.remote.StoredFinanceDocument
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * A store that holds documents as loose fields, which is the only representation these tests can be
 * written against.
 *
 * `FakeFinanceDocumentStore` holds `EncryptedEnvelopeDocument` values, so it cannot represent the
 * plaintext documents an earlier build wrote, nor the intermediate document carrying an envelope and
 * plaintext at once — and that intermediate is exactly what the encrypt-then-delete ordering exists
 * to produce. This fake keeps a field map per document instead, so a test can read back precisely
 * what a backend export would show at any point in the run.
 *
 * It implements [FinanceDocumentStore] as well so the real `KeyMaterialGate` and the real encrypted
 * data sources can run against the same documents. That is what lets a test finish a migration and
 * then read the records back through the production read path rather than through a test-only one.
 *
 * [operations] is the point of the whole class: an ordered log of every mutation, so "the envelope
 * was written before the plaintext was removed" is asserted against the sequence that actually
 * occurred rather than inferred from the end state. An end-state assertion cannot tell safe ordering
 * from unsafe ordering, because both orderings end in the same place.
 */
class FakePlaintextFinanceDocumentStore :
    PlaintextFinanceDocumentStore,
    FinanceDocumentStore {

    private val documents =
        mutableMapOf<FinanceCollection, LinkedHashMap<String, MutableMap<String, JsonElement>>>()

    private var keyMaterial: KeyMaterialDocument? = null

    private var remainingOperations: Int = Int.MAX_VALUE

    /** Every mutation in order: `write <path>`, `delete <path>`, `set <path>`, `write key_material`. */
    val operations: MutableList<String> = mutableListOf()

    /** Called after each accepted mutation, so a test can sample state at a known point in the run. */
    var onOperation: ((String) -> Unit)? = null

    /**
     * Stops the store after [count] further mutations, standing in for the process dying mid-run.
     *
     * The refused mutation does not happen, so the stored state is exactly what a real interruption
     * at that instant would leave.
     */
    fun interruptAfter(count: Int) {
        remainingOperations = count
    }

    /** Lets the store accept mutations again, standing in for the next launch. */
    fun relaunch() {
        remainingOperations = Int.MAX_VALUE
        operations.clear()
    }

    override suspend fun <T> readDocuments(
        collection: FinanceCollection,
        legacy: DeserializationStrategy<T>
    ): List<ScannedFinanceDocument<T>> {
        val plaintextFields = legacy.descriptor.fieldNames()
        return fieldsIn(collection).map { (id, fields) ->
            val carriesPlaintext = fields.keys.any { it in plaintextFields }
            ScannedFinanceDocument(
                id = id,
                carriesEnvelope = fields.containsKey(EncryptedEnvelopeDocument.ENVELOPE_VERSION_FIELD),
                carriesPlaintext = carriesPlaintext,
                plaintext = if (carriesPlaintext) decodeOrNull(legacy, fields) else null
            )
        }
    }

    override suspend fun writeEnvelope(
        collection: FinanceCollection,
        id: String,
        document: EncryptedEnvelopeDocument
    ) {
        mutate("write ${collection.path}/$id") {
            // Merge, matching the port's contract: whatever plaintext is there stays there until the
            // separate removal call.
            documentAt(collection, id).putAll(encodeToFields(EncryptedEnvelopeDocument.serializer(), document))
        }
    }

    override suspend fun removePlaintextFields(
        collection: FinanceCollection,
        id: String,
        fields: Set<String>
    ) {
        mutate("delete ${collection.path}/$id") {
            documentAt(collection, id).keys.removeAll(fields)
        }
    }

    override suspend fun readKeyMaterial(): KeyMaterialDocument? = keyMaterial

    override suspend fun writeKeyMaterial(material: KeyMaterialDocument) {
        mutate(KEY_MATERIAL_ENTRY) { keyMaterial = material }
    }

    override suspend fun readDocuments(collection: FinanceCollection): List<StoredFinanceDocument> =
        fieldsIn(collection).map { (id, fields) ->
            StoredFinanceDocument(
                id = id,
                document = JSON.decodeFromJsonElement(
                    EncryptedEnvelopeDocument.serializer(),
                    JsonObject(fields)
                )
            )
        }

    override suspend fun writeDocument(
        collection: FinanceCollection,
        id: String,
        document: EncryptedEnvelopeDocument
    ) {
        mutate("set ${collection.path}/$id") {
            val target = documentAt(collection, id)
            target.clear()
            target.putAll(encodeToFields(EncryptedEnvelopeDocument.serializer(), document))
        }
    }

    /** Places a plaintext document as an earlier build would have written it. */
    fun <T> seedPlaintext(
        collection: FinanceCollection,
        id: String,
        serializer: SerializationStrategy<T>,
        value: T
    ) {
        documentAt(collection, id).putAll(encodeToFields(serializer, value))
    }

    /** Places an already-encrypted document, as a record written after this change would be. */
    fun seedEncrypted(collection: FinanceCollection, id: String, document: EncryptedEnvelopeDocument) {
        documentAt(collection, id).putAll(
            encodeToFields(EncryptedEnvelopeDocument.serializer(), document)
        )
    }

    /** Places raw fields, for a document whose stored values no longer read as a finance record. */
    fun seedFields(collection: FinanceCollection, id: String, fields: Map<String, JsonElement>) {
        documentAt(collection, id).putAll(fields)
    }

    /** Places key material without logging a write, standing in for a completed earlier setup. */
    fun seedKeyMaterial(material: KeyMaterialDocument) {
        keyMaterial = material
    }

    fun ids(collection: FinanceCollection): Set<String> = fieldsIn(collection).keys.toSet()

    fun fieldNames(collection: FinanceCollection, id: String): Set<String> =
        documentOrFail(collection, id).keys.toSet()

    /** The stored document as serialized text, which is the form a backend export would show. */
    fun storedText(collection: FinanceCollection, id: String): String =
        JSON.encodeToString(JsonObject.serializer(), JsonObject(documentOrFail(collection, id)))

    private fun <T> decodeOrNull(
        deserializer: DeserializationStrategy<T>,
        fields: Map<String, JsonElement>
    ): T? = try {
        JSON.decodeFromJsonElement(deserializer, JsonObject(fields))
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

    private fun <T> encodeToFields(
        serializer: SerializationStrategy<T>,
        value: T
    ): Map<String, JsonElement> =
        JSON.encodeToJsonElement(serializer, value) as JsonObject

    private inline fun mutate(entry: String, apply: () -> Unit) {
        if (remainingOperations <= 0) throw StoreInterrupted(entry)
        remainingOperations--
        apply()
        operations += entry
        onOperation?.invoke(entry)
    }

    private fun fieldsIn(collection: FinanceCollection): Map<String, MutableMap<String, JsonElement>> =
        documents[collection].orEmpty()

    private fun documentAt(
        collection: FinanceCollection,
        id: String
    ): MutableMap<String, JsonElement> =
        documents.getOrPut(collection) { LinkedHashMap() }.getOrPut(id) { LinkedHashMap() }

    private fun documentOrFail(
        collection: FinanceCollection,
        id: String
    ): Map<String, JsonElement> = requireNotNull(documents[collection]?.get(id)) {
        "No document $id in ${collection.path}"
    }

    companion object {
        const val KEY_MATERIAL_ENTRY = "write key_material"

        private val JSON = Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
        }
    }
}

/** Raised where a real process would simply have stopped existing. */
class StoreInterrupted(entry: String) : RuntimeException("Store stopped before $entry")
