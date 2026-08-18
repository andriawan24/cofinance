package id.andriawan.cofinance.data.crypto

import id.andriawan.cofinance.data.model.response.AccountResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

class EncryptedEnvelopeTest {

    @Test
    fun documentCarriesVersionKeyIdNonceAndCiphertext() = runTest {
        val key = DataKey.generate()
        val envelope = RecordCipher().seal(
            AccountResponse(id = "account-1", balance = 1_000L),
            AccountResponse.serializer(),
            key
        )

        val fields = Json
            .encodeToJsonElement(EncryptedEnvelopeDocument.serializer(), envelope.toDocument())
            .jsonObject
            .keys

        assertEquals(
            setOf(
                EncryptedEnvelopeDocument.ENVELOPE_VERSION_FIELD,
                EncryptedEnvelopeDocument.KEY_ID_FIELD,
                "nonce",
                "ciphertext"
            ),
            fields
        )
    }

    @Test
    fun envelopeRoundTripsThroughItsStoredForm() = runTest {
        val key = DataKey.generate()
        val envelope = RecordCipher().seal(
            AccountResponse(id = "account-1", balance = 1_000L),
            AccountResponse.serializer(),
            key
        )

        assertEquals(envelope, envelope.toDocument().toEnvelope())
    }

    @Test
    fun plaintextDocumentIsIdentifiableByMissingVersion() {
        val plaintext = EncryptedEnvelopeDocument()

        assertEquals(0, plaintext.envelopeVersion)
        assertFailsWith<EncryptedRecordException> { plaintext.toEnvelope() }
    }

    @Test
    fun malformedStoredFieldsAreRejected() {
        val malformed = EncryptedEnvelopeDocument(
            envelopeVersion = EncryptedEnvelope.CURRENT_VERSION,
            keyId = "key-1",
            nonce = "not base64!!",
            ciphertext = "also not base64!!"
        )

        assertFailsWith<EncryptedRecordException> { malformed.toEnvelope() }
    }

    @Test
    fun nonceMustBeTwelveBytes() {
        assertFailsWith<IllegalArgumentException> {
            EncryptedEnvelope(
                version = EncryptedEnvelope.CURRENT_VERSION,
                keyId = "key-1",
                nonce = ByteArray(8),
                ciphertext = ByteArray(16) { 1 }
            )
        }
    }

    @Test
    fun descriptionDoesNotReproduceRecordBytes() {
        val envelope = EncryptedEnvelope(
            version = EncryptedEnvelope.CURRENT_VERSION,
            keyId = "key-1",
            nonce = ByteArray(EncryptedEnvelope.NONCE_SIZE) { 7 },
            ciphertext = ByteArray(16) { 9 }
        )

        assertTrue(envelope.toString().contains("ciphertextBytes=16"))
    }
}
