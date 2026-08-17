package id.andriawan.cofinance.data.crypto

import dev.whyoleg.cryptography.algorithms.AES
import dev.whyoleg.cryptography.random.CryptographyRandom

/**
 * Wraps and unwraps the data key against the app PIN composed with the device-held secret.
 *
 * This is the copy the unlock screen opens, and it never leaves the device — see
 * [KeyMaterialDocument.uploadableKeyMaterial], which uploads the recovery-phrase wrap only. A PIN
 * has 10^6 values, so a copy of it in the backend would be exhaustively searchable by anyone holding
 * the backend; keeping it local is what makes "backend material alone reveals nothing a PIN could
 * unlock" true, per Decision 3.
 *
 * Unlock here is derivation, not comparison. Nothing about the PIN is stored: no PIN, no hash, no
 * verifier, and no branch anywhere below decides whether the caller "may" have the key. The only
 * route to the data key is to actually derive the wrapping key from the PIN and the device secret
 * and have AES-GCM authenticate under it, so there is no code path an attacker could skip to be
 * handed the key. A stored PIN hash was rejected outright by Decision 3 for exactly that reason.
 *
 * The derivation runs the PIN through scrypt at [ScryptParameters.PIN] first — memory-hard, so
 * on-device guessing is bounded by memory bandwidth rather than hash throughput — and only then
 * composes the stretched output with [DeviceKeyVault.deviceSecret]. See [wrappingKey] for why that
 * composition cannot be short-circuited from either side.
 *
 * Callers must run wrap and unwrap off the main thread: scrypt at N=16384 allocates 16 MiB and costs
 * tens to low hundreds of milliseconds per call, once per unlock.
 */
class PinKeyWrapper(
    private val vault: DeviceKeyVault,
    private val random: CryptographyRandom = CryptographyRandom
) {

    /**
     * Produces the local-only wrapped copy of [dataKey] that [pin] opens on this device.
     *
     * The salt and the nonce are fresh [CryptographyRandom] draws per call, never derived from the
     * PIN or from a counter: a repeated salt would make two wraps share a wrapping key, and a
     * repeated nonce under GCM is catastrophic rather than degrading.
     *
     * @throws KeyWrapException when [pin] is empty, or when the device secret cannot be reached.
     */
    suspend fun wrap(dataKey: DataKey, pin: String): WrappedDataKey {
        val salt = random.nextBytes(KeyWrapping.SALT_SIZE)
        val wrappingKey = wrappingKey(pin, salt)
        val nonce = random.nextBytes(KeyWrapping.NONCE_SIZE)
        return WrappedDataKey.of(
            type = KeyWrapType.Pin,
            keyId = dataKey.id,
            wrappedKey = KeyWrapping.seal(wrappingKey, dataKey, nonce),
            parameters = mapOf(
                WrappedDataKey.NONCE_PARAMETER to nonce,
                WrappedDataKey.SALT_PARAMETER to salt
            )
        )
    }

    /**
     * Opens [wrap] with [pin] on this device, returning the data key in memory.
     *
     * @throws KeyWrapException when the copy is not a PIN wrap, is missing or carries malformed
     * parameters, when the device secret cannot be reached, when [pin] is empty, or when [pin] is
     * not the PIN this copy was wrapped under. A wrong PIN and a wrap carried to another device
     * reach the same failure, because both derive a different wrapping key and so fail
     * authentication; neither returns anything the caller could inspect to learn which it was.
     */
    suspend fun unwrap(wrap: WrappedDataKey, pin: String): DataKey {
        KeyWrapping.requireType(wrap, KeyWrapType.Pin)
        val salt = KeyWrapping.requiredParameter(wrap, WrappedDataKey.SALT_PARAMETER)
        val nonce = KeyWrapping.requiredParameter(wrap, WrappedDataKey.NONCE_PARAMETER)

        return KeyWrapping.open(wrappingKey(pin, salt), wrap, nonce)
    }

    /**
     * Derives the wrapping key from [pin] and the device secret, stretching the PIN first.
     *
     * The composition is `HKDF-SHA256(scrypt(pin, salt) || deviceSecret)`: both inputs are fixed
     * length — [ScryptParameters.PIN] fixes the first at 32 bytes and
     * [DeviceKeyVault.DEVICE_SECRET_SIZE] the second, checked below — so the concatenation has one
     * possible reading and no input can be shifted into the other's bytes.
     *
     * Why this cannot be short-circuited from either side:
     *
     * - HKDF's extract step is one HMAC over the whole concatenation. Its output depends on every
     *   byte of both halves and is one-way, so an attacker holding one input cannot remove it from
     *   the result to isolate the other, and cannot test a candidate for one half without the other
     *   half in hand. Holding the backend's material and the correct PIN therefore yields nothing
     *   without the device, which is the entire point of Decision 3.
     * - XOR-style composition was rejected for that reason: it is invertible, so a wrapping key
     *   recovered once together with a known PIN would hand the attacker the device secret itself,
     *   after which every wrap on that device becomes an offline search over 10^6 PINs with no
     *   device required. HMAC leaves no such algebra.
     * - Layering the two — sealing under a PIN-only key and re-sealing under a device-only key —
     *   was rejected too: it creates an intermediate artifact that depends on one input alone, and
     *   anything that strips the outer layer once leaves a PIN-only wrap that can be attacked
     *   offline. Here there is one wrapping key and one seal, so no such intermediate exists.
     *
     * The scrypt step runs before the composition rather than after, per Decision 3: it exists to
     * price each PIN guess by an attacker who already has the device, and a guess only reaches the
     * cheap HKDF step by paying for the memory-hard one first.
     *
     * @throws KeyWrapException when [pin] is empty, or when the vault cannot produce a device secret
     * of the expected size.
     */
    private suspend fun wrappingKey(pin: String, salt: ByteArray): AES.GCM.Key {
        // Refused rather than derived: nothing upstream should submit an empty PIN, and refusing it
        // here means a UI that does cannot create a wrap that opens on no input at all. It surfaces
        // as the same failure a wrong PIN does, so the unlock screen has one case to handle.
        if (pin.isEmpty()) throw KeyWrapException("A PIN wrap cannot be derived from an empty PIN")

        val deviceSecret = try {
            vault.deviceSecret()
        } catch (cause: DeviceKeyVaultException) {
            throw KeyWrapException("Device key storage could not produce the device secret", cause)
        }
        if (deviceSecret.size != DeviceKeyVault.DEVICE_SECRET_SIZE) {
            throw KeyWrapException("Device secret is not ${DeviceKeyVault.DEVICE_SECRET_SIZE} bytes")
        }

        val stretched = Scrypt.derive(pin.encodeToByteArray(), salt, ScryptParameters.PIN)
        val composed = stretched + deviceSecret
        try {
            // The salt is passed to HKDF as well as to scrypt. It is not secret in either role, and
            // reusing it ties both stages of the derivation to the same stored parameter, so an
            // edited salt fails the whole derivation rather than only its first half.
            return KeyWrapping.wrappingKey(composed, salt = salt, info = INFO)
        } finally {
            // The stretched PIN and the composition are this function's to discard: either one is
            // as good as the PIN itself to an attacker who reads process memory afterwards.
            stretched.fill(0)
            composed.fill(0)
        }
    }

    private companion object {
        /**
         * The HKDF domain separator for this wrap type.
         *
         * It differs from the device and phrase separators for the reason described on
         * [KeyWrapping.wrappingKey]: all three end in an AES-256-GCM seal of the same 32 bytes, and
         * the separator is what keeps their wrapping keys unrelated, so a copy sealed in one context
         * cannot be opened by a key derived for another. The trailing version lets a later build
         * change what is derived — raising the scrypt cost, say — without silently opening copies
         * written under the older rule.
         */
        const val INFO = "cofinance/e2ee/data-key-wrap/pin/v1"
    }
}
