package id.andriawan.cofinance.data.crypto

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlinx.coroutines.test.runTest

/**
 * Exercises the vault contract through the software fake, which is the only implementation common
 * tests can reach. Hardware backing and non-extractability are platform properties and are asserted
 * by the Android and iOS checks in tasks 2.3 and 2.4.
 */
class DeviceKeyVaultTest {

    @Test
    fun fakeSatisfiesTheVaultInterface() = runTest {
        val vault: DeviceKeyVault = FakeDeviceKeyVault()

        val publicKey = vault.devicePublicKey()
        val secret = vault.deviceSecret()

        assertEquals(DeviceKeyVault.PUBLIC_KEY_SIZE, publicKey.size)
        assertEquals(0x04.toByte(), publicKey.first())
        assertEquals(DeviceKeyVault.DEVICE_SECRET_SIZE, secret.size)
    }

    @Test
    fun deviceKeyMaterialIsStableAcrossCalls() = runTest {
        val vault = FakeDeviceKeyVault()

        assertContentEquals(vault.devicePublicKey(), vault.devicePublicKey())
        assertContentEquals(vault.deviceSecret(), vault.deviceSecret())
    }

    @Test
    fun twoPartiesAgreeOnTheSameSharedSecret() = runTest {
        val device = FakeDeviceKeyVault()
        val peer = FakeDeviceKeyVault()

        val fromDevice = device.sharedSecretWith(peer.devicePublicKey())
        val fromPeer = peer.sharedSecretWith(device.devicePublicKey())

        assertContentEquals(fromDevice, fromPeer)
        assertEquals(32, fromDevice.size)
    }

    @Test
    fun athirdPartyAgreesOnSomethingElse() = runTest {
        val device = FakeDeviceKeyVault()
        val peer = FakeDeviceKeyVault()
        val stranger = FakeDeviceKeyVault()

        val agreed = device.sharedSecretWith(peer.devicePublicKey())
        val strangerAgreed = stranger.sharedSecretWith(peer.devicePublicKey())

        assertFalse(agreed.contentEquals(strangerAgreed))
    }

    @Test
    fun destroyedMaterialIsNotRecreatedAsItself() = runTest {
        val vault = FakeDeviceKeyVault()
        val publicKey = vault.devicePublicKey()
        val secret = vault.deviceSecret()

        vault.destroyKeyMaterial()

        assertFalse(publicKey.contentEquals(vault.devicePublicKey()))
        assertFalse(secret.contentEquals(vault.deviceSecret()))
    }

    @Test
    fun peerKeyThatIsNotACurvePointIsRejected() = runTest {
        val vault = FakeDeviceKeyVault()

        assertFailsWith<DeviceKeyVaultException> {
            vault.sharedSecretWith(ByteArray(DeviceKeyVault.PUBLIC_KEY_SIZE) { 1 })
        }
    }
}
