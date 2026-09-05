package id.andriawan.cofinance.data.crypto

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.DelicateCryptographyApi
import dev.whyoleg.cryptography.algorithms.AES
import dev.whyoleg.cryptography.random.CryptographyRandom
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Where this device keeps the phrase, so that security settings can show it again.
 *
 * Decision 10 makes the recovery phrase re-displayable rather than show-once, and nothing else in
 * the system can supply it: the recovery-phrase wrap is a copy of the data key sealed *under* the
 * phrase, so it proves a phrase correct but never reveals one. Re-display therefore needs the phrase
 * itself to be kept, and this is the only place that keeps it.
 *
 * What is stored is the phrase's 128 bits of entropy sealed under the data key, so the stored blob
 * is worth exactly as much as the data key is: someone who can open it already holds the key that
 * opens every record, and someone who cannot holds 16 bytes of AES-GCM ciphertext. That is why it
 * can live in ordinary app-private storage rather than needing the sealed storage the failed-attempt
 * counter uses. The PIN is still what gates re-display, because obtaining the data key at the moment
 * of the request is what [id.andriawan.cofinance.data.lock.AppLock.verifyPin] is for.
 *
 * Nothing here erases itself when local key material is destroyed. It does not have to: without the
 * data key the blob opens for nobody, and a device that restores from the phrase writes the phrase
 * back here as part of the restore.
 */
interface RecoveryPhraseVault {

    /** Keeps [phrase] on this device, readable only by a holder of [dataKey]. */
    suspend fun store(phrase: RecoveryPhrase, dataKey: DataKey)

    /**
     * Returns the stored phrase, or null when this device holds none or [dataKey] does not open it.
     *
     * A wrong key and an absent blob are the same answer on purpose: the caller's response to both
     * is to show nothing, and there is no state in which distinguishing them helps the user.
     */
    suspend fun read(dataKey: DataKey): RecoveryPhrase?

    /** Forgets the stored phrase. Re-display stops working; recovery itself is unaffected. */
    suspend fun erase()
}

/**
 * A [RecoveryPhraseVault] over any string key-value storage, which is all either platform needs.
 *
 * The sealing is written once here rather than per platform, so `commonTest` drives the real
 * derivation and the real AES-GCM seal over a map instead of testing a fake of them. The platform
 * `actual` supplies three lambdas and nothing else.
 *
 * Every store draws a fresh salt and a fresh nonce. Re-storing the same phrase under the same data
 * key — which a PIN change does not do today but a re-wrap later might — must not produce the same
 * bytes, and a repeated GCM nonce under one key is catastrophic rather than degrading.
 */
class SealedRecoveryPhraseVault(
    private val readSealed: () -> String?,
    private val writeSealed: (String) -> Unit,
    private val clearSealed: () -> Unit
) : RecoveryPhraseVault {

    @OptIn(ExperimentalEncodingApi::class, DelicateCryptographyApi::class)
    override suspend fun store(phrase: RecoveryPhrase, dataKey: DataKey) {
        val salt = CryptographyRandom.nextBytes(SALT_SIZE)
        val nonce = CryptographyRandom.nextBytes(NONCE_SIZE)
        val entropy = phrase.toEntropy()
        val sealed = try {
            wrappingKey(dataKey, salt).cipher().encryptWithIv(iv = nonce, plaintext = entropy)
        } finally {
            // The entropy is the phrase. This function drew the copy, so it discards it.
            entropy.fill(0)
        }
        writeSealed(Base64.encode(salt + nonce + sealed))
    }

    @OptIn(ExperimentalEncodingApi::class, DelicateCryptographyApi::class)
    override suspend fun read(dataKey: DataKey): RecoveryPhrase? {
        val stored = readSealed() ?: return null
        val bytes = try {
            Base64.decode(stored)
        } catch (_: IllegalArgumentException) {
            return null
        }
        if (bytes.size <= SALT_SIZE + NONCE_SIZE) return null

        val salt = bytes.copyOfRange(0, SALT_SIZE)
        val nonce = bytes.copyOfRange(SALT_SIZE, SALT_SIZE + NONCE_SIZE)
        val sealed = bytes.copyOfRange(SALT_SIZE + NONCE_SIZE, bytes.size)

        val entropy = try {
            wrappingKey(dataKey, salt).cipher().decryptWithIv(iv = nonce, ciphertext = sealed)
        } catch (_: Throwable) {
            // A wrong key, an edited blob, and a truncated one all fail authentication here, and all
            // mean the same thing to the caller: this device cannot show the phrase.
            return null
        }
        return try {
            RecoveryPhrase.fromEntropy(entropy)
        } catch (_: IllegalArgumentException) {
            null
        } finally {
            entropy.fill(0)
        }
    }

    override suspend fun erase() = clearSealed()

    private suspend fun wrappingKey(dataKey: DataKey, salt: ByteArray): AES.GCM.Key {
        val raw = dataKey.exportRawBytes()
        return try {
            KeyWrapping.wrappingKey(inputKeyingMaterial = raw, salt = salt, info = INFO)
        } finally {
            raw.fill(0)
        }
    }

    private companion object {
        /** Matches the wrap layer's parameters, for one set of sizes to reason about. */
        const val SALT_SIZE = KeyWrapping.SALT_SIZE
        const val NONCE_SIZE = KeyWrapping.NONCE_SIZE

        /**
         * The domain separator, distinct from every wrap type's.
         *
         * The data key is also the input keying material for nothing else, but keeping the label
         * distinct means a key derived to seal the phrase can never open a wrap, or the reverse,
         * even if the inputs were ever made to coincide.
         */
        const val INFO = "cofinance/e2ee/recovery-phrase-vault/v1"
    }
}

/** A [RecoveryPhraseVault] that lives for the life of the process, for tests and previews. */
class InMemoryRecoveryPhraseVault : RecoveryPhraseVault {

    private var stored: String? = null

    private val delegate = SealedRecoveryPhraseVault(
        readSealed = { stored },
        writeSealed = { stored = it },
        clearSealed = { stored = null }
    )

    override suspend fun store(phrase: RecoveryPhrase, dataKey: DataKey) =
        delegate.store(phrase, dataKey)

    override suspend fun read(dataKey: DataKey): RecoveryPhrase? = delegate.read(dataKey)

    override suspend fun erase() = delegate.erase()
}

/** Returns the phrase vault backed by this platform's key-value storage. */
expect fun createRecoveryPhraseVault(): RecoveryPhraseVault
