package id.andriawan.cofinance.data.lock

import id.andriawan.cofinance.data.crypto.DataKey
import id.andriawan.cofinance.data.crypto.KeyMaterialDocument
import id.andriawan.cofinance.data.crypto.KeyWrapException
import id.andriawan.cofinance.data.crypto.KeyWrapType
import id.andriawan.cofinance.data.crypto.PinKeyWrapper
import id.andriawan.cofinance.data.crypto.WrappedDataKey
import id.andriawan.cofinance.data.keyring.EncryptionSessionState
import id.andriawan.cofinance.data.keyring.InMemoryEncryptionSession
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration

/** What a PIN unlock produced. */
sealed interface PinUnlockResult {

    /** The data key is in memory and the app is usable. */
    data object Unlocked : PinUnlockResult

    /** The PIN did not open the wrap. */
    data class Incorrect(
        val attemptsRemaining: Int,
        val nextAttemptDelay: Duration
    ) : PinUnlockResult

    /** The escalating delay has not elapsed; no derivation was attempted. */
    data class Throttled(val remaining: Duration) : PinUnlockResult

    /** Consecutive failures reached the threshold. Only the recovery phrase restores access. */
    data object KeyMaterialDestroyed : PinUnlockResult

    /** This device holds no PIN wrap, so there is nothing to unlock with a PIN. */
    data object PinNotSet : PinUnlockResult
}

/** What checking the PIN produced, for callers that need the key rather than a session. */
sealed interface PinVerification {

    /** The PIN was correct. [dataKey] is in memory and is the caller's to use and drop. */
    data class Verified(val dataKey: DataKey) : PinVerification

    data class Incorrect(val attemptsRemaining: Int, val nextAttemptDelay: Duration) :
        PinVerification

    data class Throttled(val remaining: Duration) : PinVerification

    data object KeyMaterialDestroyed : PinVerification

    data object PinNotSet : PinVerification
}

/** What setting, changing, or removing the PIN produced. */
sealed interface PinChangeResult {

    data object Changed : PinChangeResult

    /** The current PIN is required to change or remove an existing one, and was wrong or absent. */
    data class CurrentPinRejected(val verification: PinVerification) : PinChangeResult

    /** No PIN is set yet and the session is locked, so there is no data key to wrap. */
    data object SessionLocked : PinChangeResult
}

/**
 * The lock, as the rest of the app sees it.
 *
 * Everything the unlock screen and the profile security section need is here, and the parts that
 * make the guarantees — the derivation-based PIN wrap, the failed-attempt schedule, the
 * biometric-gated key, the in-memory session — are separate objects underneath so each can be
 * tested on its own.
 *
 * Three rules are enforced here rather than left to callers, because a screen that forgets one is
 * a security bug rather than a display bug:
 *
 * - **The PIN is the floor.** Enabling biometric unlock derives the data key from the PIN the user
 *   just entered; there is no route that seals an already-unlocked session's key without it.
 * - **Every PIN entry is counted.** Unlocking, changing the PIN, enabling biometric, and revealing
 *   the recovery phrase all go through [FailedAttemptGuard], so an attacker cannot find an
 *   unthrottled PIN oracle by using a settings screen instead of the unlock screen.
 * - **Biometric failure is not a PIN failure.** A dismissed or invalidated biometric prompt returns
 *   the user to PIN entry and leaves the counter alone; the platform throttles biometrics itself,
 *   and counting them here would let someone lock a device out by waving the wrong finger.
 */
class AppLock(
    private val session: InMemoryEncryptionSession,
    private val keyMaterial: LocalKeyMaterialStore,
    private val pinKeyWrapper: PinKeyWrapper,
    private val attempts: FailedAttemptGuard,
    private val biometrics: BiometricUnlock,
    val autoLockSettings: AutoLockSettings
) {

    /** The session state the UI gates finance data on: unlock is required before anything shows. */
    val state: StateFlow<EncryptionSessionState> get() = session.state

    /** Whether this device holds a PIN wrap at all. */
    suspend fun isPinSet(): Boolean = pinWrap() != null

    /** Whether biometric unlock is on. */
    suspend fun isBiometricEnabled(): Boolean = biometrics.isEnabled()

    /** Whether a biometric prompt could be shown, for the settings toggle's enabled state. */
    suspend fun biometricCapability(): BiometricCapability = biometrics.capability()

    /** Unlocks the session with [pin], or explains why it did not. */
    suspend fun unlockWithPin(pin: String): PinUnlockResult = when (val outcome = verifyPin(pin)) {
        is PinVerification.Verified -> {
            session.unlock(outcome.dataKey)
            PinUnlockResult.Unlocked
        }

        is PinVerification.Incorrect ->
            PinUnlockResult.Incorrect(outcome.attemptsRemaining, outcome.nextAttemptDelay)

        is PinVerification.Throttled -> PinUnlockResult.Throttled(outcome.remaining)
        PinVerification.KeyMaterialDestroyed -> PinUnlockResult.KeyMaterialDestroyed
        PinVerification.PinNotSet -> PinUnlockResult.PinNotSet
    }

    /**
     * Derives the data key from [pin] without unlocking the session.
     *
     * This is what a settings action asks for: changing the PIN, enabling biometric unlock, and
     * re-displaying the recovery phrase all need proof of the PIN at the moment of the request, and
     * per Decision 10 an already-unlocked session is no substitute for it.
     */
    suspend fun verifyPin(pin: String): PinVerification {
        val wrap = pinWrap() ?: return PinVerification.PinNotSet

        when (val gate = attempts.beginAttempt()) {
            AttemptGate.Allowed -> Unit
            is AttemptGate.Throttled -> return PinVerification.Throttled(gate.remaining)
            AttemptGate.KeyMaterialDestroyed -> return PinVerification.KeyMaterialDestroyed
        }

        val dataKey = try {
            pinKeyWrapper.unwrap(wrap, pin)
        } catch (_: KeyWrapException) {
            // The wrong PIN and a wrap carried to another device fail identically here, by design:
            // the derivation produces a different wrapping key and AES-GCM refuses it. Nothing in
            // this catch inspects why, so there is no branch an attacker could aim at.
            return when (val outcome = attempts.recordFailure()) {
                is FailedAttemptOutcome.Counted -> PinVerification.Incorrect(
                    attemptsRemaining = outcome.attemptsRemaining,
                    nextAttemptDelay = outcome.nextAttemptDelay
                )

                FailedAttemptOutcome.KeyMaterialDestroyed -> PinVerification.KeyMaterialDestroyed
            }
        }

        attempts.recordSuccess()
        return PinVerification.Verified(dataKey)
    }

    /**
     * Unlocks the session with a biometric, falling back to the PIN in every other case.
     *
     * A successful biometric never touches the failed-attempt counter, and neither does a failed
     * one. See the class documentation for why.
     */
    suspend fun unlockWithBiometric(prompt: BiometricPromptText): BiometricUnlockResult {
        if (pinWrap() == null) {
            // A biometric copy with no PIN behind it should not exist; if one somehow does, it is
            // not honored, because Decision 4 makes the PIN the floor rather than an option.
            return BiometricUnlockResult.FellBackToPin(PinFallbackReason.NotEnabled)
        }
        val result = biometrics.unlock(prompt)
        if (result is BiometricUnlockResult.Unlocked) {
            session.unlock(result.dataKey)
        }
        return result
    }

    /**
     * Sets or changes the PIN.
     *
     * [currentPin] is required whenever a PIN is already set — that is the specification's
     * requirement that lock settings changes need the current PIN — and is ignored when setting the
     * first one, which instead requires an unlocked session because that is the only other place a
     * data key can legitimately come from.
     */
    suspend fun setPin(newPin: String, currentPin: String?): PinChangeResult {
        val existing = pinWrap()
        val dataKey = if (existing == null) {
            session.dataKeyOrNull() ?: return PinChangeResult.SessionLocked
        } else {
            when (val verification = verifyPin(currentPin.orEmpty())) {
                is PinVerification.Verified -> verification.dataKey
                else -> return PinChangeResult.CurrentPinRejected(verification)
            }
        }

        val document = keyMaterial.read() ?: KeyMaterialDocument(
            keyMaterialVersion = KeyMaterialDocument.CURRENT_VERSION
        )
        val replaced = document.copy(
            wrappedKeys = document.wrappedKeys.filterNot { it.type == KeyWrapType.Pin } +
                pinKeyWrapper.wrap(dataKey, newPin)
        )
        keyMaterial.write(replaced)
        // Arming after the write, so an interruption leaves a device with no PIN wrap and no
        // counter rather than a counter whose absence would later read as tampering.
        attempts.arm()
        return PinChangeResult.Changed
    }

    /**
     * Removes the PIN, which also turns biometric unlock off.
     *
     * Biometric goes with it because Decision 4 defines it as a shortcut over the PIN; leaving a
     * biometric copy behind would make it the only credential, which is the arrangement the
     * decision exists to prevent.
     */
    suspend fun removePin(currentPin: String): PinChangeResult {
        val verification = verifyPin(currentPin)
        if (verification !is PinVerification.Verified) {
            return PinChangeResult.CurrentPinRejected(verification)
        }
        val document = keyMaterial.read() ?: return PinChangeResult.Changed
        keyMaterial.write(
            document.copy(wrappedKeys = document.wrappedKeys.filterNot { it.type == KeyWrapType.Pin })
        )
        biometrics.disable()
        return PinChangeResult.Changed
    }

    /**
     * Turns biometric unlock on, which requires the PIN to already be set and to be re-entered.
     *
     * Returning [BiometricEnableResult.PinRequired] when no PIN wrap exists is the specification's
     * "the app SHALL require the PIN to be set first", and it is checked before any prompt is
     * shown so the user is not asked for a finger before being told what is missing.
     */
    suspend fun enableBiometric(
        currentPin: String,
        prompt: BiometricPromptText
    ): BiometricEnableResult {
        if (pinWrap() == null) return BiometricEnableResult.PinRequired

        return when (val verification = verifyPin(currentPin)) {
            is PinVerification.Verified -> biometrics.enable(verification.dataKey, prompt)
            else -> BiometricEnableResult.IncorrectPin
        }
    }

    /** Turns biometric unlock off. Subsequent unlocks require the PIN. */
    suspend fun disableBiometric() = biometrics.disable()

    /** Drops the data key from memory, which is what auto-lock and an explicit lock both do. */
    fun lock() = session.lock()

    private suspend fun pinWrap(): WrappedDataKey? =
        keyMaterial.read()?.wrapsOf(KeyWrapType.Pin)?.firstOrNull()
}
