package id.andriawan.cofinance.data.crypto

import dev.whyoleg.cryptography.random.CryptographyRandom
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

@OptIn(ExperimentalEncodingApi::class)
class KeyMaterialDocumentTest {

    @Test
    fun storedMaterialCarriesNoUnwrappedKey() = runTest {
        val dataKey = DataKey.generate()
        val raw = dataKey.exportRawBytes()

        val document = KeyMaterialDocument(
            keyMaterialVersion = KeyMaterialDocument.CURRENT_VERSION,
            wrappedKeys = listOf(
                deviceWrap(dataKey.id, raw),
                phraseWrap(dataKey.id, raw)
            )
        )
        val encoded = Json.encodeToString(KeyMaterialDocument.serializer(), document)

        assertFalse(encoded.contains(Base64.encode(raw)), "Encoded material leaked the data key")
        assertFalse(
            encoded.contains(Base64.UrlSafe.encode(raw)),
            "Encoded material leaked the data key"
        )
    }

    @Test
    fun noStoredFieldCanHoldAnUnwrappedKey() {
        val documentFields = Json
            .encodeToJsonElement(
                KeyMaterialDocument.serializer(),
                KeyMaterialDocument(
                    keyMaterialVersion = KeyMaterialDocument.CURRENT_VERSION,
                    wrappedKeys = listOf(deviceWrap("key-1", ByteArray(32) { 1 }))
                )
            )
            .jsonObject

        assertEquals(
            setOf(KeyMaterialDocument.VERSION_FIELD, KeyMaterialDocument.WRAPPED_KEYS_FIELD),
            documentFields.keys
        )

        val wrapFields = Json
            .encodeToJsonElement(WrappedDataKey.serializer(), deviceWrap("key-1", ByteArray(32)))
            .jsonObject

        assertEquals(
            setOf(
                WrappedDataKey.WRAP_TYPE_FIELD,
                WrappedDataKey.KEY_ID_FIELD,
                WrappedDataKey.WRAPPED_KEY_FIELD,
                WrappedDataKey.WRAP_PARAMETERS_FIELD
            ),
            wrapFields.keys
        )
    }

    @Test
    fun keyMaterialAcceptsMoreThanTwoWrappedCopies() {
        val raw = ByteArray(32) { it.toByte() }
        val document = KeyMaterialDocument(
            keyMaterialVersion = KeyMaterialDocument.CURRENT_VERSION,
            wrappedKeys = listOf(
                deviceWrap("key-1", raw),
                phraseWrap("key-1", raw),
                pinWrap("key-1", raw),
                // A second device, which is the case the list shape exists for: pairing appends a
                // copy and re-encrypts nothing.
                deviceWrap("key-1", raw)
            )
        )

        val encoded = Json.encodeToString(KeyMaterialDocument.serializer(), document)
        val decoded = Json.decodeFromString(KeyMaterialDocument.serializer(), encoded)

        assertEquals(4, decoded.wrappedKeys.size)
        assertEquals(document, decoded)
        assertEquals(2, decoded.wrapsOf(KeyWrapType.Device).size)
    }

    @Test
    fun onlyThePhraseWrapIsUploadable() {
        val raw = ByteArray(32) { 7 }
        val document = KeyMaterialDocument(
            keyMaterialVersion = KeyMaterialDocument.CURRENT_VERSION,
            wrappedKeys = listOf(
                deviceWrap("key-1", raw),
                phraseWrap("key-1", raw),
                pinWrap("key-1", raw)
            )
        )

        val uploadable = document.uploadableKeyMaterial()

        assertEquals(
            listOf(KeyWrapType.RecoveryPhrase.id),
            uploadable.wrappedKeys.map { it.wrapType }
        )
        assertEquals(KeyMaterialDocument.CURRENT_VERSION, uploadable.keyMaterialVersion)
    }

    @Test
    fun uploadableSubsetExcludesWrapTypesThisBuildDoesNotKnow() {
        val document = Json.decodeFromString(KeyMaterialDocument.serializer(), STORED_WITH_UNKNOWN)

        assertTrue(document.uploadableKeyMaterial().wrappedKeys.all {
            it.type == KeyWrapType.RecoveryPhrase
        })
    }

    @Test
    fun unknownWrapTypeDecodesAndSurvivesRewrite() {
        val document = Json.decodeFromString(KeyMaterialDocument.serializer(), STORED_WITH_UNKNOWN)

        val unknown = document.wrappedKeys.single { it.type is KeyWrapType.Unrecognized }
        assertEquals(KeyWrapType.Unrecognized("hardware_token"), unknown.type)
        assertContentEquals(ByteArray(4) { 0 }, unknown.parameter("token_challenge"))

        val rewritten = Json.decodeFromString(
            KeyMaterialDocument.serializer(),
            Json.encodeToString(KeyMaterialDocument.serializer(), document)
        )
        assertEquals(document, rewritten)
    }

    @Test
    fun wrapCarriesTheParametersItsUnwrapNeeds() {
        val nonce = ByteArray(12) { 3 }
        val ephemeral = ByteArray(65) { 4 }
        val wrap = WrappedDataKey.of(
            type = KeyWrapType.Device,
            keyId = "key-1",
            wrappedKey = ByteArray(48) { 5 },
            parameters = mapOf(
                WrappedDataKey.NONCE_PARAMETER to nonce,
                WrappedDataKey.EPHEMERAL_PUBLIC_KEY_PARAMETER to ephemeral
            )
        )

        assertContentEquals(nonce, wrap.parameter(WrappedDataKey.NONCE_PARAMETER))
        assertContentEquals(
            ephemeral,
            wrap.parameter(WrappedDataKey.EPHEMERAL_PUBLIC_KEY_PARAMETER)
        )
        assertNull(wrap.parameter(WrappedDataKey.SALT_PARAMETER))
    }

    @Test
    fun malformedStoredBytesAreRejectedAtTheUnwrapBoundary() {
        val wrap = WrappedDataKey(
            wrapType = KeyWrapType.RecoveryPhrase.id,
            keyId = "key-1",
            wrappedKey = "not base64!!",
            wrapParameters = mapOf(WrappedDataKey.SALT_PARAMETER to "also not base64!!")
        )

        assertFailsWith<EncryptedRecordException> { wrap.wrappedKeyBytes() }
        assertFailsWith<EncryptedRecordException> { wrap.parameter(WrappedDataKey.SALT_PARAMETER) }
    }

    @Test
    fun descriptionsDoNotReproduceAnyBytes() {
        val raw = ByteArray(32) { 9 }
        val wrap = deviceWrap("key-1", raw)
        val document = KeyMaterialDocument(
            keyMaterialVersion = KeyMaterialDocument.CURRENT_VERSION,
            wrappedKeys = listOf(wrap)
        )

        assertFalse(wrap.toString().contains(wrap.wrappedKey))
        assertFalse(document.toString().contains(wrap.wrappedKey))
    }

    // Stands in for the wrapping operations that arrive with tasks 2.5 and 3.2. Only the container
    // is under test here, so the bytes need to be indistinguishable from the key rather than
    // genuinely sealed.
    private fun pretendWrapped(raw: ByteArray): ByteArray {
        val pad = CryptographyRandom.nextBytes(raw.size)
        return ByteArray(raw.size) { index -> (raw[index].toInt() xor pad[index].toInt()).toByte() }
    }

    private fun deviceWrap(keyId: String, raw: ByteArray): WrappedDataKey = WrappedDataKey.of(
        type = KeyWrapType.Device,
        keyId = keyId,
        wrappedKey = pretendWrapped(raw),
        parameters = mapOf(
            WrappedDataKey.NONCE_PARAMETER to ByteArray(12) { 1 },
            WrappedDataKey.EPHEMERAL_PUBLIC_KEY_PARAMETER to ByteArray(65) { 2 }
        )
    )

    private fun phraseWrap(keyId: String, raw: ByteArray): WrappedDataKey = WrappedDataKey.of(
        type = KeyWrapType.RecoveryPhrase,
        keyId = keyId,
        wrappedKey = pretendWrapped(raw),
        parameters = mapOf(
            WrappedDataKey.NONCE_PARAMETER to ByteArray(12) { 3 },
            WrappedDataKey.SALT_PARAMETER to ByteArray(16) { 4 }
        )
    )

    private fun pinWrap(keyId: String, raw: ByteArray): WrappedDataKey = WrappedDataKey.of(
        type = KeyWrapType.Pin,
        keyId = keyId,
        wrappedKey = pretendWrapped(raw),
        parameters = mapOf(
            WrappedDataKey.NONCE_PARAMETER to ByteArray(12) { 5 },
            WrappedDataKey.SALT_PARAMETER to ByteArray(16) { 6 }
        )
    )

    private companion object {
        /** Key material as a later build might have written it, read by this one. */
        val STORED_WITH_UNKNOWN = """
            {
              "key_material_version": 1,
              "wrapped_keys": [
                {
                  "wrap_type": "recovery_phrase",
                  "key_id": "key-1",
                  "wrapped_key": "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                  "wrap_parameters": { "nonce": "AAAAAAAAAAAAAAAA", "salt": "AAAAAAAAAAAAAAAA" }
                },
                {
                  "wrap_type": "hardware_token",
                  "key_id": "key-1",
                  "wrapped_key": "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                  "wrap_parameters": { "token_challenge": "AAAAAA==" }
                }
              ]
            }
        """.trimIndent()
    }
}
