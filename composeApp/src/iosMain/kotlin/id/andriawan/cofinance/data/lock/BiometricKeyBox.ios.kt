package id.andriawan.cofinance.data.lock

import id.andriawan.cofinance.data.crypto.cfOwning
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDataRefVar
import platform.CoreFoundation.CFErrorRefVar
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.kCFAllocatorDefault
import platform.CoreFoundation.kCFBooleanTrue
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSError
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAErrorBiometryLockout
import platform.LocalAuthentication.LAErrorBiometryNotAvailable
import platform.LocalAuthentication.LAErrorBiometryNotEnrolled
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthenticationWithBiometrics
import platform.Security.SecAccessControlCreateFlags
import platform.Security.SecAccessControlCreateWithFlags
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecAuthFailed
import platform.Security.errSecInteractionNotAllowed
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.errSecUserCanceled
import platform.Security.kSecAccessControlBiometryCurrentSet
import platform.Security.kSecAttrAccessControl
import platform.Security.kSecAttrAccessibleWhenUnlockedThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecReturnData
import platform.Security.kSecUseAuthenticationContext
import platform.Security.kSecValueData

/** Account of the biometrically sealed copy of the data key. */
internal const val BIOMETRIC_DATA_KEY_ACCOUNT: String = "biometric-data-key"

/**
 * The iOS [BiometricKeyBox]: a Keychain item behind `kSecAccessControlBiometryCurrentSet`.
 *
 * That flag is the enrollment-invalidating policy Decision 4 asks for, expressed the way iOS
 * expresses it: the item is bound to the *current* set of enrolled fingerprints or faces, and any
 * change to that set — adding a finger, re-enrolling a face, or removing biometrics entirely —
 * permanently prevents it from being read. There is no way to re-authorize it, which is the point;
 * the app deletes what is left and the user re-enables biometric unlock after entering their PIN.
 *
 * `WhenUnlockedThisDeviceOnly` on top of it keeps the sealed copy off backups and off any other
 * device, matching how the device secret is stored.
 *
 * No prompt is shown by this class directly. Reading an item under a biometric access control makes
 * the Keychain itself present the system prompt, so there is no view controller to supply and
 * nothing for the caller to host — which is why the iOS side needs none of the activity plumbing
 * the Android side documents.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal class KeychainBiometricKeyBox(
    private val service: String = LOCK_KEYCHAIN_SERVICE,
    private val account: String = BIOMETRIC_DATA_KEY_ACCOUNT
) : BiometricKeyBox {

    override suspend fun capability(): BiometricCapability = memScoped {
        val context = LAContext()
        val error = alloc<ObjCObjectVar<NSError?>>()
        val usable = context.canEvaluatePolicy(
            LAPolicyDeviceOwnerAuthenticationWithBiometrics,
            error.ptr
        )
        if (usable) return@memScoped BiometricCapability.Available
        when (error.value?.code) {
            LAErrorBiometryNotEnrolled -> BiometricCapability.NotEnrolled
            LAErrorBiometryNotAvailable -> BiometricCapability.NoHardware
            LAErrorBiometryLockout -> BiometricCapability.Unavailable
            else -> BiometricCapability.Unavailable
        }
    }

    /**
     * Whether a sealed copy exists, asked without prompting.
     *
     * The query returns attributes rather than data, so the Keychain has no reason to authenticate
     * the user: a settings screen showing the state of a toggle must not summon Face ID.
     */
    override suspend fun hasSealedSecret(): Boolean = cfOwning { owner ->
        memScoped {
            val query = owner.dictionaryOf(
                listOf(
                    kSecClass to kSecClassGenericPassword,
                    kSecAttrService to owner.stringOf(service),
                    kSecAttrAccount to owner.stringOf(account)
                )
            )
            SecItemCopyMatching(query, null) == errSecSuccess
        }
    }

    override suspend fun seal(
        plaintext: ByteArray,
        prompt: BiometricPromptText
    ): BiometricSealResult {
        clear()
        return cfOwning { owner ->
            memScoped {
                val error = alloc<CFErrorRefVar>()
                val access = SecAccessControlCreateWithFlags(
                    kCFAllocatorDefault,
                    kSecAttrAccessibleWhenUnlockedThisDeviceOnly,
                    BIOMETRY_CURRENT_SET,
                    error.ptr
                ) ?: return@memScoped BiometricSealResult.Failed(
                    "the biometric access control could not be created"
                )
                owner.own(access, "an access control")

                val context = LAContext().apply { localizedReason = prompt.title }
                val attributes = owner.dictionaryOf(
                    listOf(
                        kSecClass to kSecClassGenericPassword,
                        kSecAttrService to owner.stringOf(service),
                        kSecAttrAccount to owner.stringOf(account),
                        kSecAttrAccessControl to access,
                        kSecUseAuthenticationContext to owner.own(CFBridgingRetain(context), "an authentication context"),
                        kSecValueData to owner.dataOf(plaintext, "the sealed data key")
                    )
                )
                when (val status = SecItemAdd(attributes, null)) {
                    errSecSuccess -> BiometricSealResult.Sealed
                    errSecUserCanceled -> BiometricSealResult.Cancelled
                    errSecInteractionNotAllowed -> BiometricSealResult.Unavailable
                    else -> BiometricSealResult.Failed("OSStatus $status")
                }
            }
        }
    }

    override suspend fun open(prompt: BiometricPromptText): BiometricOpenResult = cfOwning { owner ->
        memScoped {
            val context = LAContext().apply {
                localizedReason = prompt.title
                localizedFallbackTitle = prompt.negativeButtonLabel
            }
            val query = owner.dictionaryOf(
                listOf(
                    kSecClass to kSecClassGenericPassword,
                    kSecAttrService to owner.stringOf(service),
                    kSecAttrAccount to owner.stringOf(account),
                    kSecUseAuthenticationContext to owner.own(CFBridgingRetain(context), "an authentication context"),
                    kSecReturnData to kCFBooleanTrue
                )
            )
            val found = alloc<CFDataRefVar>()
            when (val status = SecItemCopyMatching(query, found.ptr.reinterpret())) {
                errSecSuccess -> {
                    val data = found.value
                        ?: return@memScoped BiometricOpenResult.Failed("no data was returned")
                    try {
                        BiometricOpenResult.Opened(data.readBytes())
                    } finally {
                        CFRelease(data)
                    }
                }

                errSecItemNotFound -> BiometricOpenResult.Absent
                errSecUserCanceled -> BiometricOpenResult.Cancelled
                // The Keychain reports an item bound to a superseded biometric set as an
                // authentication failure, because from its side that is what happened: the access
                // control can no longer be satisfied by anything. Nothing will ever open it again,
                // so it is discarded and the caller falls back to the PIN.
                errSecAuthFailed -> {
                    clear()
                    BiometricOpenResult.Invalidated
                }

                errSecInteractionNotAllowed -> BiometricOpenResult.Unavailable
                else -> BiometricOpenResult.Failed("OSStatus $status")
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

    private companion object {
        val BIOMETRY_CURRENT_SET: SecAccessControlCreateFlags =
            kSecAccessControlBiometryCurrentSet
    }
}

@OptIn(ExperimentalForeignApi::class)
actual fun createBiometricKeyBox(): BiometricKeyBox = KeychainBiometricKeyBox()
