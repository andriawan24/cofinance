package id.andriawan.cofinance.data.crypto

import dev.whyoleg.cryptography.BinarySize.Companion.bytes
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.DelicateCryptographyApi
import dev.whyoleg.cryptography.algorithms.AES
import dev.whyoleg.cryptography.algorithms.HKDF
import dev.whyoleg.cryptography.algorithms.SHA256

/**
 * Raised when a wrapped copy of the data key cannot be produced, or cannot be opened.
 *
 * Kept apart from [EncryptedRecordException] because the two describe different failures to a
 * caller: a record that fails to open is one unreadable row, whereas a wrap that fails to open is
 * the whole account's data being out of reach through that route, and the screens that react to it
 * are the unlock and restore screens rather than the record reader. The unwrap paths translate the
 * malformed-storage failures [WrappedDataKey] raises into this type, so an unwrap has exactly one
 * failure type to handle.
 */
class KeyWrapException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * The parts every wrap type shares: the HKDF step that turns some input keying material into a
 * wrapping key, and the AES-256-GCM seal applied under it.
 *
 * The wrap types differ only in where their input keying material comes from and in the HKDF `info`
 * they derive under. Keeping the derivation and the seal here rather than in each wrapper is what
 * makes "every wrap is AES-256-GCM under an HKDF-SHA256 output" a single fact to check.
 *
 * Nothing here ever holds the unwrapped data key beyond the call that produces it: [seal] takes a
 * [DataKey] and exports its bytes itself, and [open] hands the bytes straight to
 * [DataKey.fromRawBytes]. No caller has to touch raw key bytes to wrap or unwrap.
 */
internal object KeyWrapping {

    /** 12 bytes, the size AES-GCM is defined against, matching the record envelope's nonce. */
    const val NONCE_SIZE: Int = 12

    /** 128 bits of HKDF salt, enough that two wraps of one phrase never derive the same key. */
    const val SALT_SIZE: Int = 16

    /** AES-256, matching the data key it wraps and the security level of the phrase above it. */
    private val WRAPPING_KEY_SIZE = 32.bytes

    private val hkdf get() = CryptographyProvider.Default.get(HKDF)
    private val aes get() = CryptographyProvider.Default.get(AES.GCM)

    /**
     * Derives the wrapping key from [inputKeyingMaterial].
     *
     * [info] is the domain separator, and every wrap type passes a different one. That is what keeps
     * the wraps independent: two derivations that ever saw the same input keying material would
     * still produce unrelated keys, and a wrap sealed in one context cannot be opened by a key
     * derived for another even when the caller holds both inputs.
     *
     * [salt] is null for inputs that are already full-entropy secrets, where HKDF's extract step has
     * no entropy left to concentrate; RFC 5869 substitutes a string of zeros in that case.
     */
    suspend fun wrappingKey(
        inputKeyingMaterial: ByteArray,
        salt: ByteArray?,
        info: String
    ): AES.GCM.Key {
        val derived = hkdf
            .secretDerivation(
                digest = SHA256,
                outputSize = WRAPPING_KEY_SIZE,
                salt = salt,
                info = info.encodeToByteArray()
            )
            .deriveSecretToByteArray(inputKeyingMaterial)
        return aes.keyDecoder().decodeFromByteArray(AES.Key.Format.RAW, derived)
    }

    /**
     * Seals [dataKey] under [wrappingKey] with [nonce], returning ciphertext with its tag appended.
     *
     * The nonce is supplied rather than derived here for the same reason [RecordCipher] draws one
     * per seal: it has to be a fresh draw from the platform CSPRNG per wrap operation. Deriving it
     * from the key or from a counter would repeat it across the re-wraps that PIN changes and device
     * pairing perform, and nonce reuse under GCM is catastrophic rather than degrading.
     */
    // The library reserves explicit-IV encryption for callers that guarantee nonce uniqueness
    // themselves. Every caller here passes a fresh CryptographyRandom draw, and the wrap stores it
    // as its own parameter rather than relying on the library's default of prepending it.
    @OptIn(DelicateCryptographyApi::class)
    suspend fun seal(wrappingKey: AES.GCM.Key, dataKey: DataKey, nonce: ByteArray): ByteArray {
        require(nonce.size == NONCE_SIZE) { "Wrap nonce must be $NONCE_SIZE bytes" }
        return wrappingKey.cipher().encryptWithIv(iv = nonce, plaintext = dataKey.exportRawBytes())
    }

    /**
     * Opens [wrap] under [wrappingKey].
     *
     * @throws KeyWrapException when the wrapping key is not the one the copy was sealed under, or
     * when the nonce or the sealed bytes were altered. Authentication is what makes both cases the
     * same case, and nothing is returned in either, so a tampered wrap cannot yield a key.
     */
    @OptIn(DelicateCryptographyApi::class)
    suspend fun open(wrappingKey: AES.GCM.Key, wrap: WrappedDataKey, nonce: ByteArray): DataKey {
        val raw = try {
            wrappingKey.cipher().decryptWithIv(iv = nonce, ciphertext = wrappedKeyBytes(wrap))
        } catch (cause: Throwable) {
            throw KeyWrapException("The ${wrap.wrapType} wrap failed authentication", cause)
        }
        return try {
            DataKey.fromRawBytes(wrap.keyId, raw)
        } catch (cause: IllegalArgumentException) {
            throw KeyWrapException("The ${wrap.wrapType} wrap did not open onto a data key", cause)
        }
    }

    /** Rejects a wrap of the wrong type before any key material is derived for it. */
    fun requireType(wrap: WrappedDataKey, expected: KeyWrapType) {
        if (wrap.type != expected) {
            throw KeyWrapException("Expected a ${expected.id} wrap, not ${wrap.wrapType}")
        }
    }

    /**
     * Returns the unwrap parameter stored under [name].
     *
     * @throws KeyWrapException when the parameter is absent or is not well-formed, which is what a
     * truncated or edited key material document reaching an unwrap looks like.
     */
    fun requiredParameter(wrap: WrappedDataKey, name: String): ByteArray = translating(wrap) {
        wrap.parameter(name)
    } ?: throw KeyWrapException("The ${wrap.wrapType} wrap carries no $name")

    private fun wrappedKeyBytes(wrap: WrappedDataKey): ByteArray =
        translating(wrap) { wrap.wrappedKeyBytes() }

    private fun <T> translating(wrap: WrappedDataKey, read: () -> T): T = try {
        read()
    } catch (cause: EncryptedRecordException) {
        throw KeyWrapException("The ${wrap.wrapType} wrap is not well-formed", cause)
    }
}
