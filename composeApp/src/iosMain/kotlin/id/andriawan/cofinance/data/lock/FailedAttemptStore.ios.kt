package id.andriawan.cofinance.data.lock

import id.andriawan.cofinance.data.crypto.cfOwning
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDataGetBytePtr
import platform.CoreFoundation.CFDataGetLength
import platform.CoreFoundation.CFDataRef
import platform.CoreFoundation.CFDataRefVar
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.kCFBooleanTrue
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.SecItemUpdate
import platform.Security.errSecDuplicateItem
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

/** Service of the lock's Keychain items. */
internal const val LOCK_KEYCHAIN_SERVICE: String = "id.andriawan.cofinance.lock"

/** Account of the failed-attempt counter's generic password item. */
internal const val FAILED_ATTEMPTS_ACCOUNT: String = "failed-attempts"

/**
 * The failed-attempt counter as a Keychain item.
 *
 * iOS gives the reinstall requirement directly, where Android can only approximate it: a Keychain
 * item belongs to the keychain rather than to the application's container, so deleting the app does
 * not delete it and a reinstall finds the same counter. That is the same property the device key
 * vault relies on for the device secret, and it is why Decision 3's "the counter's persistence is
 * tied to the device key's lifetime" is literally true on this platform.
 *
 * The item is `AfterFirstUnlockThisDeviceOnly`: it never leaves the device, is excluded from
 * backups and from migration to a new device, and is readable after the first unlock following a
 * reboot, which is what lets a write happen even if the app is resumed into the background.
 *
 * No additional encryption is layered on top. The Keychain *is* the secure storage the requirement
 * asks for, and another app cannot read or edit an item under this app's keychain access group.
 */
@OptIn(ExperimentalForeignApi::class)
internal class KeychainFailedAttemptStore(
    private val service: String = LOCK_KEYCHAIN_SERVICE,
    private val account: String = FAILED_ATTEMPTS_ACCOUNT
) : FailedAttemptStore {

    override suspend fun read(): StoredFailedAttempts = cfOwning { owner ->
        memScoped {
            val query = owner.dictionaryOf(
                listOf(
                    kSecClass to kSecClassGenericPassword,
                    kSecAttrService to owner.stringOf(service),
                    kSecAttrAccount to owner.stringOf(account),
                    kSecReturnData to kCFBooleanTrue
                )
            )
            val found = alloc<CFDataRefVar>()
            when (SecItemCopyMatching(query, found.ptr.reinterpret())) {
                errSecSuccess -> {
                    val data = found.value ?: return@memScoped StoredFailedAttempts.Unreadable
                    val bytes = try {
                        data.readBytes()
                    } finally {
                        CFRelease(data)
                    }
                    decode(bytes)?.let(StoredFailedAttempts::Recorded)
                        ?: StoredFailedAttempts.Unreadable
                }

                errSecItemNotFound -> StoredFailedAttempts.None
                // Any other status means the item exists in some form the app cannot read, which is
                // the tamper-evidence case rather than an absence.
                else -> StoredFailedAttempts.Unreadable
            }
        }
    }

    override suspend fun write(record: FailedAttemptRecord) {
        val encoded = encode(record)
        cfOwning { owner ->
            val query = owner.dictionaryOf(
                listOf(
                    kSecClass to kSecClassGenericPassword,
                    kSecAttrService to owner.stringOf(service),
                    kSecAttrAccount to owner.stringOf(account)
                )
            )
            val attributes = owner.dictionaryOf(
                listOf(
                    kSecClass to kSecClassGenericPassword,
                    kSecAttrService to owner.stringOf(service),
                    kSecAttrAccount to owner.stringOf(account),
                    kSecAttrAccessible to kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly,
                    kSecValueData to owner.dataOf(encoded, "the failed-attempt record")
                )
            )
            when (SecItemAdd(attributes, null)) {
                errSecSuccess -> Unit
                errSecDuplicateItem -> {
                    val update = owner.dictionaryOf(
                        listOf(
                            kSecValueData to owner.dataOf(encoded, "the failed-attempt record")
                        )
                    )
                    SecItemUpdate(query, update)
                }

                else -> Unit
            }
        }
    }

    override suspend fun clear() {
        cfOwning { owner ->
            val query = owner.dictionaryOf(
                listOf(
                    kSecClass to kSecClassGenericPassword,
                    kSecAttrService to owner.stringOf(service),
                    kSecAttrAccount to owner.stringOf(account)
                )
            )
            SecItemDelete(query)
        }
    }

    /** Fixed 12 bytes: a four-byte count followed by an eight-byte timestamp, big-endian. */
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
        return if (failures < 0) null else FailedAttemptRecord(failures, millis)
    }

    private companion object {
        const val RECORD_SIZE = 12
    }
}

@OptIn(ExperimentalForeignApi::class)
internal fun CFDataRef.readBytes(): ByteArray {
    val length = CFDataGetLength(this).toInt()
    if (length == 0) return ByteArray(0)
    val pointer: CPointer<*> = CFDataGetBytePtr(this) ?: return ByteArray(0)
    val bytes = pointer.reinterpret<kotlinx.cinterop.ByteVar>()
    return ByteArray(length) { index -> bytes[index] }
}

@OptIn(ExperimentalForeignApi::class)
actual fun createFailedAttemptStore(): FailedAttemptStore = KeychainFailedAttemptStore()
