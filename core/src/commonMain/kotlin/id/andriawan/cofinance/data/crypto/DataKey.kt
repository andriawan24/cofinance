package id.andriawan.cofinance.data.crypto

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.AES
import dev.whyoleg.cryptography.random.CryptographyRandom
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * The single symmetric key every finance record is encrypted under.
 *
 * Records are encrypted with this key rather than directly with an asymmetric key: asymmetric
 * primitives are size-limited and slow per record, and keeping bulk encryption symmetric is what
 * allows a second device to be added later by wrapping this key again rather than re-encrypting
 * every record. The wrapping layer that protects this key lives above it and is not this type's
 * concern.
 *
 * The key is held in memory only. Nothing here writes it anywhere.
 */
class DataKey internal constructor(val id: String, internal val key: AES.GCM.Key) {
    suspend fun exportRawBytes(): ByteArray = key.encodeToByteArray(AES.Key.Format.RAW)

    override fun toString(): String = "DataKey(id=$id)"

    companion object {
        private val KEY_SIZE = AES.Key.Size.B256

        private val algorithm get() = CryptographyProvider.Default.get(AES.GCM)

        suspend fun generate(): DataKey = DataKey(
            id = randomKeyId(),
            key = algorithm.keyGenerator(KEY_SIZE).generateKey()
        )

        suspend fun fromRawBytes(id: String, raw: ByteArray): DataKey {
            require(raw.size == RAW_KEY_BYTES) { "Data key must be $RAW_KEY_BYTES bytes" }

            return DataKey(
                id = id,
                key = algorithm.keyDecoder().decodeFromByteArray(AES.Key.Format.RAW, raw)
            )
        }

        @OptIn(ExperimentalEncodingApi::class)
        private fun randomKeyId(): String =
            Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT)
                .encode(CryptographyRandom.nextBytes(KEY_ID_BYTES))

        private const val RAW_KEY_BYTES = 32
        private const val KEY_ID_BYTES = 8
    }
}
