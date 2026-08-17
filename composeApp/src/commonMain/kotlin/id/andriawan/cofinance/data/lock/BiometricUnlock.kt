package id.andriawan.cofinance.data.lock

import id.andriawan.cofinance.data.crypto.DataKey

/** What enabling biometric unlock did. */
sealed interface BiometricEnableResult {

    /** The data key is now sealed behind the biometric-gated key as well as behind the PIN. */
    data object Enabled : BiometricEnableResult

    /** No PIN is set. Per Decision 4 biometric is a shortcut over the PIN, never a replacement. */
    data object PinRequired : BiometricEnableResult

    /** The PIN offered to authorize the change was not the PIN in effect. */
    data object IncorrectPin : BiometricEnableResult

    /** The user dismissed the biometric prompt. Nothing changed. */
    data object Cancelled : BiometricEnableResult

    /** Biometric authentication cannot be offered on this device right now. */
    data class Unavailable(val capability: BiometricCapability) : BiometricEnableResult

    /** The platform refused for a reason worth logging but not showing. */
    data class Failed(val reason: String) : BiometricEnableResult
}

/** What a biometric unlock attempt produced. */
sealed interface BiometricUnlockResult {

    /** Authentication succeeded and the data key is in hand, without the PIN being entered. */
    data class Unlocked(val dataKey: DataKey) : BiometricUnlockResult

    /**
     * Every way the biometric path can fail to produce a key.
     *
     * They are one branch on purpose: the caller's response to all of them is identical — show PIN
     * entry — and the specification asks for exactly that in each case. [reason] exists so the
     * unlock screen can say something useful, not so it can behave differently.
     */
    data class FellBackToPin(val reason: PinFallbackReason) : BiometricUnlockResult
}

/** Why the biometric path handed the user back to the PIN. */
enum class PinFallbackReason {

    /** Biometric unlock is not enabled on this device. */
    NotEnabled,

    /** The user dismissed the prompt. */
    Cancelled,

    /** Authentication was attempted and did not succeed. */
    Failed,

    /** The enrolled biometrics changed, so the biometric key was destroyed. */
    Invalidated,

    /** No usable biometric hardware or enrollment right now. */
    Unavailable
}

/**
 * Biometric unlock: a shortcut over the PIN, never an alternative to it.
 *
 * Enabling it seals the data key a second time, behind a platform key that requires a biometric
 * authentication per use and that the platform destroys when the enrolled biometric set changes.
 * The PIN wrap is untouched by all of it, which is what makes an enrollment change a six-digit
 * inconvenience rather than a recovery-phrase restore.
 *
 * Requiring the PIN is structural rather than a flag: [AppLock.enableBiometric] obtains the data key
 * by *deriving* it from the PIN the user just entered, so a device with no PIN wrap has nothing to
 * derive from and nothing to seal. There is no path here that takes a data key from an already
 * unlocked session and seals it without the PIN being re-entered.
 *
 * The sealed payload is the data key's raw bytes together with its identifier, because a key
 * without its identifier cannot be matched to the records it sealed. It is framed rather than
 * concatenated so that an identifier of any length reads back exactly.
 */
class BiometricUnlock(private val box: BiometricKeyBox) {

    /** Whether a prompt can be shown, for a settings screen deciding whether to offer the toggle. */
    suspend fun capability(): BiometricCapability = box.capability()

    /** Whether biometric unlock is on, which is the presence of a sealed copy of the data key. */
    suspend fun isEnabled(): Boolean = box.hasSealedSecret()

    /**
     * Seals [dataKey] behind the biometric key.
     *
     * Callers must have proven the PIN first; [AppLock] is the one that does, and this method is
     * internal to that flow rather than something a screen calls directly.
     */
    suspend fun enable(dataKey: DataKey, prompt: BiometricPromptText): BiometricEnableResult {
        val capability = box.capability()
        if (capability != BiometricCapability.Available) {
            return BiometricEnableResult.Unavailable(capability)
        }
        val payload = encodePayload(dataKey)
        return try {
            when (val sealed = box.seal(payload, prompt)) {
                BiometricSealResult.Sealed -> BiometricEnableResult.Enabled
                BiometricSealResult.Cancelled -> BiometricEnableResult.Cancelled
                BiometricSealResult.Unavailable ->
                    BiometricEnableResult.Unavailable(box.capability())

                is BiometricSealResult.Failed -> BiometricEnableResult.Failed(sealed.reason)
            }
        } finally {
            // The payload carries the data key in the clear. It is this function's to discard.
            payload.fill(0)
        }
    }

    /** Prompts and returns the data key, or the reason the caller should ask for the PIN instead. */
    suspend fun unlock(prompt: BiometricPromptText): BiometricUnlockResult =
        when (val opened = box.open(prompt)) {
            is BiometricOpenResult.Opened -> decodePayload(opened.plaintext)
                ?.let { BiometricUnlockResult.Unlocked(it) }
                // Unreadable sealed bytes are treated as a failure rather than as corruption to
                // report: the PIN opens the same key, so there is nothing lost and nothing to say.
                ?: BiometricUnlockResult.FellBackToPin(PinFallbackReason.Failed)

            BiometricOpenResult.Cancelled ->
                BiometricUnlockResult.FellBackToPin(PinFallbackReason.Cancelled)

            is BiometricOpenResult.Failed ->
                BiometricUnlockResult.FellBackToPin(PinFallbackReason.Failed)

            BiometricOpenResult.Invalidated ->
                BiometricUnlockResult.FellBackToPin(PinFallbackReason.Invalidated)

            BiometricOpenResult.Absent ->
                BiometricUnlockResult.FellBackToPin(PinFallbackReason.NotEnabled)

            BiometricOpenResult.Unavailable ->
                BiometricUnlockResult.FellBackToPin(PinFallbackReason.Unavailable)
        }

    /** Turns biometric unlock off, after which unlocking requires the PIN. */
    suspend fun disable() = box.clear()

    private suspend fun encodePayload(dataKey: DataKey): ByteArray {
        val id = dataKey.id.encodeToByteArray()
        require(id.size in 1..MAXIMUM_KEY_ID_BYTES) { "Data key identifier is not storable" }
        val raw = dataKey.exportRawBytes()
        return try {
            ByteArray(1 + id.size + raw.size).also { payload ->
                payload[0] = id.size.toByte()
                id.copyInto(payload, 1)
                raw.copyInto(payload, 1 + id.size)
            }
        } finally {
            raw.fill(0)
        }
    }

    private suspend fun decodePayload(payload: ByteArray): DataKey? {
        if (payload.isEmpty()) return null
        val idLength = payload[0].toInt() and 0xFF
        if (idLength == 0 || payload.size <= 1 + idLength) return null
        val id = payload.decodeToString(1, 1 + idLength)
        val raw = payload.copyOfRange(1 + idLength, payload.size)
        return try {
            DataKey.fromRawBytes(id, raw)
        } catch (_: IllegalArgumentException) {
            null
        } finally {
            raw.fill(0)
            payload.fill(0)
        }
    }

    private companion object {
        /** One length byte frames the identifier, so it cannot exceed what a byte can express. */
        const val MAXIMUM_KEY_ID_BYTES = 255
    }
}
