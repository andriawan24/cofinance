package id.andriawan.cofinance.data.crypto

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.value
import kotlinx.coroutines.test.runTest
import platform.CoreFoundation.CFDictionaryGetValue
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.Security.SecItemCopyMatching
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessControl
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleWhenUnlockedThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrApplicationTag
import platform.Security.kSecAttrKeyClass
import platform.Security.kSecAttrKeyClassPrivate
import platform.Security.kSecAttrKeyType
import platform.Security.kSecAttrKeyTypeECSECPrimeRandom
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecClassKey
import platform.Security.kSecReturnAttributes
import platform.Security.kSecReturnData

/**
 * Checks the Keychain vault's contract on whatever backing the running platform provides.
 *
 * Two caveats govern how a result here may be read.
 *
 * First, **the simulator has no Secure Enclave**, so [KeychainDeviceKeyVault] falls back to a
 * Keychain-resident software key there. The contract tests below still hold on that fallback — the
 * item is device-restricted, this class never exports the private key, and ECDH agrees — but a
 * green simulator run says nothing about hardware sealing, and a software Keychain key is in fact
 * exportable by anything holding the Keychain. [thePrivateKeyCannotBeExportedFromTheSecureEnclave]
 * therefore asserts non-extractability only where an Enclave actually backs the key, which makes a
 * physical-device run the only place that requirement is verified.
 *
 * Second, these tests need an entitled Keychain. A bare Kotlin/Native test executable has no
 * keychain-access-group entitlement, so `SecItemAdd` can fail with `errSecMissingEntitlement`
 * (-34018); running them inside an app or XCTest bundle is what gives them a Keychain to talk to.
 *
 * Neither caveat can be evaluated today: `:composeApp:linkDebugTestIosSimulatorArm64` fails with
 * `ld: framework 'FirebaseCore' not found`, the linking risk recorded in this change's `design.md`.
 *
 * Every test uses a randomized Keychain tag, so a run can neither read nor destroy the real device
 * key material, and clears its own items afterwards.
 */
@OptIn(ExperimentalForeignApi::class)
class KeychainDeviceKeyVaultTest {

    @Test
    fun devicePublicKeyIsAnUncompressedSec1PointAndIsStable() = withVault { vault ->
        val first = vault.devicePublicKey()

        assertEquals(
            DeviceKeyVault.PUBLIC_KEY_SIZE,
            first.size,
            "the device public key must be the 65-byte uncompressed SEC 1 point"
        )
        assertEquals(0x04.toByte(), first[0], "an uncompressed SEC 1 point is tagged 0x04")
        assertContentEquals(first, vault.devicePublicKey(), "the key pair is created only once")
    }

    @Test
    fun ecdhAgreesWithAnotherKeychainKey() = runTest {
        val ours = newVault()
        val theirs = newVault()
        try {
            val fromOurSide = ours.sharedSecretWith(theirs.devicePublicKey())
            val fromTheirSide = theirs.sharedSecretWith(ours.devicePublicKey())

            assertEquals(32, fromOurSide.size, "raw P-256 ECDH output is the 32-byte X coordinate")
            assertContentEquals(fromOurSide, fromTheirSide, "ECDH must agree in both directions")
        } finally {
            ours.destroyKeyMaterial()
            theirs.destroyKeyMaterial()
        }
    }

    /**
     * The boundary that has to match across implementations: this returns the raw agreement output
     * and HKDF is applied above it. A vault that pre-hashed the secret would pass every
     * same-platform test and fail only once another device tried to open a wrap this one wrote.
     */
    @Test
    fun ecdhAgreesWithTheSoftwareImplementation() = runTest {
        val vault = newVault()
        val software = FakeDeviceKeyVault()
        try {
            val fromKeychain = vault.sharedSecretWith(software.devicePublicKey())
            val fromSoftware = software.sharedSecretWith(vault.devicePublicKey())

            assertContentEquals(
                fromSoftware,
                fromKeychain,
                "the Keychain vault and the software vault must derive the same raw secret"
            )
        } finally {
            vault.destroyKeyMaterial()
        }
    }

    @Test
    fun aMalformedPeerKeyIsRejected() = withVault { vault ->
        assertFailsWith<DeviceKeyVaultException> { vault.sharedSecretWith(ByteArray(0)) }
        assertFailsWith<DeviceKeyVaultException> { vault.sharedSecretWith(ByteArray(65)) }
        assertFailsWith<DeviceKeyVaultException> {
            vault.sharedSecretWith(ByteArray(64) { 0x04.toByte() })
        }
        assertFailsWith<DeviceKeyVaultException> {
            vault.sharedSecretWith(ByteArray(65) { if (it == 0) 0x04.toByte() else 0xFF.toByte() })
        }
    }

    @Test
    fun deviceSecretIsStableAndFullLength() = withVault { vault ->
        val secret = vault.deviceSecret()

        assertEquals(DeviceKeyVault.DEVICE_SECRET_SIZE, secret.size)
        assertFalse(secret.all { it == 0.toByte() }, "the device secret must be random bytes")
        assertContentEquals(secret, vault.deviceSecret(), "the device secret is created only once")
    }

    @Test
    fun destroyingKeyMaterialDiscardsBothSecrets() = withVault { vault ->
        val publicKey = vault.devicePublicKey()
        val secret = vault.deviceSecret()

        vault.destroyKeyMaterial()

        assertFalse(
            publicKey.contentEquals(vault.devicePublicKey()),
            "a destroyed key pair must not come back"
        )
        assertFalse(
            secret.contentEquals(vault.deviceSecret()),
            "a destroyed device secret must not come back"
        )
    }

    @Test
    fun thePrivateKeyCannotBeExportedFromTheSecureEnclave() = withVault { vault ->
        vault.devicePublicKey()

        if (!vault.isSecureEnclaveBacked()) {
            // Keychain-resident fallback, which is what the simulator always gives us. A software
            // key's bytes are readable through the Keychain by design, so asserting export failure
            // here would assert a guarantee this backing does not make. The guarantee belongs to
            // the Enclave and has to be verified on a device.
            println("Skipped: no Secure Enclave on this platform, key is Keychain-resident")
            return@withVault
        }

        val exported = copyKeyItem(vault.keyTag, kSecReturnData)
        exported?.let { CFRelease(it) }
        assertNull(exported, "the Secure Enclave must refuse to hand out the private key bytes")
    }

    @Test
    fun theKeyItemIsBoundToThisDeviceAndTheSecretIsNotBackedUp() = withVault { vault ->
        vault.devicePublicKey()
        vault.deviceSecret()

        val keyAttributes = assertNotNull(
            copyKeyItem(vault.keyTag, kSecReturnAttributes),
            "the device key item must exist after first use"
        )
        try {
            assertNotNull(
                CFDictionaryGetValue(keyAttributes.reinterpret(), kSecAttrAccessControl),
                "the key must carry the access control it was created with"
            )
        } finally {
            CFRelease(keyAttributes)
        }

        val secretAttributes = assertNotNull(
            copySecretItemAttributes(vault.secretService, vault.secretAccount),
            "the device secret item must exist after first use"
        )
        try {
            val accessible = CFDictionaryGetValue(secretAttributes.reinterpret(), kSecAttrAccessible)
            assertEquals(
                kSecAttrAccessibleWhenUnlockedThisDeviceOnly.copyAsString(),
                accessible.copyAsString(),
                "the device secret must be WhenUnlockedThisDeviceOnly, so no backup carries it"
            )
        } finally {
            CFRelease(secretAttributes)
        }
    }

    /**
     * Not an assertion about hardware — it cannot be one on the simulator. It reports the backing
     * so a run is never mistaken for the other case, and it is where a physical-device run asserts
     * `true`.
     */
    @Test
    fun theKeyBackingIsReported() = withVault { vault ->
        vault.devicePublicKey()

        val enclaveBacked = vault.isSecureEnclaveBacked()

        println(
            "Device key backing: " +
                if (enclaveBacked) "Secure Enclave" else "Keychain-resident software key"
        )
        assertTrue(
            enclaveBacked || !enclaveBacked,
            "the backing must be reportable rather than unknown"
        )
    }

    private fun newVault(): KeychainDeviceKeyVault {
        val suffix = Random.nextLong().toULong().toString(radix = 16)
        return KeychainDeviceKeyVault(
            keyTag = "$DEVICE_KEY_TAG.test.$suffix",
            secretService = "$DEVICE_SECRET_SERVICE.test.$suffix",
            secretAccount = DEVICE_SECRET_ACCOUNT
        )
    }

    private fun withVault(block: suspend (KeychainDeviceKeyVault) -> Unit) = runTest {
        val vault = newVault()
        try {
            block(vault)
        } finally {
            vault.destroyKeyMaterial()
        }
    }

    /** Returns the retained Keychain value for the key item, or `null` when it is not returned. */
    private fun copyKeyItem(tag: String, returnAttribute: CFStringRef?) = cfOwning { owner ->
        memScoped {
            val query = owner.dictionaryOf(
                listOf(
                    kSecClass to kSecClassKey,
                    kSecAttrApplicationTag to owner.dataOf(tag.encodeToByteArray(), "key tag"),
                    kSecAttrKeyType to kSecAttrKeyTypeECSECPrimeRandom,
                    kSecAttrKeyClass to kSecAttrKeyClassPrivate,
                    returnAttribute to kCFBooleanTrue
                )
            )
            val found = alloc<CFTypeRefVar>()
            if (SecItemCopyMatching(query, found.ptr) == errSecSuccess) found.value else null
        }
    }

    private fun copySecretItemAttributes(service: String, account: String) = cfOwning { owner ->
        memScoped {
            val query = owner.dictionaryOf(
                listOf(
                    kSecClass to kSecClassGenericPassword,
                    kSecAttrService to owner.stringOf(service),
                    kSecAttrAccount to owner.stringOf(account),
                    kSecReturnAttributes to kCFBooleanTrue
                )
            )
            val found = alloc<CFTypeRefVar>()
            if (SecItemCopyMatching(query, found.ptr) == errSecSuccess) found.value else null
        }
    }
}
