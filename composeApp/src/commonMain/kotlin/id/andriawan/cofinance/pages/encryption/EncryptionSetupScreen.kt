package id.andriawan.cofinance.pages.encryption

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cofinance.composeapp.generated.resources.Res
import cofinance.composeapp.generated.resources.action_copy_recovery_phrase
import cofinance.composeapp.generated.resources.action_download_recovery_phrase
import cofinance.composeapp.generated.resources.action_written_it_down
import cofinance.composeapp.generated.resources.description_encryption_setup
import cofinance.composeapp.generated.resources.description_recovery_phrase
import cofinance.composeapp.generated.resources.error_encryption_setup_failed
import cofinance.composeapp.generated.resources.message_recovery_phrase_copied
import cofinance.composeapp.generated.resources.message_recovery_phrase_copy_failed
import cofinance.composeapp.generated.resources.message_recovery_phrase_save_failed
import cofinance.composeapp.generated.resources.message_recovery_phrase_saved
import cofinance.composeapp.generated.resources.note_local_data_not_encrypted
import cofinance.composeapp.generated.resources.note_recovery_phrase_export
import cofinance.composeapp.generated.resources.title_encryption_setup
import cofinance.composeapp.generated.resources.title_recovery_phrase
import cofinance.composeapp.generated.resources.warning_recovery_phrase_loss
import id.andriawan.cofinance.components.PrimaryButton
import id.andriawan.cofinance.components.SecondaryButton
import id.andriawan.cofinance.data.crypto.PhraseExportStatus
import id.andriawan.cofinance.theme.CofinanceTheme
import id.andriawan.cofinance.utils.Dimensions
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Encryption setup, shown once at sign-in and never to a local-only user.
 *
 * The screen exists to get twelve words onto paper, so the words are laid out to be read aloud and
 * copied rather than skimmed, and the consequence of losing them is stated before the button that
 * moves past them.
 */
@Composable
fun EncryptionSetupScreen(
    onSetupComplete: () -> Unit,
    onRestoreRequired: () -> Unit,
    viewModel: EncryptionSetupViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.start() }

    LaunchedEffect(uiState.step) {
        when (uiState.step) {
            EncryptionSetupStep.Completed -> onSetupComplete()
            EncryptionSetupStep.RestoreRequired -> onRestoreRequired()
            else -> Unit
        }
    }

    Scaffold { contentPadding ->
        EncryptionSetupContent(
            modifier = Modifier.padding(contentPadding),
            uiState = uiState,
            onEvent = viewModel::onEvent
        )
    }
}

@Composable
private fun EncryptionSetupContent(
    uiState: EncryptionSetupUiState,
    onEvent: (EncryptionSetupUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    when (uiState.step) {
        EncryptionSetupStep.Preparing, EncryptionSetupStep.RestoreRequired -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (uiState.error == null) {
                    CircularProgressIndicator()
                } else {
                    Text(
                        modifier = Modifier.padding(Dimensions.SIZE_24),
                        text = stringResource(Res.string.error_encryption_setup_failed),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.error
                        )
                    )
                }
            }
        }

        EncryptionSetupStep.PhraseDisplay -> PhraseDisplayContent(
            modifier = modifier,
            uiState = uiState,
            onEvent = onEvent
        )

        EncryptionSetupStep.Completed -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
private fun PhraseDisplayContent(
    uiState: EncryptionSetupUiState,
    onEvent: (EncryptionSetupUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Dimensions.SIZE_24)
    ) {
        Text(
            text = stringResource(Res.string.title_encryption_setup),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        )

        Spacer(modifier = Modifier.height(Dimensions.SIZE_8))

        Text(
            text = stringResource(Res.string.description_encryption_setup),
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        Spacer(modifier = Modifier.height(Dimensions.SIZE_24))

        Text(
            text = stringResource(Res.string.title_recovery_phrase),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        )

        Spacer(modifier = Modifier.height(Dimensions.SIZE_4))

        Text(
            text = stringResource(Res.string.description_recovery_phrase),
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        Spacer(modifier = Modifier.height(Dimensions.SIZE_16))

        RecoveryPhraseWords(words = uiState.words)

        Spacer(modifier = Modifier.height(Dimensions.SIZE_12))

        // Copying and saving are offered side by side because neither is the right answer for every
        // user: the clipboard suits a password manager, the file suits everything else.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimensions.SIZE_12)
        ) {
            SecondaryButton(
                modifier = Modifier.weight(1f),
                enabled = !uiState.isBusy,
                onClick = { onEvent(EncryptionSetupUiEvent.CopyPhrase) }
            ) {
                Text(
                    text = stringResource(Res.string.action_copy_recovery_phrase),
                    style = MaterialTheme.typography.labelMedium
                )
            }

            SecondaryButton(
                modifier = Modifier.weight(1f),
                enabled = !uiState.isBusy,
                onClick = { onEvent(EncryptionSetupUiEvent.DownloadPhrase) }
            ) {
                Text(
                    text = stringResource(Res.string.action_download_recovery_phrase),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        uiState.exportStatus?.let { status ->
            Spacer(modifier = Modifier.height(Dimensions.SIZE_8))

            Text(
                text = exportStatusText(status),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = if (status.isFailure) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            )
        }

        Spacer(modifier = Modifier.height(Dimensions.SIZE_8))

        Text(
            text = stringResource(Res.string.note_recovery_phrase_export),
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        Spacer(modifier = Modifier.height(Dimensions.SIZE_24))

        // The consequence sits above the button that moves past the words, not below it.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Dimensions.SIZE_12))
                .background(MaterialTheme.colorScheme.errorContainer)
                .padding(Dimensions.SIZE_16)
        ) {
            Text(
                text = stringResource(Res.string.warning_recovery_phrase_loss),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            )
        }

        Spacer(modifier = Modifier.height(Dimensions.SIZE_12))

        Text(
            text = stringResource(Res.string.note_local_data_not_encrypted),
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        Spacer(modifier = Modifier.height(Dimensions.SIZE_24))

        PrimaryButton(
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isBusy,
            onClick = { onEvent(EncryptionSetupUiEvent.PhraseSaved) }
        ) {
            Text(
                text = stringResource(Res.string.action_written_it_down),
                style = MaterialTheme.typography.labelMedium
            )
        }

        uiState.error?.let {
            Spacer(modifier = Modifier.height(Dimensions.SIZE_12))

            Text(
                text = stringResource(Res.string.error_encryption_setup_failed),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.error
                )
            )
        }
    }
}

/** Whether the last export attempt left the user without the copy they asked for. */
private val PhraseExportStatus.isFailure: Boolean
    get() = this is PhraseExportStatus.CopyFailed || this is PhraseExportStatus.SaveFailed

@Composable
private fun exportStatusText(status: PhraseExportStatus): String = when (status) {
    is PhraseExportStatus.Copied -> stringResource(Res.string.message_recovery_phrase_copied)
    is PhraseExportStatus.Saved ->
        stringResource(Res.string.message_recovery_phrase_saved, status.location)

    is PhraseExportStatus.CopyFailed ->
        stringResource(Res.string.message_recovery_phrase_copy_failed)

    is PhraseExportStatus.SaveFailed ->
        stringResource(Res.string.message_recovery_phrase_save_failed)
}

/** The twelve words, numbered and two to a row, which is the shape people copy from. */
@Composable
private fun RecoveryPhraseWords(words: List<String>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimensions.SIZE_12))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(Dimensions.SIZE_16),
        verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_12)
    ) {
        words.chunked(2).forEachIndexed { rowIndex, pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimensions.SIZE_12)
            ) {
                pair.forEachIndexed { columnIndex, word ->
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(Dimensions.SIZE_8)
                    ) {
                        Text(
                            text = "${rowIndex * 2 + columnIndex + 1}.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )

                        Text(
                            text = word,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun EncryptionSetupPreview() {
    CofinanceTheme {
        Surface {
            EncryptionSetupContent(
                uiState = EncryptionSetupUiState(
                    step = EncryptionSetupStep.PhraseDisplay,
                    words = listOf(
                        "abandon", "ability", "able", "about", "above", "absent",
                        "absorb", "abstract", "absurd", "abuse", "access", "accident"
                    )
                ),
                onEvent = { }
            )
        }
    }
}
