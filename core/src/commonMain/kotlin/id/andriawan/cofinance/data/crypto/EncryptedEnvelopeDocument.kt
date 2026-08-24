package id.andriawan.cofinance.data.crypto

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The stored form of an [EncryptedEnvelope], shaped for a backend document.
 *
 * Binary fields are Base64 so they survive a JSON-shaped store. The presence of
 * [ENVELOPE_VERSION_FIELD] is what distinguishes an encrypted document from a plaintext one left
 * behind by an earlier build, so migration scans for its absence rather than tracking state
 * elsewhere.
 */
@Serializable
data class EncryptedEnvelopeDocument(
    @SerialName(ENVELOPE_VERSION_FIELD)
    val envelopeVersion: Int = 0,
    @SerialName(KEY_ID_FIELD)
    val keyId: String = "",
    val nonce: String = "",
    val ciphertext: String = ""
) {
    companion object {
        const val ENVELOPE_VERSION_FIELD = "envelope_version"
        const val KEY_ID_FIELD = "key_id"
    }
}

@OptIn(ExperimentalEncodingApi::class)
fun EncryptedEnvelope.toDocument(): EncryptedEnvelopeDocument = EncryptedEnvelopeDocument(
    envelopeVersion = version,
    keyId = keyId,
    nonce = Base64.encode(nonce),
    ciphertext = Base64.encode(ciphertext)
)

/**
 * Rebuilds an envelope from its stored form.
 *
 * @throws EncryptedRecordException when the document is not a well-formed envelope, which covers a
 * plaintext document reaching a decrypt path as well as one whose fields have been tampered with.
 */
@OptIn(ExperimentalEncodingApi::class)
fun EncryptedEnvelopeDocument.toEnvelope(): EncryptedEnvelope = try {
    EncryptedEnvelope(
        version = envelopeVersion,
        keyId = keyId,
        nonce = Base64.decode(nonce),
        ciphertext = Base64.decode(ciphertext)
    )
} catch (cause: IllegalArgumentException) {
    throw EncryptedRecordException("Stored document is not a well-formed encrypted envelope", cause)
}
