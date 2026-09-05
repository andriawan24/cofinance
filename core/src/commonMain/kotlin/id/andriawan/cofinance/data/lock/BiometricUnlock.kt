package id.andriawan.cofinance.data.lock

import id.andriawan.cofinance.data.crypto.DataKey

/** What enabling biometric unlock did. */
sealed interface BiometricEnableResult {
    data object Enabled : BiometricEnableResult
    data object PinRequired : BiometricEnableResult
    data object IncorrectPin : BiometricEnableResult
    data object Cancelled : BiometricEnableResult
    data class Unavailable(val capability: BiometricCapability) : BiometricEnableResult
    data class Failed(val reason: String) : BiometricEnableResult
}

/** What a biometric unlock attempt produced. */
sealed interface BiometricUnlockResult {
    data class Unlocked(val dataKey: DataKey) : BiometricUnlockResult
    data class FellBackToPin(val reason: PinFallbackReason) : BiometricUnlockResult
}

/** Why the biometric path handed the user back to the PIN. */
enum class PinFallbackReason {
    NotEnabled,
    Cancelled,
    Failed,
    Invalidated,
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
    suspend fun capability(): BiometricCapability = box.capability()

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
            payload.fill(0)
        }
    }

    /** Prompts and returns the data key, or the reason the caller should ask for the PIN instead. */
    suspend fun unlock(prompt: BiometricPromptText): BiometricUnlockResult =
        when (val opened = box.open(prompt)) {
            is BiometricOpenResult.Opened -> decodePayload(opened.plaintext)
                ?.let { BiometricUnlockResult.Unlocked(it) }
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
        const val MAXIMUM_KEY_ID_BYTES = 255
    }
}
