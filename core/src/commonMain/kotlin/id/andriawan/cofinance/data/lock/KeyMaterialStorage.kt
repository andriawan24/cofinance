package id.andriawan.cofinance.data.lock

import id.andriawan.cofinance.data.crypto.KeyMaterialDocument
import kotlinx.serialization.json.Json

/**
 * The bytes of the local key material document, and where the platform keeps them.
 *
 * This is deliberately a byte store rather than a document store. What differs between Android and
 * iOS is only *where* a blob may durably live — an app-private file against a Keychain item — and
 * nothing about how key material is encoded, so the encoding stays in `commonMain` where a test can
 * read it. It also means the platform halves have no way to see, and therefore no way to leak, the
 * structure of what they hold.
 */
interface KeyMaterialStorage {
    suspend fun read(): ByteArray?
    suspend fun write(bytes: ByteArray)
    suspend fun clear()
}

/** The platform's durable storage for this installation's key material. */
expect fun createKeyMaterialStorage(): KeyMaterialStorage

/**
 * The [LocalKeyMaterialStore] the app runs on: a [KeyMaterialDocument] serialized into
 * [KeyMaterialStorage].
 *
 * ## Why this exists at all
 *
 * The device wrap is the copy that opens the data without asking for twelve words, and the backend
 * never holds it — only the recovery-phrase wrap is uploaded. So if the device wrap does not
 * outlive the process, every relaunch presents setup or restore and the feature reads as broken.
 * That is what the in-memory placeholder this replaces actually did.
 *
 * ## Why nothing here is encrypted a second time
 *
 * Every element of [KeyMaterialDocument.wrappedKeys] is already sealed: the device wrap under an
 * ECDH agreement with platform key material that cannot leave the device, the PIN wrap under
 * `HKDF(scrypt(pin) || device secret)`. A copy of these bytes taken off the device opens nothing.
 * Wrapping the blob in a second layer would add a key whose loss would strand the wraps behind it,
 * and would buy no confidentiality that the wraps do not already have.
 *
 * The type system carries the other half of the guarantee: [KeyMaterialDocument] has no
 * representation for an unwrapped key — see `WrappedDataKey` — so there is no value this store
 * could be handed that contains one, and none it could hand back.
 *
 * ## Failure behaviour
 *
 * Bytes that no longer parse read as `null` rather than throwing. A launch that cannot decode its
 * key material has to reach setup or restore, which an absent document already produces; throwing
 * would instead produce a device that cannot start. The written bytes are only ever replaced whole,
 * so a partial document is not a state this can reach on its own.
 */
class StoredLocalKeyMaterialStore(
    private val storage: KeyMaterialStorage,
    private val json: Json = KEY_MATERIAL_JSON
) : LocalKeyMaterialStore {

    override suspend fun read(): KeyMaterialDocument? {
        val bytes = storage.read() ?: return null
        return try {
            json.decodeFromString(
                KeyMaterialDocument.serializer(),
                bytes.decodeToString()
            )
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun write(document: KeyMaterialDocument) {
        storage.write(
            json.encodeToString(KeyMaterialDocument.serializer(), document).encodeToByteArray()
        )
    }

    override suspend fun erase() {
        storage.clear()
    }

    companion object {
        val KEY_MATERIAL_JSON: Json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
}
