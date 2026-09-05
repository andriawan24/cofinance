package id.andriawan.cofinance.data.crypto

/**
 * The platform's hardware-backed key storage, as the wrapping layer needs to see it.
 *
 * Two independent secrets live behind this interface, both created on first use and both bound to
 * this device:
 *
 * - a P-256 key pair whose private half never leaves platform storage. The curve follows the
 *   hardware rather than preference — the Secure Enclave supports P-256 only — and the private key
 *   is usable exclusively through [sharedSecretWith], which is what makes "only this device can
 *   open it" true against an attacker holding the filesystem.
 * - a device secret the PIN is composed with, so that deriving the PIN wrap requires possession of
 *   the device in addition to knowledge of six digits.
 *
 * Public keys and shared secrets cross this boundary as raw bytes rather than as library key types,
 * because a platform key is a handle inside Keystore or the Keychain and has no in-process form.
 */
interface DeviceKeyVault {

    /**
     * Returns the device public key, creating the key pair on first call.
     *
     * The encoding is the uncompressed SEC 1 point, `0x04 || X || Y`, which is what both platforms
     * hand back and what the software fake in tests produces.
     */
    suspend fun devicePublicKey(): ByteArray

    /**
     * Performs ECDH between the device private key and [peerPublicKey], returning the raw shared
     * secret.
     *
     * The result is agreement output, not a key: the wrapping layer runs it through HKDF before
     * anything is encrypted under it.
     */
    suspend fun sharedSecretWith(peerPublicKey: ByteArray): ByteArray

    /**
     * Returns the device secret, creating it on first call and returning the same value afterwards.
     *
     * "Non-extractable" here means the secret cannot leave this device: it is created inside
     * platform secure storage, is never uploaded, and is not part of a device backup. It is
     * readable in process because the PIN composition has to combine it with a derived value.
     */
    suspend fun deviceSecret(): ByteArray

    /**
     * Destroys the device key pair and the device secret.
     *
     * Called when consecutive failed PIN attempts reach the threshold. Records stay recoverable
     * through the recovery-phrase wrap, which is the point of holding two independent wraps.
     */
    suspend fun destroyKeyMaterial()

    companion object {
        const val PUBLIC_KEY_SIZE: Int = 65

        const val DEVICE_SECRET_SIZE: Int = 32
    }
}

class DeviceKeyVaultException(message: String, cause: Throwable? = null) : Exception(message, cause)

expect fun createDeviceKeyVault(): DeviceKeyVault
