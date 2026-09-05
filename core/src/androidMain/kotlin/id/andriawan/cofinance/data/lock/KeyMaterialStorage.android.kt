package id.andriawan.cofinance.data.lock

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * The Android key material storage: one file inside the application's private directory.
 *
 * ## Why app-private storage rather than the Keystore or `EncryptedSharedPreferences`
 *
 * What is stored here is a list of *already sealed* wraps. The device wrap only opens through an
 * ECDH agreement against key material that never leaves the Keystore, and the PIN wrap only opens
 * through `HKDF(scrypt(pin) || device secret)` where the device secret is an HMAC under a
 * non-extractable Keystore key. A copy of this file lifted off the device — a backup, a rooted read
 * — is inert, so a second encryption layer would protect bytes that are already protected.
 *
 * It would also cost something real. Sealing this file under its own Keystore key means a device
 * that loses that key — an OS upgrade that invalidates entries, a Keystore corruption — loses the
 * wraps *behind* it, sending a user who still has a perfectly good PIN to a twelve-word restore.
 * The failed-attempt counter is sealed precisely because it has no protection of its own; this file
 * has, and the asymmetry is the reason the two are stored differently.
 *
 * The directory is the lock's own, so `android:allowBackup` aside, everything the lock writes lives
 * in one place and [clear] removes exactly it. Note that `allowBackup` is `true` in the manifest:
 * that is harmless for this file for the same reason the above holds — a restored copy on a
 * different device has no Keystore material behind it and opens nothing.
 *
 * ## Durability
 *
 * The write goes to a temporary file and is renamed over the target, so a process death mid-write
 * leaves the previous document rather than a truncated one. Losing a *new* document that way is
 * survivable — setup or restore runs again — where losing the previous one is not.
 */
class FileKeyMaterialStorage internal constructor(
    private val directory: () -> File
) : KeyMaterialStorage {

    private val mutex = Mutex()

    val documentFile: File get() = File(directory(), DOCUMENT_FILE)

    override suspend fun read(): ByteArray? = guarded {
        val file = documentFile
        if (file.isFile) file.readBytes().takeIf { it.isNotEmpty() } else null
    }

    override suspend fun write(bytes: ByteArray): Unit = guarded {
        val target = documentFile
        val temporary = File(target.parentFile, "${target.name}.tmp")
        temporary.writeBytes(bytes)
        if (!temporary.renameTo(target)) {
            temporary.delete()
            throw IOException("The local key material could not be stored")
        }
    }

    override suspend fun clear(): Unit = guarded { documentFile.delete() }

    private suspend fun <T> guarded(block: () -> T): T = withContext(Dispatchers.IO) {
        mutex.withLock { block() }
    }

    companion object {
        const val DOCUMENT_FILE: String = "key-material.json"
    }
}

actual fun createKeyMaterialStorage(): KeyMaterialStorage =
    FileKeyMaterialStorage { LockStorage.directory() }
