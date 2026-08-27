package id.andriawan.cofinance.pages.profile.security

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cofinance.composeapp.generated.resources.Res
import cofinance.composeapp.generated.resources.action_change_pin
import cofinance.composeapp.generated.resources.action_confirm
import cofinance.composeapp.generated.resources.action_copy_recovery_phrase
import cofinance.composeapp.generated.resources.action_download_recovery_phrase
import cofinance.composeapp.generated.resources.action_remove_pin
import cofinance.composeapp.generated.resources.action_set_pin
import cofinance.composeapp.generated.resources.action_view_recovery_phrase
import cofinance.composeapp.generated.resources.biometric_prompt_enable_subtitle
import cofinance.composeapp.generated.resources.biometric_prompt_enable_title
import cofinance.composeapp.generated.resources.biometric_prompt_negative
import cofinance.composeapp.generated.resources.description_auto_lock
import cofinance.composeapp.generated.resources.description_biometric_unlock
import cofinance.composeapp.generated.resources.description_recovery_phrase_reveal
import cofinance.composeapp.generated.resources.error_biometric_cancelled
import cofinance.composeapp.generated.resources.error_biometric_failed
import cofinance.composeapp.generated.resources.error_biometric_no_hardware
import cofinance.composeapp.generated.resources.error_biometric_not_enrolled
import cofinance.composeapp.generated.resources.error_biometric_requires_pin
import cofinance.composeapp.generated.resources.error_biometric_unavailable
import cofinance.composeapp.generated.resources.error_current_pin_incorrect
import cofinance.composeapp.generated.resources.error_pin_confirmation_mismatch
import cofinance.composeapp.generated.resources.error_pin_incorrect
import cofinance.composeapp.generated.resources.error_pin_incorrect_with_delay
import cofinance.composeapp.generated.resources.error_pin_key_material_destroyed
import cofinance.composeapp.generated.resources.error_pin_length
import cofinance.composeapp.generated.resources.error_pin_not_set
import cofinance.composeapp.generated.resources.error_pin_wait
import cofinance.composeapp.generated.resources.error_recovery_phrase_unavailable
import cofinance.composeapp.generated.resources.error_session_locked
import cofinance.composeapp.generated.resources.label_app_pin
import cofinance.composeapp.generated.resources.label_auto_lock
import cofinance.composeapp.generated.resources.label_biometric_unlock
import cofinance.composeapp.generated.resources.label_cancel
import cofinance.composeapp.generated.resources.label_close
import cofinance.composeapp.generated.resources.label_confirm_new_pin
import cofinance.composeapp.generated.resources.label_current_pin
import cofinance.composeapp.generated.resources.label_new_pin
import cofinance.composeapp.generated.resources.message_auto_lock_changed
import cofinance.composeapp.generated.resources.message_biometric_disabled
import cofinance.composeapp.generated.resources.message_biometric_enabled
import cofinance.composeapp.generated.resources.message_pin_changed
import cofinance.composeapp.generated.resources.message_pin_removed
import cofinance.composeapp.generated.resources.message_pin_set
import cofinance.composeapp.generated.resources.message_recovery_phrase_copied
import cofinance.composeapp.generated.resources.message_recovery_phrase_copy_failed
import cofinance.composeapp.generated.resources.message_recovery_phrase_save_failed
import cofinance.composeapp.generated.resources.message_recovery_phrase_saved
import cofinance.composeapp.generated.resources.note_local_data_not_encrypted
import cofinance.composeapp.generated.resources.note_recovery_phrase_export
import cofinance.composeapp.generated.resources.title_auto_lock
import cofinance.composeapp.generated.resources.title_current_pin_required
import cofinance.composeapp.generated.resources.title_recovery_phrase
import cofinance.composeapp.generated.resources.title_security_settings
import cofinance.composeapp.generated.resources.title_set_pin
import cofinance.composeapp.generated.resources.warning_recovery_phrase_loss
import id.andriawan.cofinance.components.PrimaryButton
import id.andriawan.cofinance.components.SecondaryButton
import id.andriawan.cofinance.data.crypto.PhraseExportStatus
import id.andriawan.cofinance.data.lock.AutoLockTimeout
import id.andriawan.cofinance.data.lock.BiometricCapability
import id.andriawan.cofinance.data.lock.BiometricPromptText
import id.andriawan.cofinance.pages.lock.autoLockTimeoutLabel
import id.andriawan.cofinance.pages.lock.lockDelayText
import id.andriawan.cofinance.theme.CofinanceTheme
import id.andriawan.cofinance.utils.Dimensions
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Duration

/**
 * The security section of the profile page.
 *
 * It is absent, rather than disabled, for a user who has not completed encryption setup: there is no
 * data key to protect, and offering a PIN that guards nothing would be theatre.
 */
@Composable
fun SecuritySettingsSection(
    modifier: Modifier = Modifier,
    viewModel: SecuritySettingsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.start() }

    if (!uiState.isSetUp) return

    val enablePrompt = BiometricPromptText(
        title = stringResource(Res.string.biometric_prompt_enable_title),
        subtitle = stringResource(Res.string.biometric_prompt_enable_subtitle),
        negativeButtonLabel = stringResource(Res.string.biometric_prompt_negative)
    )

    SecuritySettingsContent(
        modifier = modifier,
        uiState = uiState,
        biometricEnablePrompt = enablePrompt,
        onEvent = viewModel::onEvent
    )
}

@Composable
fun SecuritySettingsContent(
    uiState: SecuritySettingsUiState,
    biometricEnablePrompt: BiometricPromptText,
    onEvent: (SecuritySettingsUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAutoLockPicker by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(Dimensions.SIZE_16),
            verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_16)
        ) {
            Text(
                text = stringResource(Res.string.title_security_settings),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )

            SettingRow(title = stringResource(Res.string.label_app_pin)) {
                Row(horizontalArrangement = Arrangement.spacedBy(Dimensions.SIZE_8)) {
                    TextButton(
                        onClick = {
                            onEvent(
                                SecuritySettingsUiEvent.Requested(
                                    if (uiState.isPinSet) SecurityIntent.ChangePin
                                    else SecurityIntent.SetPin
                                )
                            )
                        }
                    ) {
                        Text(
                            text = stringResource(
                                if (uiState.isPinSet) Res.string.action_change_pin
                                else Res.string.action_set_pin
                            ),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    if (uiState.isPinSet) {
                        TextButton(
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            onClick = {
                                onEvent(
                                    SecuritySettingsUiEvent.Requested(SecurityIntent.RemovePin)
                                )
                            }
                        ) {
                            Text(
                                text = stringResource(Res.string.action_remove_pin),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }

            SettingRow(
                title = stringResource(Res.string.label_biometric_unlock),
                subtitle = stringResource(Res.string.description_biometric_unlock)
            ) {
                Switch(
                    checked = uiState.isBiometricEnabled,
                    // Never disabled: turning it on without a PIN has to say why, and a dead switch
                    // says nothing. The refusal is reported by the view model instead.
                    onCheckedChange = { enable ->
                        onEvent(
                            SecuritySettingsUiEvent.Requested(
                                if (enable) SecurityIntent.EnableBiometric(biometricEnablePrompt)
                                else SecurityIntent.DisableBiometric
                            )
                        )
                    }
                )
            }

            SettingRow(
                modifier = Modifier
                    .clip(RoundedCornerShape(Dimensions.SIZE_12))
                    .clickable { showAutoLockPicker = true }
                    .semantics { role = Role.Button },
                title = stringResource(Res.string.label_auto_lock),
                subtitle = stringResource(Res.string.description_auto_lock)
            ) {
                Text(
                    text = autoLockTimeoutLabel(uiState.autoLockTimeout),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            SettingRow(
                title = stringResource(Res.string.title_recovery_phrase),
                subtitle = stringResource(Res.string.description_recovery_phrase_reveal)
            ) {
                TextButton(
                    onClick = {
                        onEvent(
                            SecuritySettingsUiEvent.Requested(SecurityIntent.RevealRecoveryPhrase)
                        )
                    }
                ) {
                    Text(
                        text = stringResource(Res.string.action_view_recovery_phrase),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            uiState.error?.let { error ->
                Text(
                    text = securityErrorText(error),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            uiState.notice?.let { notice ->
                Text(
                    text = securityNoticeText(notice),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    if (showAutoLockPicker) {
        AutoLockPickerDialog(
            selected = uiState.autoLockTimeout,
            options = uiState.autoLockOptions,
            onSelected = { timeout ->
                showAutoLockPicker = false
                onEvent(SecuritySettingsUiEvent.Requested(SecurityIntent.ChangeAutoLock(timeout)))
            },
            onDismiss = { showAutoLockPicker = false }
        )
    }

    uiState.prompt?.let { prompt ->
        SecurityPinPromptDialog(
            prompt = prompt,
            isBusy = uiState.isBusy,
            onEvent = onEvent
        )
    }

    if (uiState.revealedPhrase.isNotEmpty()) {
        RevealedPhraseDialog(
            words = uiState.revealedPhrase,
            exportStatus = uiState.exportStatus,
            onEvent = onEvent,
            onDismiss = { onEvent(SecuritySettingsUiEvent.RecoveryPhraseDismissed) }
        )
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String = "",
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = Dimensions.SIZE_4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimensions.SIZE_12)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )

            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        trailing()
    }
}

@Composable
private fun AutoLockPickerDialog(
    selected: AutoLockTimeout,
    options: List<AutoLockTimeout>,
    onSelected: (AutoLockTimeout) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(Dimensions.SIZE_24),
                verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_12)
            ) {
                Text(
                    text = stringResource(Res.string.title_auto_lock),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Dimensions.SIZE_12))
                            .clickable { onSelected(option) }
                            .padding(Dimensions.SIZE_8),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimensions.SIZE_8)
                    ) {
                        RadioButton(selected = option == selected, onClick = { onSelected(option) })
                        Text(
                            text = autoLockTimeoutLabel(option),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                SecondaryButton(modifier = Modifier.fillMaxWidth(), onClick = onDismiss) {
                    Text(
                        text = stringResource(Res.string.label_cancel),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

/**
 * The one prompt every change passes through.
 *
 * It cannot be bypassed by an unlocked session: the view model derives the key from what is typed
 * here rather than reading the one already in memory.
 */
@Composable
private fun SecurityPinPromptDialog(
    prompt: SecurityPinPrompt,
    isBusy: Boolean,
    onEvent: (SecuritySettingsUiEvent) -> Unit
) {
    Dialog(onDismissRequest = { onEvent(SecuritySettingsUiEvent.PromptDismissed) }) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(Dimensions.SIZE_24)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_12)
            ) {
                Text(
                    text = stringResource(
                        if (prompt.requiresCurrentPin) Res.string.title_current_pin_required
                        else Res.string.title_set_pin
                    ),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (prompt.requiresCurrentPin) {
                    PinField(
                        value = prompt.currentPin,
                        label = stringResource(Res.string.label_current_pin),
                        isError = prompt.error != null,
                        enabled = !isBusy,
                        onValueChange = {
                            onEvent(SecuritySettingsUiEvent.CurrentPinChanged(it))
                        }
                    )
                }

                if (prompt.requiresNewPin) {
                    PinField(
                        value = prompt.newPin,
                        label = stringResource(Res.string.label_new_pin),
                        isError = false,
                        enabled = !isBusy,
                        onValueChange = { onEvent(SecuritySettingsUiEvent.NewPinChanged(it)) }
                    )

                    PinField(
                        value = prompt.confirmPin,
                        label = stringResource(Res.string.label_confirm_new_pin),
                        isError = false,
                        enabled = !isBusy,
                        onValueChange = { onEvent(SecuritySettingsUiEvent.ConfirmPinChanged(it)) }
                    )
                }

                prompt.error?.let { error ->
                    Text(
                        text = securityErrorText(error),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                PrimaryButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = prompt.canSubmit && !isBusy,
                    onClick = { onEvent(SecuritySettingsUiEvent.PromptSubmitted) }
                ) {
                    Text(
                        text = stringResource(Res.string.action_confirm),
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                SecondaryButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onEvent(SecuritySettingsUiEvent.PromptDismissed) }
                ) {
                    Text(
                        text = stringResource(Res.string.label_cancel),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun RevealedPhraseDialog(
    words: List<String>,
    exportStatus: PhraseExportStatus?,
    onEvent: (SecuritySettingsUiEvent) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(Dimensions.SIZE_24)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_12)
            ) {
                Text(
                    text = stringResource(Res.string.title_recovery_phrase),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Dimensions.SIZE_12))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(Dimensions.SIZE_16),
                    verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_8)
                ) {
                    words.forEachIndexed { index, word ->
                        Text(
                            text = "${index + 1}. $word",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(Dimensions.SIZE_8)) {
                    SecondaryButton(
                        modifier = Modifier.weight(1f),
                        onClick = { onEvent(SecuritySettingsUiEvent.CopyRecoveryPhrase) }
                    ) {
                        Text(
                            text = stringResource(Res.string.action_copy_recovery_phrase),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    SecondaryButton(
                        modifier = Modifier.weight(1f),
                        onClick = { onEvent(SecuritySettingsUiEvent.DownloadRecoveryPhrase) }
                    ) {
                        Text(
                            text = stringResource(Res.string.action_download_recovery_phrase),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                exportStatus?.let { status ->
                    Text(
                        text = when (status) {
                            is PhraseExportStatus.Copied ->
                                stringResource(Res.string.message_recovery_phrase_copied)

                            is PhraseExportStatus.Saved -> stringResource(
                                Res.string.message_recovery_phrase_saved,
                                status.location
                            )

                            is PhraseExportStatus.CopyFailed ->
                                stringResource(Res.string.message_recovery_phrase_copy_failed)

                            is PhraseExportStatus.SaveFailed ->
                                stringResource(Res.string.message_recovery_phrase_save_failed)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = when (status) {
                            is PhraseExportStatus.CopyFailed, is PhraseExportStatus.SaveFailed ->
                                MaterialTheme.colorScheme.error

                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                Text(
                    text = stringResource(Res.string.note_recovery_phrase_export),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = stringResource(Res.string.warning_recovery_phrase_loss),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )

                Text(
                    text = stringResource(Res.string.note_local_data_not_encrypted),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                PrimaryButton(modifier = Modifier.fillMaxWidth(), onClick = onDismiss) {
                    Text(
                        text = stringResource(Res.string.label_close),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun PinField(
    value: String,
    label: String,
    isError: Boolean,
    enabled: Boolean,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = label) },
        singleLine = true,
        enabled = enabled,
        isError = isError,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.NumberPassword,
            imeAction = ImeAction.Next
        )
    )
}

@Composable
private fun securityErrorText(error: SecuritySettingsError): String = when (error) {
    is SecuritySettingsError.IncorrectPin -> when {
        error.attemptsRemaining < 0 -> stringResource(Res.string.error_current_pin_incorrect)
        error.nextAttemptDelay > Duration.ZERO -> stringResource(
            Res.string.error_pin_incorrect_with_delay,
            error.attemptsRemaining,
            lockDelayText(error.nextAttemptDelay)
        )

        else -> stringResource(Res.string.error_pin_incorrect, error.attemptsRemaining)
    }

    is SecuritySettingsError.Throttled ->
        stringResource(Res.string.error_pin_wait, lockDelayText(error.remaining))

    SecuritySettingsError.KeyMaterialDestroyed ->
        stringResource(Res.string.error_pin_key_material_destroyed)

    SecuritySettingsError.PinNotSet -> stringResource(Res.string.error_pin_not_set)
    SecuritySettingsError.SessionLocked -> stringResource(Res.string.error_session_locked)
    SecuritySettingsError.PinIncomplete -> stringResource(Res.string.error_pin_length)
    SecuritySettingsError.PinConfirmationMismatch ->
        stringResource(Res.string.error_pin_confirmation_mismatch)

    SecuritySettingsError.BiometricRequiresPin ->
        stringResource(Res.string.error_biometric_requires_pin)

    is SecuritySettingsError.BiometricUnavailable -> when (error.capability) {
        BiometricCapability.NotEnrolled -> stringResource(Res.string.error_biometric_not_enrolled)
        BiometricCapability.NoHardware -> stringResource(Res.string.error_biometric_no_hardware)
        else -> stringResource(Res.string.error_biometric_unavailable)
    }

    SecuritySettingsError.BiometricCancelled -> stringResource(Res.string.error_biometric_cancelled)
    SecuritySettingsError.BiometricFailed -> stringResource(Res.string.error_biometric_failed)
    SecuritySettingsError.RecoveryPhraseUnavailable ->
        stringResource(Res.string.error_recovery_phrase_unavailable)
}

@Composable
private fun securityNoticeText(notice: SecurityNotice): String = when (notice) {
    SecurityNotice.PinSet -> stringResource(Res.string.message_pin_set)
    SecurityNotice.PinChanged -> stringResource(Res.string.message_pin_changed)
    SecurityNotice.PinRemoved -> stringResource(Res.string.message_pin_removed)
    SecurityNotice.BiometricEnabled -> stringResource(Res.string.message_biometric_enabled)
    SecurityNotice.BiometricDisabled -> stringResource(Res.string.message_biometric_disabled)
    SecurityNotice.AutoLockChanged -> stringResource(Res.string.message_auto_lock_changed)
}

@Preview
@Composable
private fun SecuritySettingsPreview() {
    CofinanceTheme {
        Surface {
            SecuritySettingsContent(
                uiState = SecuritySettingsUiState(
                    isSetUp = true,
                    isPinSet = true,
                    isBiometricEnabled = false,
                    biometricCapability = BiometricCapability.Available
                ),
                biometricEnablePrompt = BiometricPromptText(title = "", negativeButtonLabel = ""),
                onEvent = { }
            )
        }
    }
}
