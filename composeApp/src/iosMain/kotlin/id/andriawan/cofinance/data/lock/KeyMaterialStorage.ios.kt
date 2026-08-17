package id.andriawan.cofinance.data.lock

import id.andriawan.cofinance.data.crypto.cfOwning
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.value
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
import platform.Security.kSecAttrAccessibleWhenUnlockedThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

/** Account of the key material document's generic password item. */
internal const val KEY_MATERIAL_ACCOUNT: String = "key-material"

/**
 * The iOS key material storage: a Keychain generic password holding the serialized document.
 *
 * The Keychain rather than a file in the app container, because it is the storage iOS offers for
 * exactly this and it comes with the two properties this document needs. `ThisDeviceOnly` excludes
 * it from iCloud Keychain and from encrypted backups, so the device wrap cannot be carried to
 * another device and quietly work there — the recovery phrase is meant to be the only route onto a
 * new device, and a synchronizing item would silently make it not so. `WhenUnlocked` means the
 * bytes are unreadable while the device is locked, which costs nothing here: key material is read
 * on the launch path and at unlock, both of which are foreground moments.
 *
 * `WhenUnlockedThisDeviceOnly` is a deliberate step *up* from the failed-attempt counter's
 * `AfterFirstUnlockThisDeviceOnly`. That counter has to be writable from a background resume; this
 * document does not, so it takes the stricter class.
 *
 * As with every Keychain item, this outlives an app uninstall. That matches the device key vault,
 * whose key material also survives, so a reinstall finds a device wrap and the platform key it
 * opens against still paired rather than a wrap with nothing behind it.
 */
@OptIn(ExperimentalForeignApi::class)
internal class KeychainKeyMaterialStorage(
    private val service: String = LOCK_KEYCHAIN_SERVICE,
    private val account: String = KEY_MATERIAL_ACCOUNT
) : KeyMaterialStorage {

    override suspend fun read(): ByteArray? = cfOwning { owner ->
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
                    val data = found.value ?: return@memScoped null
                    try {
                        data.readBytes().takeIf { it.isNotEmpty() }
                    } finally {
                        CFRelease(data)
                    }
                }

                // Anything other than success is treated as "no readable document", which routes
                // the launch to setup or restore. Throwing here would instead produce an app that
                // cannot start on a device whose Keychain is momentarily unavailable.
                else -> null
            }
        }
    }

    override suspend fun write(bytes: ByteArray) {
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
                    kSecAttrAccessible to kSecAttrAccessibleWhenUnlockedThisDeviceOnly,
                    kSecValueData to owner.dataOf(bytes, "the local key material")
                )
            )
            when (val status = SecItemAdd(attributes, null)) {
                errSecSuccess -> Unit
                errSecDuplicateItem -> {
                    val update = owner.dictionaryOf(
                        listOf(
                            kSecValueData to owner.dataOf(bytes, "the local key material")
                        )
                    )
                    val updated = SecItemUpdate(query, update)
                    check(updated == errSecSuccess) {
                        "Storing the local key material failed (OSStatus $updated)"
                    }
                }

                else -> error("Storing the local key material failed (OSStatus $status)")
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
            val status = SecItemDelete(query)
            check(status == errSecSuccess || status == errSecItemNotFound) {
                "Erasing the local key material failed (OSStatus $status)"
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
actual fun createKeyMaterialStorage(): KeyMaterialStorage = KeychainKeyMaterialStorage()
