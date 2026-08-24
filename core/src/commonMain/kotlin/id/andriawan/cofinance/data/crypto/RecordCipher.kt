package id.andriawan.cofinance.data.crypto

import dev.whyoleg.cryptography.DelicateCryptographyApi
import dev.whyoleg.cryptography.random.CryptographyRandom
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json

class RecordCipher(
    private val random: CryptographyRandom = CryptographyRandom,
    private val json: Json = RECORD_JSON
) {
    @OptIn(DelicateCryptographyApi::class)
    suspend fun <T> seal(
        value: T,
        serializer: SerializationStrategy<T>,
        key: DataKey
    ): EncryptedEnvelope {
        val plaintext = json.encodeToString(serializer, value).encodeToByteArray()
        val nonce = random.nextBytes(EncryptedEnvelope.NONCE_SIZE)
        val ciphertext = key.key.cipher().encryptWithIv(iv = nonce, plaintext = plaintext)
        return EncryptedEnvelope(
            version = EncryptedEnvelope.CURRENT_VERSION,
            keyId = key.id,
            nonce = nonce,
            ciphertext = ciphertext
        )
    }

    @OptIn(DelicateCryptographyApi::class)
    suspend fun <T> open(
        envelope: EncryptedEnvelope,
        deserializer: DeserializationStrategy<T>,
        key: DataKey
    ): T {
        if (envelope.version != EncryptedEnvelope.CURRENT_VERSION) {
            throw EncryptedRecordException(
                "Unsupported envelope version ${envelope.version}"
            )
        }
        if (envelope.keyId != key.id) {
            throw EncryptedRecordException(
                "Record was sealed under key ${envelope.keyId}, not ${key.id}"
            )
        }
        val plaintext = try {
            key.key.cipher().decryptWithIv(iv = envelope.nonce, ciphertext = envelope.ciphertext)
        } catch (cause: Throwable) {
            throw EncryptedRecordException("Record failed authentication", cause)
        }
        return try {
            json.decodeFromString(deserializer, plaintext.decodeToString())
        } catch (cause: SerializationException) {
            throw EncryptedRecordException(
                "Decrypted record is not a readable finance record",
                cause
            )
        }
    }

    companion object {
        private val RECORD_JSON = Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
        }
    }
}
