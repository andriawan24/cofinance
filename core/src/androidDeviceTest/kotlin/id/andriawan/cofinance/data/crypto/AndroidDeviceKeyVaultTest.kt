package id.andriawan.cofinance.data.crypto

import android.os.Build
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.math.BigInteger
import java.security.AlgorithmParameters
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECParameterSpec
import java.security.spec.ECPoint
import java.security.spec.ECPublicKeySpec
import java.security.KeyFactory
import java.security.interfaces.ECPublicKey
import javax.crypto.KeyAgreement

/**
 * Device test for the Android Keystore vault.
 *
 * This has to run on a device or emulator: the Android Keystore has no host-side implementation, so
 * the properties under test — that the key material is hardware-backed and that the private key
 * cannot be exported — are unobservable from a JVM unit test.
 *
 * The assertions are written per API level rather than assuming the strongest case, because the
 * module floor is API 24 and the Keystore's key agreement support starts at 31. See the class
 * documentation on [AndroidDeviceKeyVault] for what each level actually guarantees.
 */
class AndroidDeviceKeyVaultTest {

    private lateinit var vault: AndroidDeviceKeyVault

    @Before
    fun setUp() = runBlocking {
        vault = createDeviceKeyVault() as AndroidDeviceKeyVault
        vault.destroyKeyMaterial()
    }

    @After
    fun tearDown() = runBlocking {
        vault.destroyKeyMaterial()
    }

    @Test
    fun deviceKeyMaterialIsHardwareBacked() = runBlocking {
        vault.devicePublicKey()
        vault.deviceSecret()

        val protection = vault.protection()

        assertTrue(
            "device secret key is not hardware-backed: ${protection.secret}",
            protection.secret.isSecureHardware()
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            assertTrue(
                "agreement key is not hardware-backed: ${protection.agreement}",
                protection.agreement.isSecureHardware()
            )
            assertNull("no sealing key is expected on API 31+", protection.sealing)
        } else {
            assertTrue(
                "sealing key is not hardware-backed: ${protection.sealing}",
                protection.sealing.isSecureHardware()
            )
            assertNull("no keystore agreement key exists below API 31", protection.agreement)
        }
    }

    @Test
    fun keystoreResidentPrivateKeyCannotBeExported() = runBlocking {
        vault.devicePublicKey()
        vault.deviceSecret()

        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

        val secretKey = keyStore.getKey(AndroidDeviceKeyVault.SECRET_KEY_ALIAS, null)
        assertNotNull(secretKey)
        assertNull("device secret key material escaped the keystore", secretKey.encoded)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val agreementKey = keyStore.getKey(AndroidDeviceKeyVault.AGREEMENT_KEY_ALIAS, null)
            assertNotNull(agreementKey)
            assertNull("device private key material escaped the keystore", agreementKey.encoded)
        } else {
            val sealingKey = keyStore.getKey(AndroidDeviceKeyVault.SEALING_KEY_ALIAS, null)
            assertNotNull(sealingKey)
            assertNull("sealing key material escaped the keystore", sealingKey.encoded)
        }
    }

    @Test
    fun sealedAgreementKeyOnDiskCarriesNoPlaintextKeyMaterial() = runBlocking {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) return@runBlocking

        val publicKey = vault.devicePublicKey()
        val sealed = java.io.File(
            DeviceKeyVaultStorage.directory(),
            AndroidDeviceKeyVault.SEALED_AGREEMENT_KEY_FILE
        ).readBytes()

        assertTrue("sealed agreement key was not written", sealed.isNotEmpty())
        assertFalse(
            "the sealed file exposes the public point, so it is not encrypted",
            sealed.containsSequence(publicKey)
        )
    }

    @Test
    fun devicePublicKeyIsAStableUncompressedP256Point() = runBlocking {
        val first = vault.devicePublicKey()
        val second = vault.devicePublicKey()

        assertEquals(DeviceKeyVault.PUBLIC_KEY_SIZE, first.size)
        assertEquals(0x04.toByte(), first[0])
        assertArrayEquals(first, second)
    }

    @Test
    fun sharedSecretMatchesThePeersOwnAgreement() = runBlocking {
        val peer = generateSoftwareKeyPair()

        val fromVault = vault.sharedSecretWith(encodeUncompressed(peer.public as ECPublicKey))
        val fromPeer = KeyAgreement.getInstance("ECDH").run {
            init(peer.private)
            doPhase(decodeUncompressed(vault.devicePublicKey()), true)
            generateSecret()
        }

        assertArrayEquals(fromPeer, fromVault)
    }

    @Test
    fun deviceSecretIsStableAndFullLength() = runBlocking {
        val first = vault.deviceSecret()
        val second = vault.deviceSecret()

        assertEquals(DeviceKeyVault.DEVICE_SECRET_SIZE, first.size)
        assertArrayEquals(first, second)
    }

    @Test
    fun destroyingKeyMaterialReplacesBothSecrets() = runBlocking {
        val publicKey = vault.devicePublicKey()
        val secret = vault.deviceSecret()

        vault.destroyKeyMaterial()

        assertFalse(publicKey.contentEquals(vault.devicePublicKey()))
        assertFalse(secret.contentEquals(vault.deviceSecret()))
    }

    @Test
    fun peerKeyOffTheCurveIsRejected() = runBlocking {
        val offCurve = ByteArray(DeviceKeyVault.PUBLIC_KEY_SIZE).also { it[0] = 0x04 }
        offCurve[DeviceKeyVault.PUBLIC_KEY_SIZE - 1] = 0x01

        try {
            vault.sharedSecretWith(offCurve)
            fail("an off-curve peer point was accepted")
        } catch (expected: DeviceKeyVaultException) {
            assertNotNull(expected.message)
        }
    }

    @Test
    fun peerKeyOfTheWrongLengthIsRejected() = runBlocking {
        try {
            vault.sharedSecretWith(ByteArray(32))
            fail("a malformed peer point was accepted")
        } catch (expected: DeviceKeyVaultException) {
            assertNotNull(expected.message)
        }
    }

    private fun DeviceKeySecurityLevel?.isSecureHardware(): Boolean =
        this == DeviceKeySecurityLevel.TRUSTED_ENVIRONMENT ||
            this == DeviceKeySecurityLevel.STRONG_BOX

    private fun generateSoftwareKeyPair(): KeyPair =
        KeyPairGenerator.getInstance("EC").run {
            initialize(ECGenParameterSpec("secp256r1"))
            generateKeyPair()
        }

    private fun encodeUncompressed(key: ECPublicKey): ByteArray {
        val x = key.w.affineX.toFixedLength(32)
        val y = key.w.affineY.toFixedLength(32)
        return ByteArray(DeviceKeyVault.PUBLIC_KEY_SIZE).also {
            it[0] = 0x04
            x.copyInto(it, 1)
            y.copyInto(it, 33)
        }
    }

    private fun decodeUncompressed(encoded: ByteArray): ECPublicKey {
        val parameters = AlgorithmParameters.getInstance("EC").run {
            init(ECGenParameterSpec("secp256r1"))
            getParameterSpec(ECParameterSpec::class.java)
        }
        val point = ECPoint(
            BigInteger(1, encoded.copyOfRange(1, 33)),
            BigInteger(1, encoded.copyOfRange(33, 65))
        )
        return KeyFactory.getInstance("EC")
            .generatePublic(ECPublicKeySpec(point, parameters)) as ECPublicKey
    }

    private fun BigInteger.toFixedLength(length: Int): ByteArray {
        val magnitude = toByteArray()
        val offset = (magnitude.size - length).coerceAtLeast(0)
        val copied = magnitude.size - offset
        return ByteArray(length).also { magnitude.copyInto(it, length - copied, offset) }
    }

    private fun ByteArray.containsSequence(needle: ByteArray): Boolean {
        if (needle.isEmpty() || needle.size > size) return false
        outer@ for (start in 0..size - needle.size) {
            for (index in needle.indices) {
                if (this[start + index] != needle[index]) continue@outer
            }
            return true
        }
        return false
    }
}
