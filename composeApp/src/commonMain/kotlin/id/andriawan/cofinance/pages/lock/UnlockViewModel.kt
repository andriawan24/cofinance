package id.andriawan.cofinance.pages.lock

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.andriawan.cofinance.data.lock.AppLock
import id.andriawan.cofinance.data.lock.BiometricCapability
import id.andriawan.cofinance.data.lock.BiometricPromptText
import id.andriawan.cofinance.data.lock.BiometricUnlockResult
import id.andriawan.cofinance.data.lock.PinFallbackReason
import id.andriawan.cofinance.data.lock.PinUnlockResult
import kotlin.time.Duration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * What the unlock screen tells the user after an attempt.
 *
 * The two throttling cases are separate because they answer different questions. [IncorrectPin]
 * answers "was that my PIN?" and has to carry both how many attempts are left and how long the next
 * one waits, since a user who is told neither will read a silent five-minute delay as a broken app.
 * [Throttled] answers "why did nothing happen?" for an attempt that was refused before any
 * derivation was tried.
 */
sealed interface UnlockFeedback {

    /** The PIN was wrong. [nextAttemptDelay] is zero while attempts are still free. */
    data class IncorrectPin(
        val attemptsRemaining: Int,
        val nextAttemptDelay: Duration
    ) : UnlockFeedback

    /** The escalating delay has not elapsed. Nothing was tried, and nothing was counted. */
    data class Throttled(val remaining: Duration) : UnlockFeedback

    /** This device holds no PIN wrap, so there is nothing a PIN could open here. */
    data object PinNotSet : UnlockFeedback

    /** The biometric path handed the user back to the PIN, and why. */
    data class BiometricFellBack(val reason: PinFallbackReason) : UnlockFeedback
}

@Stable
data class UnlockUiState(
    val pin: String = "",
    val isBusy: Boolean = false,
    /** Whether the biometric shortcut is on and usable right now. */
    val isBiometricOffered: Boolean = false,
    val feedback: UnlockFeedback? = null,
    val isUnlocked: Boolean = false,
    /**
     * Ten consecutive failures destroyed the local key material.
     *
     * This is not an error to sit on. The recovery-phrase wrap is untouched and lives in the
     * account rather than on the device, so the data is still there; the screen routes to restore
     * rather than reporting a loss.
     */
    val requiresRecoveryPhrase: Boolean = false
) {
    val canSubmit: Boolean
        get() = PinRules.isComplete(pin) && !isBusy && !isUnlocked && !requiresRecoveryPhrase
}

sealed interface UnlockUiEvent {
    data class PinChanged(val value: String) : UnlockUiEvent
    data object Submit : UnlockUiEvent
    data class UseBiometric(val prompt: BiometricPromptText) : UnlockUiEvent
}

/**
 * The unlock screen: the only way back to finance data once the data key has left memory.
 *
 * Everything security-relevant is decided by [AppLock] — whether the PIN opens the wrap, whether
 * this attempt is throttled, whether the tenth failure has arrived — and this class exists to make
 * those outcomes legible. That division matters: a screen that computed its own attempt count, or
 * that treated a throttled attempt as a wrong PIN, would be reporting something other than what the
 * lock did.
 *
 * The operations are `suspend` and the events launch them, so a test can drive an unlock without a
 * main dispatcher.
 */
@Stable
class UnlockViewModel(private val appLock: AppLock) : ViewModel() {

    private val _uiState = MutableStateFlow(UnlockUiState())
    val uiState = _uiState.asStateFlow()

    fun onEvent(event: UnlockUiEvent) {
        when (event) {
            is UnlockUiEvent.PinChanged -> onPinChanged(event.value)
            is UnlockUiEvent.Submit -> viewModelScope.launch { submit() }
            is UnlockUiEvent.UseBiometric -> viewModelScope.launch { unlockWithBiometric(event.prompt) }
        }
    }

    /** Reads what this device can offer. Safe to call again. */
    fun start() {
        viewModelScope.launch { prepare() }
    }

    suspend fun prepare() {
        val biometricOffered = appLock.isBiometricEnabled() &&
            appLock.biometricCapability() == BiometricCapability.Available
        val pinSet = appLock.isPinSet()
        _uiState.update {
            it.copy(
                isBiometricOffered = biometricOffered,
                // Said up front rather than after a submission: a device with no PIN wrap cannot be
                // opened here at all, and letting the user type six digits first would be a lie.
                feedback = if (pinSet) it.feedback else UnlockFeedback.PinNotSet
            )
        }
    }

    /** Attempts to unlock with the entered PIN. */
    suspend fun submit() {
        val state = _uiState.value
        if (!state.canSubmit) return
        _uiState.update { it.copy(isBusy = true, feedback = null) }

        when (val result = appLock.unlockWithPin(state.pin)) {
            PinUnlockResult.Unlocked -> _uiState.update {
                it.copy(pin = "", isBusy = false, isUnlocked = true, feedback = null)
            }

            is PinUnlockResult.Incorrect -> _uiState.update {
                it.copy(
                    pin = "",
                    isBusy = false,
                    feedback = UnlockFeedback.IncorrectPin(
                        attemptsRemaining = result.attemptsRemaining,
                        nextAttemptDelay = result.nextAttemptDelay
                    )
                )
            }

            is PinUnlockResult.Throttled -> _uiState.update {
                it.copy(
                    pin = "",
                    isBusy = false,
                    feedback = UnlockFeedback.Throttled(result.remaining)
                )
            }

            PinUnlockResult.KeyMaterialDestroyed -> _uiState.update {
                it.copy(pin = "", isBusy = false, requiresRecoveryPhrase = true, feedback = null)
            }

            PinUnlockResult.PinNotSet -> _uiState.update {
                it.copy(pin = "", isBusy = false, feedback = UnlockFeedback.PinNotSet)
            }
        }
    }

    /**
     * Attempts the biometric shortcut.
     *
     * Every failure lands on PIN entry with an explanation and costs nothing: the failed-attempt
     * counter belongs to the PIN, and a dismissed prompt must not spend an attempt against it.
     */
    suspend fun unlockWithBiometric(prompt: BiometricPromptText) {
        if (_uiState.value.isBusy || _uiState.value.isUnlocked) return
        _uiState.update { it.copy(isBusy = true, feedback = null) }

        when (val result = appLock.unlockWithBiometric(prompt)) {
            is BiometricUnlockResult.Unlocked -> _uiState.update {
                it.copy(pin = "", isBusy = false, isUnlocked = true)
            }

            is BiometricUnlockResult.FellBackToPin -> _uiState.update {
                it.copy(
                    isBusy = false,
                    // An invalidated key is off for good, so the shortcut stops being offered.
                    isBiometricOffered = it.isBiometricOffered &&
                        result.reason != PinFallbackReason.Invalidated &&
                        result.reason != PinFallbackReason.NotEnabled,
                    feedback = UnlockFeedback.BiometricFellBack(result.reason)
                )
            }
        }
    }

    private fun onPinChanged(value: String) {
        _uiState.update { it.copy(pin = PinRules.sanitize(value), feedback = null) }
    }
}
