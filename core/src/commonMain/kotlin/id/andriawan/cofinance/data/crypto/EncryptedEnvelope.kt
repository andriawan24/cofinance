package id.andriawan.cofinance.data.crypto

/**
 * A single encrypted finance record as it exists in memory.
 *
 * The envelope carries everything needed to decrypt the record apart from the key itself: the format
 * [version], the [keyId] of the data key that sealed it, the per-record [nonce], and the [ciphertext]
 * with its authentication tag appended. It deliberately carries no finance value in readable form.
 */
class EncryptedEnvelope(
    val version: Int,
    val keyId: String,
    val nonce: ByteArray,
    val ciphertext: ByteArray
) {

    init {
        require(version > 0) { "Envelope version must be positive" }
        require(keyId.isNotBlank()) { "Envelope must identify the key that sealed it" }
        require(nonce.size == NONCE_SIZE) { "AES-GCM nonce must be $NONCE_SIZE bytes" }
        require(ciphertext.isNotEmpty()) { "Envelope must carry ciphertext" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EncryptedEnvelope) return false
        return version == other.version &&
            keyId == other.keyId &&
            nonce.contentEquals(other.nonce) &&
            ciphertext.contentEquals(other.ciphertext)
    }

    override fun hashCode(): Int {
        var result = version
        result = 31 * result + keyId.hashCode()
        result = 31 * result + nonce.contentHashCode()
        result = 31 * result + ciphertext.contentHashCode()
        return result
    }

    /** Describes the envelope without reproducing any of its bytes. */
    override fun toString(): String =
        "EncryptedEnvelope(version=$version, keyId=$keyId, ciphertextBytes=${ciphertext.size})"

    companion object {
        /** The only envelope format this build writes. Reads reject anything else. */
        const val CURRENT_VERSION: Int = 1

        /** 12 bytes is the nonce size AES-GCM is defined against and the only one accepted here. */
        const val NONCE_SIZE: Int = 12
    }
}

/** Raised when a stored record cannot be turned back into a finance record. */
class EncryptedRecordException(message: String, cause: Throwable? = null) : Exception(message, cause)
