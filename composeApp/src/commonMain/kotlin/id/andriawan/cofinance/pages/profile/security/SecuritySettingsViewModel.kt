package id.andriawan.cofinance.pages.profile.security

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.andriawan.cofinance.data.crypto.PhraseExportStatus
import id.andriawan.cofinance.data.crypto.RECOVERY_PHRASE_FILE_NAME
import id.andriawan.cofinance.data.crypto.RecoveryPhraseExporter
import id.andriawan.cofinance.data.crypto.RecoveryPhraseVault
import id.andriawan.cofinance.data.crypto.toRecoveryPhraseExportText
import id.andriawan.cofinance.data.keyring.EncryptionSessionState
import id.andriawan.cofinance.data.lock.AppLock
import id.andriawan.cofinance.data.lock.AutoLockTimeout
import id.andriawan.cofinance.data.lock.BiometricCapability
import id.andriawan.cofinance.data.lock.BiometricEnableResult
import id.andriawan.cofinance.data.lock.BiometricPromptText
import id.andriawan.cofinance.data.lock.PinChangeResult
import id.andriawan.cofinance.data.lock.PinVerification
import id.andriawan.cofinance.pages.lock.PinRules
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A change the user asked for, which is also what the PIN prompt is authorizing. */
sealed interface SecurityIntent {

    /** The first PIN on this device. There is no current PIN to require. */
    data object SetPin : SecurityIntent

    data object ChangePin : SecurityIntent

    data object RemovePin : SecurityIntent

    /** Turning the biometric shortcut on, which [AppLock] refuses without a PIN. */
    data class EnableBiometric(val prompt: BiometricPromptText) : SecurityIntent

    data object DisableBiometric : SecurityIntent

    /** Showing the phrase again, per Decision 10. */
    data object RevealRecoveryPhrase : SecurityIntent

    data class ChangeAutoLock(val timeout: AutoLockTimeout) : SecurityIntent
}

/** Why a security settings action did not go through. */
sealed interface SecuritySettingsError {

    /** The current PIN was wrong. Every entry here is counted, so the numbers are real. */
    data class IncorrectPin(
        val attemptsRemaining: Int,
        val nextAttemptDelay: Duration
    ) : SecuritySettingsError

    /** The escalating delay has not elapsed. Settings are not an unthrottled PIN oracle. */
    data class Throttled(val remaining: Duration) : SecuritySettingsError

    /** Consecutive failures reached the threshold, here as much as on the unlock screen. */
    data object KeyMaterialDestroyed : SecuritySettingsError

    /** No PIN is set, so there is no current PIN to prove. */
    data object PinNotSet : SecuritySettingsError

    /** Setting a first PIN needs the data key, which a locked session does not hold. */
    data object SessionLocked : SecuritySettingsError

    /** The new PIN is not [PinRules.LENGTH] digits. */
    data object PinIncomplete : SecuritySettingsError

    /** The new PIN and its confirmation differ. */
    data object PinConfirmationMismatch : SecuritySettingsError

    /** Biometric unlock was asked for with no PIN set. Per Decision 4 the PIN is the floor. */
    data object BiometricRequiresPin : SecuritySettingsError

    data class BiometricUnavailable(val capability: BiometricCapability) : SecuritySettingsError

    data object BiometricCancelled : SecuritySettingsError

    data object BiometricFailed : SecuritySettingsError

    /** The PIN was right but this device kept no copy of the phrase to show. */
    data object RecoveryPhraseUnavailable : SecuritySettingsError
}

/** What just succeeded, so the section can confirm it. */
enum class SecurityNotice {
    PinSet,
    PinChanged,
    PinRemoved,
    BiometricEnabled,
    BiometricDisabled,
    AutoLockChanged
}

/**
 * The PIN prompt standing in front of a change.
 *
 * It carries the intent it authorizes so that one prompt serves every control, and so that a
 * submitted prompt cannot be applied to a different change than the one the user opened.
 */
@Stable
data class SecurityPinPrompt(
    val intent: SecurityIntent,
    val requiresCurrentPin: Boolean,
    val requiresNewPin: Boolean,
    val currentPin: String = "",
    val newPin: String = "",
    val confirmPin: String = "",
    val error: SecuritySettingsError? = null
) {
    val canSubmit: Boolean
        get() = (!requiresCurrentPin || PinRules.isComplete(currentPin)) &&
            (!requiresNewPin || (newPin.isNotEmpty() && confirmPin.isNotEmpty()))
}

@Stable
data class SecuritySettingsUiState(
    /** The whole section is absent until encryption setup has completed; there is no key to lock. */
    val isSetUp: Boolean = false,
    val isPinSet: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val biometricCapability: BiometricCapability = BiometricCapability.Unavailable,
    val autoLockTimeout: AutoLockTimeout = AutoLockTimeout.Default,
    val autoLockOptions: List<AutoLockTimeout> = AutoLockTimeout.options,
    val prompt: SecurityPinPrompt? = null,
    val isBusy: Boolean = false,
    /** The six groups, and only ever after a PIN entered for this request. */
    val revealedPhrase: List<String> = emptyList(),
    /** What the last copy or save of the revealed phrase did. Cleared when the phrase is hidden. */
    val exportStatus: PhraseExportStatus? = null,
    val notice: SecurityNotice? = null,
    /** A refusal that has no prompt behind it, such as biometric asked for with no PIN. */
    val error: SecuritySettingsError? = null
)

sealed interface SecuritySettingsUiEvent {
    data class Requested(val intent: SecurityIntent) : SecuritySettingsUiEvent
    data class CurrentPinChanged(val value: String) : SecuritySettingsUiEvent
    data class NewPinChanged(val value: String) : SecuritySettingsUiEvent
    data class ConfirmPinChanged(val value: String) : SecuritySettingsUiEvent
    data object PromptSubmitted : SecuritySettingsUiEvent
    data object PromptDismissed : SecuritySettingsUiEvent
    data object RecoveryPhraseDismissed : SecuritySettingsUiEvent
    data object CopyRecoveryPhrase : SecuritySettingsUiEvent
    data object DownloadRecoveryPhrase : SecuritySettingsUiEvent
    data object NoticeDismissed : SecuritySettingsUiEvent
}

/**
 * The security section of the profile page: PIN, biometric, auto-lock, and the recovery phrase.
 *
 * Two rules shape everything here, and both are requirements rather than preferences:
 *
 * - **The current PIN authorizes every change.** Changing or removing the PIN, turning the biometric
 *   shortcut on or off, changing the auto-lock timeout, and re-displaying the phrase all open the
 *   same prompt first. Turning biometric *off* is included even though it only tightens the lock,
 *   because the requirement is written about changing lock settings rather than about weakening
 *   them, and a device handed over while unlocked should not be able to reconfigure the lock at all.
 * - **Being unlocked proves nothing.** Every one of those goes through [AppLock.verifyPin], which
 *   derives the key from the PIN just entered and counts the attempt. Decision 10 says so for the
 *   phrase specifically; applying it uniformly means there is no settings action that an unlocked
 *   session performs on its own.
 *
 * The failed-attempt threshold is deliberately absent from the state: Decision 9 fixes it, and this
 * screen offers no control over it.
 */
@Stable
class SecuritySettingsViewModel(
    private val appLock: AppLock,
    private val recoveryPhraseVault: RecoveryPhraseVault,
    private val recoveryPhraseExporter: RecoveryPhraseExporter
) : ViewModel() {

    private val _uiState = MutableStateFlow(SecuritySettingsUiState())
    val uiState = _uiState.asStateFlow()

    fun onEvent(event: SecuritySettingsUiEvent) {
        when (event) {
            is SecuritySettingsUiEvent.Requested -> viewModelScope.launch { request(event.intent) }
            is SecuritySettingsUiEvent.CurrentPinChanged -> updatePrompt {
                it.copy(currentPin = PinRules.sanitize(event.value), error = null)
            }

            is SecuritySettingsUiEvent.NewPinChanged -> updatePrompt {
                it.copy(newPin = PinRules.sanitize(event.value), error = null)
            }

            is SecuritySettingsUiEvent.ConfirmPinChanged -> updatePrompt {
                it.copy(confirmPin = PinRules.sanitize(event.value), error = null)
            }

            SecuritySettingsUiEvent.PromptSubmitted -> viewModelScope.launch { submitPrompt() }
            SecuritySettingsUiEvent.PromptDismissed -> dismissPrompt()
            SecuritySettingsUiEvent.RecoveryPhraseDismissed ->
                _uiState.update { it.copy(revealedPhrase = emptyList(), exportStatus = null) }

            SecuritySettingsUiEvent.CopyRecoveryPhrase ->
                viewModelScope.launch { copyRevealedPhrase() }

            SecuritySettingsUiEvent.DownloadRecoveryPhrase ->
                viewModelScope.launch { downloadRevealedPhrase() }

            SecuritySettingsUiEvent.NoticeDismissed -> _uiState.update { it.copy(notice = null) }
        }
    }

    /** Reads what this device currently has configured. Safe to call again. */
    fun start() {
        viewModelScope.launch { refresh() }
    }

    suspend fun refresh() {
        val isSetUp = appLock.state.value != EncryptionSessionState.SetupIncomplete
        _uiState.update {
            it.copy(
                isSetUp = isSetUp,
                isPinSet = appLock.isPinSet(),
                isBiometricEnabled = appLock.isBiometricEnabled(),
                biometricCapability = appLock.biometricCapability(),
                autoLockTimeout = appLock.autoLockSettings.timeout.value
            )
        }
    }

    /**
     * Opens the PIN prompt for [intent], or refuses it outright when it cannot be authorized.
     *
     * Setting the first PIN is the one intent with no current PIN to require, since there is none;
     * it requires an unlocked session instead, which [AppLock.setPin] enforces.
     */
    suspend fun request(intent: SecurityIntent) {
        val state = _uiState.value

        if (intent is SecurityIntent.EnableBiometric && !state.isPinSet) {
            // Said rather than shown as a disabled switch. A control that does nothing when tapped
            // teaches the user that the feature is broken; this teaches them what to do first.
            _uiState.update { it.copy(error = SecuritySettingsError.BiometricRequiresPin) }
            return
        }

        if (intent is SecurityIntent.ChangeAutoLock && !state.isPinSet) {
            // No PIN exists to require. The timeout still matters — it decides when the key is
            // dropped — so the choice is honored rather than blocked behind a PIN the user has not
            // set yet.
            appLock.autoLockSettings.setTimeout(intent.timeout)
            _uiState.update {
                it.copy(autoLockTimeout = intent.timeout, notice = SecurityNotice.AutoLockChanged)
            }
            return
        }

        _uiState.update {
            it.copy(
                error = null,
                notice = null,
                revealedPhrase = emptyList(),
                prompt = SecurityPinPrompt(
                    intent = intent,
                    requiresCurrentPin = intent !is SecurityIntent.SetPin || state.isPinSet,
                    requiresNewPin = intent is SecurityIntent.SetPin ||
                        intent is SecurityIntent.ChangePin
                )
            )
        }
    }

    /** Applies the open prompt's intent, given the PIN entered into it. */
    suspend fun submitPrompt() {
        val state = _uiState.value
        val prompt = state.prompt ?: return
        if (state.isBusy || !prompt.canSubmit) return

        if (prompt.requiresNewPin) {
            // These two are checked here rather than by the lock, and nothing is attempted against
            // the lock when they fail, so the entered current PIN is left in place: retyping it
            // after a typo in the *new* PIN would be a penalty for the wrong mistake.
            if (!PinRules.isComplete(prompt.newPin)) {
                return failPrompt(SecuritySettingsError.PinIncomplete, clearCurrentPin = false)
            }
            if (prompt.newPin != prompt.confirmPin) {
                return failPrompt(
                    SecuritySettingsError.PinConfirmationMismatch,
                    clearCurrentPin = false
                )
            }
        }

        _uiState.update { it.copy(isBusy = true, prompt = prompt.copy(error = null)) }

        when (val intent = prompt.intent) {
            SecurityIntent.SetPin, SecurityIntent.ChangePin -> applyPinChange(prompt, intent)
            SecurityIntent.RemovePin -> applyRemovePin(prompt)
            is SecurityIntent.EnableBiometric -> applyEnableBiometric(prompt, intent)
            SecurityIntent.DisableBiometric -> applyDisableBiometric(prompt)
            SecurityIntent.RevealRecoveryPhrase -> applyRevealPhrase(prompt)
            is SecurityIntent.ChangeAutoLock -> applyAutoLock(prompt, intent)
        }
    }

    private suspend fun applyPinChange(prompt: SecurityPinPrompt, intent: SecurityIntent) {
        val result = appLock.setPin(
            newPin = prompt.newPin,
            currentPin = prompt.currentPin.takeIf { prompt.requiresCurrentPin }
        )
        when (result) {
            PinChangeResult.Changed -> succeed(
                if (intent is SecurityIntent.SetPin) SecurityNotice.PinSet
                else SecurityNotice.PinChanged
            )

            is PinChangeResult.CurrentPinRejected -> failPrompt(errorOf(result.verification))
            PinChangeResult.SessionLocked -> failPrompt(SecuritySettingsError.SessionLocked)
        }
    }

    private suspend fun applyRemovePin(prompt: SecurityPinPrompt) {
        when (val result = appLock.removePin(prompt.currentPin)) {
            PinChangeResult.Changed -> succeed(SecurityNotice.PinRemoved)
            is PinChangeResult.CurrentPinRejected -> failPrompt(errorOf(result.verification))
            PinChangeResult.SessionLocked -> failPrompt(SecuritySettingsError.SessionLocked)
        }
    }

    private suspend fun applyEnableBiometric(
        prompt: SecurityPinPrompt,
        intent: SecurityIntent.EnableBiometric
    ) {
        when (val result = appLock.enableBiometric(prompt.currentPin, intent.prompt)) {
            BiometricEnableResult.Enabled -> succeed(SecurityNotice.BiometricEnabled)
            BiometricEnableResult.PinRequired ->
                failPrompt(SecuritySettingsError.BiometricRequiresPin)

            BiometricEnableResult.IncorrectPin -> failPrompt(currentPinError())
            BiometricEnableResult.Cancelled -> failPrompt(SecuritySettingsError.BiometricCancelled)
            is BiometricEnableResult.Unavailable ->
                failPrompt(SecuritySettingsError.BiometricUnavailable(result.capability))

            is BiometricEnableResult.Failed -> failPrompt(SecuritySettingsError.BiometricFailed)
        }
    }

    private suspend fun applyDisableBiometric(prompt: SecurityPinPrompt) {
        when (val verification = appLock.verifyPin(prompt.currentPin)) {
            is PinVerification.Verified -> {
                appLock.disableBiometric()
                succeed(SecurityNotice.BiometricDisabled)
            }

            else -> failPrompt(errorOf(verification))
        }
    }

    private suspend fun applyAutoLock(
        prompt: SecurityPinPrompt,
        intent: SecurityIntent.ChangeAutoLock
    ) {
        when (val verification = appLock.verifyPin(prompt.currentPin)) {
            is PinVerification.Verified -> {
                appLock.autoLockSettings.setTimeout(intent.timeout)
                succeed(SecurityNotice.AutoLockChanged)
            }

            else -> failPrompt(errorOf(verification))
        }
    }

    /**
     * Shows the phrase, and only against a PIN entered for this request.
     *
     * The session's own data key is never used here even when one is held, which is the whole of
     * Decision 10: an unlocked phone handed to someone else must not surrender the phrase.
     */
    private suspend fun applyRevealPhrase(prompt: SecurityPinPrompt) {
        when (val verification = appLock.verifyPin(prompt.currentPin)) {
            is PinVerification.Verified -> {
                val phrase = recoveryPhraseVault.read(verification.dataKey)
                if (phrase == null) {
                    failPrompt(SecuritySettingsError.RecoveryPhraseUnavailable)
                } else {
                    _uiState.update {
                        it.copy(isBusy = false, prompt = null, revealedPhrase = phrase.groups)
                    }
                    refresh()
                }
            }

            else -> failPrompt(errorOf(verification))
        }
    }

    private suspend fun succeed(notice: SecurityNotice) {
        _uiState.update { it.copy(isBusy = false, prompt = null, notice = notice) }
        refresh()
    }

    /**
     * Reports [error] and keeps the prompt open.
     *
     * [clearCurrentPin] is true for everything the lock rejected, so a wrong PIN is not left on
     * screen to be resubmitted, and false for the checks made here before the lock was consulted.
     */
    private fun failPrompt(error: SecuritySettingsError, clearCurrentPin: Boolean = true) {
        _uiState.update { state ->
            state.copy(
                isBusy = false,
                prompt = state.prompt?.copy(
                    currentPin = if (clearCurrentPin) "" else state.prompt.currentPin,
                    error = error
                )
            )
        }
    }

    private fun dismissPrompt() {
        // Dismissing shows nothing and changes nothing, which is what the specification requires of
        // a dismissed re-display prompt and is the right answer for every other intent too.
        _uiState.update { it.copy(prompt = null, revealedPhrase = emptyList()) }
    }

    /**
     * Copies the phrase that is currently on screen.
     *
     * Only the revealed groups are reachable here — the vault is not read again — so a copy is
     * possible exactly while the dialog the PIN opened is still up.
     */
    suspend fun copyRevealedPhrase() {
        val groups = _uiState.value.revealedPhrase
        if (groups.isEmpty()) return
        val copied = try {
            recoveryPhraseExporter.copyToClipboard(groups.toRecoveryPhraseExportText())
        } catch (cause: Throwable) {
            if (cause is CancellationException) throw cause
            false
        }
        _uiState.update {
            it.copy(
                exportStatus =
                    if (copied) PhraseExportStatus.Copied else PhraseExportStatus.CopyFailed
            )
        }
    }

    /** Writes the revealed phrase to a file. A cancelled picker reports a failure, not a file. */
    suspend fun downloadRevealedPhrase() {
        val groups = _uiState.value.revealedPhrase
        if (groups.isEmpty()) return
        val location = try {
            recoveryPhraseExporter.saveToFile(
                RECOVERY_PHRASE_FILE_NAME,
                groups.toRecoveryPhraseExportText()
            )
        } catch (cause: Throwable) {
            if (cause is CancellationException) throw cause
            null
        }
        _uiState.update {
            it.copy(
                exportStatus = location?.let(PhraseExportStatus::Saved)
                    ?: PhraseExportStatus.SaveFailed
            )
        }
    }

    private fun updatePrompt(transform: (SecurityPinPrompt) -> SecurityPinPrompt) {
        _uiState.update { state ->
            state.copy(prompt = state.prompt?.let(transform), error = null)
        }
    }

    /**
     * The error for a rejection reported without a verification attached.
     *
     * [BiometricEnableResult.IncorrectPin] flattens the verification it came from, so the attempt
     * count is not available to report. Saying only that the PIN was wrong is accurate; inventing a
     * count would not be.
     */
    private fun currentPinError(): SecuritySettingsError =
        SecuritySettingsError.IncorrectPin(attemptsRemaining = -1, nextAttemptDelay = Duration.ZERO)

    private fun errorOf(verification: PinVerification): SecuritySettingsError = when (verification) {
        is PinVerification.Verified -> SecuritySettingsError.PinIncomplete
        is PinVerification.Incorrect -> SecuritySettingsError.IncorrectPin(
            attemptsRemaining = verification.attemptsRemaining,
            nextAttemptDelay = verification.nextAttemptDelay
        )

        is PinVerification.Throttled -> SecuritySettingsError.Throttled(verification.remaining)
        PinVerification.KeyMaterialDestroyed -> SecuritySettingsError.KeyMaterialDestroyed
        PinVerification.PinNotSet -> SecuritySettingsError.PinNotSet
    }
}
