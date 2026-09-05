package id.andriawan.cofinance.data.crypto

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.EC
import dev.whyoleg.cryptography.algorithms.ECDH
import dev.whyoleg.cryptography.random.CryptographyRandom

/**
 * Wraps and unwraps the data key against the device key held by [vault].
 *
 * This is the copy used for ordinary unlock, and the reason a signed-in user does not have to touch
 * their recovery phrase to open their own records. It is never uploaded: the private half of the
 * device key pair lives in Android Keystore or the iOS Keychain and cannot leave the device, so the
 * wrap is meaningless anywhere else.
 *
 * The agreement is ephemeral-static ECDH over P-256 — the curve follows the hardware, per Decision 2
 * — with the raw agreement output run through HKDF-SHA256 into a 256-bit key. A fresh ephemeral key
 * pair per wrap is what makes two wraps of the same data key against the same device derive
 * different wrapping keys; the ephemeral public key is stored alongside the wrap because the device
 * needs it to complete the same agreement from its side.
 */
class DeviceKeyWrapper(
    private val vault: DeviceKeyVault,
    private val random: CryptographyRandom = CryptographyRandom
) {

    private val ecdh get() = CryptographyProvider.Default.get(ECDH)

    /**
     * Produces a device-bound wrapped copy of [dataKey].
     *
     * @throws KeyWrapException when the vault's public key is not a usable P-256 point.
     */
    suspend fun wrap(dataKey: DataKey): WrappedDataKey {
        val ephemeral = ecdh.keyPairGenerator(EC.Curve.P256).generateKey()
        val devicePublicKey = vault.devicePublicKey()
        val sharedSecret = try {
            val device = ecdh.publicKeyDecoder(EC.Curve.P256)
                .decodeFromByteArray(EC.PublicKey.Format.RAW, devicePublicKey)
            ephemeral.privateKey.sharedSecretGenerator().generateSharedSecretToByteArray(device)
        } catch (cause: Throwable) {
            throw KeyWrapException("Device public key is not a usable P-256 point", cause)
        }

        val wrappingKey = KeyWrapping.wrappingKey(sharedSecret, salt = null, info = INFO)
        val nonce = random.nextBytes(KeyWrapping.NONCE_SIZE)
        return WrappedDataKey.of(
            type = KeyWrapType.Device,
            keyId = dataKey.id,
            wrappedKey = KeyWrapping.seal(wrappingKey, dataKey, nonce),
            parameters = mapOf(
                WrappedDataKey.NONCE_PARAMETER to nonce,
                WrappedDataKey.EPHEMERAL_PUBLIC_KEY_PARAMETER to
                        ephemeral.publicKey.encodeToByteArray(EC.PublicKey.Format.RAW)
            )
        )
    }

    /**
     * Opens [wrap] with this device's key, returning the data key in memory.
     *
     * @throws KeyWrapException when the copy is not a device wrap, is missing or carries malformed
     * parameters, or was wrapped against a different device — which reaches the same authentication
     * failure as tampering, because a different device agrees on a different shared secret and so
     * derives a different wrapping key.
     */
    suspend fun unwrap(wrap: WrappedDataKey): DataKey {
        KeyWrapping.requireType(wrap, KeyWrapType.Device)
        val ephemeralPublicKey =
            KeyWrapping.requiredParameter(wrap, WrappedDataKey.EPHEMERAL_PUBLIC_KEY_PARAMETER)
        val nonce = KeyWrapping.requiredParameter(wrap, WrappedDataKey.NONCE_PARAMETER)

        val sharedSecret = try {
            vault.sharedSecretWith(ephemeralPublicKey)
        } catch (cause: DeviceKeyVaultException) {
            throw KeyWrapException("Device key storage could not complete the agreement", cause)
        }
        val wrappingKey = KeyWrapping.wrappingKey(sharedSecret, salt = null, info = INFO)
        return KeyWrapping.open(wrappingKey, wrap, nonce)
    }

    private companion object {
        const val INFO = "cofinance/e2ee/data-key-wrap/device/v1"
    }
}
