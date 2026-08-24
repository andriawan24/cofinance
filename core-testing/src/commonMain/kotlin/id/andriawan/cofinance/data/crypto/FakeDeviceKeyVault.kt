package id.andriawan.cofinance.data.crypto

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.EC
import dev.whyoleg.cryptography.algorithms.ECDH
import dev.whyoleg.cryptography.random.CryptographyRandom

/**
 * A [DeviceKeyVault] backed by software P-256, so the wrapping layer is exercisable in
 * `commonTest`.
 *
 * This is a test double and deliberately provides no hardware guarantee: the private key and the
 * device secret live in process memory, are extractable by anything in the test JVM, and vanish
 * with the instance. It reproduces the interface's behavior — same public key across calls, ECDH
 * agreement against a peer, a stable device secret, destruction that discards both — and nothing
 * about where the real implementations keep their material.
 */
class FakeDeviceKeyVault : DeviceKeyVault {

    private val ecdh = CryptographyProvider.Default.get(ECDH)
    private var keyPair: ECDH.KeyPair? = null
    private var secret: ByteArray? = null

    override suspend fun devicePublicKey(): ByteArray =
        keyPair().publicKey.encodeToByteArray(EC.PublicKey.Format.RAW)

    override suspend fun sharedSecretWith(peerPublicKey: ByteArray): ByteArray {
        val peer = try {
            ecdh.publicKeyDecoder(EC.Curve.P256)
                .decodeFromByteArray(EC.PublicKey.Format.RAW, peerPublicKey)
        } catch (cause: Throwable) {
            throw DeviceKeyVaultException("Peer public key is not a P-256 point", cause)
        }

        return keyPair().privateKey.sharedSecretGenerator().generateSharedSecretToByteArray(peer)
    }

    override suspend fun deviceSecret(): ByteArray = secret
        ?: CryptographyRandom.nextBytes(DeviceKeyVault.DEVICE_SECRET_SIZE).also { secret = it }

    override suspend fun destroyKeyMaterial() {
        keyPair = null
        secret = null
    }

    private suspend fun keyPair(): ECDH.KeyPair = keyPair
        ?: ecdh.keyPairGenerator(EC.Curve.P256).generateKey().also { keyPair = it }
}
