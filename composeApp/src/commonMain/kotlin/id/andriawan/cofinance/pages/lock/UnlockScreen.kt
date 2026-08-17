package id.andriawan.cofinance.pages.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cofinance.composeapp.generated.resources.Res
import cofinance.composeapp.generated.resources.action_unlock
import cofinance.composeapp.generated.resources.action_use_biometric
import cofinance.composeapp.generated.resources.biometric_prompt_negative
import cofinance.composeapp.generated.resources.biometric_prompt_unlock_subtitle
import cofinance.composeapp.generated.resources.biometric_prompt_unlock_title
import cofinance.composeapp.generated.resources.description_unlock_app
import cofinance.composeapp.generated.resources.error_biometric_fell_back
import cofinance.composeapp.generated.resources.error_biometric_invalidated
import cofinance.composeapp.generated.resources.error_pin_incorrect
import cofinance.composeapp.generated.resources.error_pin_incorrect_with_delay
import cofinance.composeapp.generated.resources.error_pin_not_set
import cofinance.composeapp.generated.resources.error_pin_wait
import cofinance.composeapp.generated.resources.label_pin
import cofinance.composeapp.generated.resources.title_unlock_app
import id.andriawan.cofinance.components.PrimaryButton
import id.andriawan.cofinance.components.SecondaryButton
import id.andriawan.cofinance.data.lock.BiometricPromptText
import id.andriawan.cofinance.data.lock.PinFallbackReason
import id.andriawan.cofinance.theme.CofinanceTheme
import id.andriawan.cofinance.utils.Dimensions
import kotlin.time.Duration
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * The unlock screen, shown only to a session that holds key material without holding the key.
 *
 * It is not reachable by a user who has never completed encryption setup — see [launchLockDecision],
 * which answers `Main` for that state — so a local-only user never meets it.
 *
 * Two of its jobs are easy to get wrong and are therefore explicit here. The throttle is always
 * explained, with the wait and the attempts left, because a silent five-minute delay reads as a
 * broken app. And the tenth failure routes to recovery-phrase restore rather than sitting on an
 * error, because at that point the data is still recoverable and the user needs to be told where.
 */
@Composable
fun UnlockScreen(
    onUnlocked: () -> Unit,
    onRecoveryPhraseRequired: () -> Unit,
    viewModel: UnlockViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val biometricPrompt = BiometricPromptText(
        title = stringResource(Res.string.biometric_prompt_unlock_title),
        subtitle = stringResource(Res.string.biometric_prompt_unlock_subtitle),
        negativeButtonLabel = stringResource(Res.string.biometric_prompt_negative)
    )

    LaunchedEffect(Unit) { viewModel.start() }

    // Prompting as soon as the shortcut is available is the point of having one; the negative button
    // returns the user to the field below, which is already on screen behind the prompt.
    LaunchedEffect(uiState.isBiometricOffered) {
        if (uiState.isBiometricOffered && !uiState.isUnlocked) {
            viewModel.unlockWithBiometric(biometricPrompt)
        }
    }

    LaunchedEffect(uiState.isUnlocked) { if (uiState.isUnlocked) onUnlocked() }

    LaunchedEffect(uiState.requiresRecoveryPhrase) {
        if (uiState.requiresRecoveryPhrase) onRecoveryPhraseRequired()
    }

    Scaffold { contentPadding ->
        UnlockContent(
            modifier = Modifier.padding(contentPadding),
            uiState = uiState,
            onEvent = viewModel::onEvent,
            biometricPrompt = biometricPrompt
        )
    }
}

@Composable
private fun UnlockContent(
    uiState: UnlockUiState,
    onEvent: (UnlockUiEvent) -> Unit,
    biometricPrompt: BiometricPromptText,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Dimensions.SIZE_24),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(Res.string.title_unlock_app),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        )

        Spacer(modifier = Modifier.height(Dimensions.SIZE_8))

        Text(
            text = stringResource(Res.string.description_unlock_app),
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        Spacer(modifier = Modifier.height(Dimensions.SIZE_24))

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = uiState.pin,
            onValueChange = { onEvent(UnlockUiEvent.PinChanged(it)) },
            label = { Text(text = stringResource(Res.string.label_pin)) },
            singleLine = true,
            enabled = !uiState.isBusy,
            isError = uiState.feedback != null,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done
            )
        )

        uiState.feedback?.let { feedback ->
            Spacer(modifier = Modifier.height(Dimensions.SIZE_12))
            UnlockFeedbackMessage(feedback)
        }

        Spacer(modifier = Modifier.height(Dimensions.SIZE_24))

        PrimaryButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onEvent(UnlockUiEvent.Submit) },
            enabled = uiState.canSubmit
        ) {
            Text(
                text = stringResource(Res.string.action_unlock),
                style = MaterialTheme.typography.labelMedium
            )
        }

        if (uiState.isBiometricOffered) {
            Spacer(modifier = Modifier.height(Dimensions.SIZE_12))

            SecondaryButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onEvent(UnlockUiEvent.UseBiometric(biometricPrompt)) }
            ) {
                Text(
                    text = stringResource(Res.string.action_use_biometric),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

/** Says what happened and, when the answer is "wait", says how long and how many tries are left. */
@Composable
private fun UnlockFeedbackMessage(feedback: UnlockFeedback, modifier: Modifier = Modifier) {
    val message = when (feedback) {
        is UnlockFeedback.IncorrectPin -> if (feedback.nextAttemptDelay > Duration.ZERO) {
            stringResource(
                Res.string.error_pin_incorrect_with_delay,
                feedback.attemptsRemaining,
                lockDelayText(feedback.nextAttemptDelay)
            )
        } else {
            stringResource(Res.string.error_pin_incorrect, feedback.attemptsRemaining)
        }

        is UnlockFeedback.Throttled ->
            stringResource(Res.string.error_pin_wait, lockDelayText(feedback.remaining))

        UnlockFeedback.PinNotSet -> stringResource(Res.string.error_pin_not_set)

        is UnlockFeedback.BiometricFellBack -> when (feedback.reason) {
            PinFallbackReason.Invalidated -> stringResource(Res.string.error_biometric_invalidated)
            else -> stringResource(Res.string.error_biometric_fell_back)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimensions.SIZE_12))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(Dimensions.SIZE_16)
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        )
    }
}

@Preview
@Composable
private fun UnlockPreview() {
    CofinanceTheme {
        Surface {
            UnlockContent(
                uiState = UnlockUiState(
                    pin = "1234",
                    isBiometricOffered = true,
                    feedback = UnlockFeedback.IncorrectPin(
                        attemptsRemaining = 4,
                        nextAttemptDelay = Duration.ZERO
                    )
                ),
                onEvent = { },
                biometricPrompt = BiometricPromptText(title = "", negativeButtonLabel = "")
            )
        }
    }
}
