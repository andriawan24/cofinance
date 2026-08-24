package id.andriawan.cofinance.data.crypto

import dev.whyoleg.cryptography.random.CryptographyRandom

/**
 * Wraps and unwraps the data key against the user's 12-word recovery phrase.
 *
 * This is the only wrapped copy that leaves the device, and the only route back to synchronized
 * records on a device that holds no key material. It derives from the phrase's entropy rather than
 * from its words: the words are a transcription format, and two phrases that differ only in spacing
 * or capitalization are the same phrase, which is already settled by [RecoveryPhrase.parse].
 *
 * The derivation is HKDF-SHA256 with a random per-wrap salt, not a memory-hard function. That is
 * deliberate. The phrase carries 128 bits of real entropy, so an attacker holding the uploaded wrap
 * has no search to accelerate and a memory-hard derivation would only slow the legitimate restore.
 * The low-entropy input in this system is the six-digit PIN, and it is a different wrap type with a
 * different derivation — scrypt composed with a device-held secret, per Decision 3.
 */
class RecoveryPhraseKeyWrapper(private val random: CryptographyRandom = CryptographyRandom) {

    /** Produces the wrapped copy of [dataKey] that [phrase] opens. */
    suspend fun wrap(dataKey: DataKey, phrase: RecoveryPhrase): WrappedDataKey {
        val salt = random.nextBytes(KeyWrapping.SALT_SIZE)
        val wrappingKey = KeyWrapping.wrappingKey(phrase.toEntropy(), salt = salt, info = INFO)
        val nonce = random.nextBytes(KeyWrapping.NONCE_SIZE)
        return WrappedDataKey.of(
            type = KeyWrapType.RecoveryPhrase,
            keyId = dataKey.id,
            wrappedKey = KeyWrapping.seal(wrappingKey, dataKey, nonce),
            parameters = mapOf(
                WrappedDataKey.NONCE_PARAMETER to nonce,
                WrappedDataKey.SALT_PARAMETER to salt
            )
        )
    }

    /**
     * Opens [wrap] with [phrase], returning the data key in memory.
     *
     * @throws KeyWrapException when the copy is not a phrase wrap, is missing or carries malformed
     * parameters, or when [phrase] is not the phrase it was wrapped under. A wrong phrase is a wrong
     * wrapping key and so fails authentication, which is what lets the restore screen tell the user
     * their phrase does not open this account without importing anything first.
     */
    suspend fun unwrap(wrap: WrappedDataKey, phrase: RecoveryPhrase): DataKey {
        KeyWrapping.requireType(wrap, KeyWrapType.RecoveryPhrase)
        val salt = KeyWrapping.requiredParameter(wrap, WrappedDataKey.SALT_PARAMETER)
        val nonce = KeyWrapping.requiredParameter(wrap, WrappedDataKey.NONCE_PARAMETER)

        val wrappingKey = KeyWrapping.wrappingKey(phrase.toEntropy(), salt = salt, info = INFO)
        return KeyWrapping.open(wrappingKey, wrap, nonce)
    }

    private companion object {
        const val INFO = "cofinance/e2ee/data-key-wrap/recovery-phrase/v1"
    }
}
