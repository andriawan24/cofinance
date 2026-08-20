package id.andriawan.cofinance.data.lock

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.ProviderException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * The failed-attempt counter, sealed under a non-extractable Keystore key.
 *
 * ## What "cannot be reset by reinstalling" means on Android, precisely
 *
 * Android has no storage an uninstall leaves behind — there is no equivalent of the iOS Keychain's
 * survival — so the requirement cannot be met by finding a more durable file. It is met by
 * coupling instead, and the coupling is real rather than rhetorical:
 *
 * - Clearing the app's data or uninstalling it deletes this counter *and* deletes every Keystore
 *   entry the application owns, including the device key material in `AndroidDeviceKeyVault`. The
 *   PIN wrap composes the PIN with a secret derived from one of those entries, so after a reinstall
 *   the PIN-wrapped copy of the data key — if a filesystem copy of it was even kept — no longer
 *   opens under any PIN at all. There is nothing left for the fresh set of attempts to be spent on.
 * - The attacker's remaining route is the recovery-phrase wrap in the backend, which a six-digit
 *   PIN never protected in the first place; it takes 12 words.
 * - Deleting only this file, leaving the Keystore intact, does not buy attempts either:
 *   [FailedAttemptGuard] treats an absent record on a device that holds a PIN wrap as tampering and
 *   destroys local key material.
 *
 * So the guarantee that holds on Android is "a reinstall does not grant further attempts against
 * the same wrapped key", not "the integer survives a reinstall". Only the first is a security
 * property; the second is a storage detail, and stating it as a promise on this platform would be
 * false.
 *
 * ## What sealing buys
 *
 * The record is encrypted with AES-256-GCM under a Keystore key that cannot leave the device, so an
 * attacker holding app-private storage — a backup, a rooted read — can neither read the count nor
 * write a smaller one. Authentication means an edited byte surfaces as
 * [StoredFailedAttempts.Unreadable] rather than as a plausible number.
 */
class KeystoreSealedFailedAttemptStore internal constructor(
    private val directory: () -> File
) : FailedAttemptStore {

    private val mutex = Mutex()

    override suspend fun read(): StoredFailedAttempts = guarded {
        val file = recordFile()
        if (!file.isFile) return@guarded StoredFailedAttempts.None

        val key = existingSealingKey() ?: return@guarded StoredFailedAttempts.Unreadable
        val sealed = file.readBytes()
        val plaintext = try {
            unseal(key, sealed)
        } catch (_: GeneralSecurityException) {
            return@guarded StoredFailedAttempts.Unreadable
        } catch (_: IllegalArgumentException) {
            return@guarded StoredFailedAttempts.Unreadable
        }
        decode(plaintext)?.let(StoredFailedAttempts::Recorded) ?: StoredFailedAttempts.Unreadable
    }

    override suspend fun write(record: FailedAttemptRecord): Unit = guarded {
        val sealed = seal(loadOrCreateSealingKey(), encode(record))
        val target = recordFile()
        val temporary = File(target.parentFile, "${target.name}.tmp")
        temporary.writeBytes(sealed)
        if (!temporary.renameTo(target)) {
            temporary.delete()
            throw IOException("The failed-attempt counter could not be stored")
        }
    }

    /**
     * Removes the record but keeps the Keystore key.
     *
     * The key is not deleted because deleting it is what a *tampering* attacker would want:
     * an existing record that no longer opens is evidence, and evidence is only useful while the
     * key that produced it still exists. A subsequent [write] reuses the same key, so the sealed
     * records of one installation stay mutually consistent.
     */
    override suspend fun clear(): Unit = guarded {
        recordFile().delete()
        Unit
    }

    private fun recordFile(): File = File(directory(), RECORD_FILE)

    // -------------------------------------------------------------------------------------------
    // Record encoding: a fixed 12 bytes, so a length alone reveals nothing about the count.
    // -------------------------------------------------------------------------------------------

    private fun encode(record: FailedAttemptRecord): ByteArray {
        val bytes = ByteArray(RECORD_SIZE)
        var index = 0
        for (shift in intArrayOf(24, 16, 8, 0)) {
            bytes[index++] = (record.consecutiveFailures ushr shift).toByte()
        }
        for (shift in intArrayOf(56, 48, 40, 32, 24, 16, 8, 0)) {
            bytes[index++] = (record.lastFailureAtMillis ushr shift).toByte()
        }
        return bytes
    }

    private fun decode(bytes: ByteArray): FailedAttemptRecord? {
        if (bytes.size != RECORD_SIZE) return null
        var failures = 0
        for (index in 0 until 4) {
            failures = (failures shl 8) or (bytes[index].toInt() and 0xFF)
        }
        var millis = 0L
        for (index in 4 until RECORD_SIZE) {
            millis = (millis shl 8) or (bytes[index].toLong() and 0xFF)
        }
        if (failures < 0) return null
        return FailedAttemptRecord(failures, millis)
    }

    // -------------------------------------------------------------------------------------------
    // Sealing
    // -------------------------------------------------------------------------------------------

    private fun seal(key: SecretKey, plaintext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext)
        return ByteArray(1 + iv.size + ciphertext.size).also { out ->
            out[0] = iv.size.toByte()
            iv.copyInto(out, 1)
            ciphertext.copyInto(out, 1 + iv.size)
        }
    }

    private fun unseal(key: SecretKey, sealed: ByteArray): ByteArray {
        require(sealed.isNotEmpty()) { "empty sealed record" }
        val ivLength = sealed[0].toInt() and 0xFF
        require(sealed.size > 1 + ivLength + GCM_TAG_BITS / 8 - 1) { "truncated sealed record" }
        val iv = sealed.copyOfRange(1, 1 + ivLength)
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(sealed, 1 + ivLength, sealed.size - 1 - ivLength)
    }

    private fun existingSealingKey(): SecretKey? {
        val keyStore = keyStore()
        if (!keyStore.containsAlias(KEY_ALIAS)) return null
        return keyStore.getKey(KEY_ALIAS, null) as? SecretKey
    }

    private fun loadOrCreateSealingKey(): SecretKey = existingSealingKey() ?: generateSealingKey()

    private fun generateSealingKey(): SecretKey {
        fun generate(strongBox: Boolean): SecretKey {
            val generator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                KEYSTORE_PROVIDER
            )
            generator.init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setKeySize(AES_KEY_BITS)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .apply { if (strongBox) setIsStrongBoxBacked(true) }
                    .build()
            )
            return generator.generateKey()
        }

        // StrongBox is opportunistic here for the same reason it is in the device key vault: devices
        // report its absence inconsistently, so both documented failures fall back rather than
        // propagating.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                return generate(strongBox = true)
            } catch (_: StrongBoxUnavailableException) {
                // fall through
            } catch (_: ProviderException) {
                // fall through
            }
        }
        return generate(strongBox = false)
    }

    private fun keyStore(): KeyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }

    private suspend fun <T> guarded(block: () -> T): T = withContext(Dispatchers.IO) {
        mutex.withLock { block() }
    }

    companion object {
        /** Keystore alias of the key the counter is sealed under. Exposed for the device test. */
        const val KEY_ALIAS: String = "id.andriawan.cofinance.lock.attempts"

        /** File holding the sealed counter, inside the lock's app-private directory. */
        const val RECORD_FILE: String = "failed-attempts.bin"

        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val AES_KEY_BITS = 256
        private const val GCM_TAG_BITS = 128
        private const val RECORD_SIZE = 12
    }
}

actual fun createFailedAttemptStore(): FailedAttemptStore =
    KeystoreSealedFailedAttemptStore { LockStorage.directory() }
