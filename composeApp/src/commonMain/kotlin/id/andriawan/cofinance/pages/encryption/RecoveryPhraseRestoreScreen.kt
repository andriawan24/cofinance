package id.andriawan.cofinance.pages.encryption

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cofinance.composeapp.generated.resources.Res
import cofinance.composeapp.generated.resources.action_restore
import cofinance.composeapp.generated.resources.description_restore_data
import cofinance.composeapp.generated.resources.error_recovery_phrase_checksum
import cofinance.composeapp.generated.resources.error_recovery_phrase_group_count
import cofinance.composeapp.generated.resources.error_recovery_phrase_malformed_group
import cofinance.composeapp.generated.resources.error_recovery_phrase_mismatch
import cofinance.composeapp.generated.resources.error_recovery_phrase_no_key_material
import cofinance.composeapp.generated.resources.error_restore_failed
import cofinance.composeapp.generated.resources.label_recovery_phrase_input
import cofinance.composeapp.generated.resources.title_restore_data
import id.andriawan.cofinance.components.PrimaryButton
import id.andriawan.cofinance.theme.CofinanceTheme
import id.andriawan.cofinance.utils.Dimensions
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Restore on a device that holds no key material.
 *
 * The whole phrase is typed into one field rather than into six, because a phrase read off paper
 * arrives with arbitrary spacing and line breaks and the parser already normalizes both. What the
 * screen has to do well is say precisely what is wrong when entry fails, which is why the errors are
 * distinct: a mistyped group names its position, and a phrase whose groups are all well formed but
 * which does not check out says so instead of pretending the phrase is simply "wrong".
 *
 * Case carries entropy in this phrase, so the field neither autocapitalizes nor autocorrects, and it
 * renders in monospace so the user can tell the characters apart while typing them.
 */
@Composable
fun RecoveryPhraseRestoreScreen(
    onRestored: () -> Unit,
    viewModel: RecoveryPhraseRestoreViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isRestored) {
        if (uiState.isRestored) onRestored()
    }

    Scaffold { contentPadding ->
        RecoveryPhraseRestoreContent(
            modifier = Modifier.padding(contentPadding),
            uiState = uiState,
            onEvent = viewModel::onEvent
        )
    }
}

@Composable
private fun RecoveryPhraseRestoreContent(
    uiState: RecoveryPhraseRestoreUiState,
    onEvent: (RecoveryPhraseRestoreUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Dimensions.SIZE_24)
    ) {
        Text(
            text = stringResource(Res.string.title_restore_data),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        )

        Spacer(modifier = Modifier.height(Dimensions.SIZE_8))

        Text(
            text = stringResource(Res.string.description_restore_data),
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        Spacer(modifier = Modifier.height(Dimensions.SIZE_24))

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = uiState.phraseInput,
            onValueChange = { onEvent(RecoveryPhraseRestoreUiEvent.PhraseChanged(it)) },
            label = { Text(text = stringResource(Res.string.label_recovery_phrase_input)) },
            minLines = 3,
            isError = uiState.error != null,
            enabled = !uiState.isRestoring,
            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done,
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false
            )
        )

        uiState.error?.let { error ->
            Spacer(modifier = Modifier.height(Dimensions.SIZE_8))

            Text(
                text = error.asMessage(),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.error
                )
            )
        }

        Spacer(modifier = Modifier.height(Dimensions.SIZE_24))

        PrimaryButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onEvent(RecoveryPhraseRestoreUiEvent.Restore) },
            enabled = uiState.canRestore
        ) {
            Text(
                text = stringResource(Res.string.action_restore),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun RecoveryPhraseEntryError.asMessage(): String = when (this) {
    is RecoveryPhraseEntryError.WrongGroupCount ->
        stringResource(Res.string.error_recovery_phrase_group_count, actual)

    is RecoveryPhraseEntryError.MalformedGroup ->
        stringResource(Res.string.error_recovery_phrase_malformed_group, position, group)

    RecoveryPhraseEntryError.ChecksumFailed ->
        stringResource(Res.string.error_recovery_phrase_checksum)

    RecoveryPhraseEntryError.PhraseDoesNotOpenThisAccount ->
        stringResource(Res.string.error_recovery_phrase_mismatch)

    RecoveryPhraseEntryError.NoStoredKeyMaterial ->
        stringResource(Res.string.error_recovery_phrase_no_key_material)

    RecoveryPhraseEntryError.RestoreFailed ->
        stringResource(Res.string.error_restore_failed)
}

@Preview
@Composable
private fun RecoveryPhraseRestorePreview() {
    CofinanceTheme {
        Surface {
            RecoveryPhraseRestoreContent(
                uiState = RecoveryPhraseRestoreUiState(
                    phraseInput = "k3Rm 9XaQ 2mNp",
                    error = RecoveryPhraseEntryError.WrongGroupCount(actual = 3)
                ),
                onEvent = { }
            )
        }
    }
}
