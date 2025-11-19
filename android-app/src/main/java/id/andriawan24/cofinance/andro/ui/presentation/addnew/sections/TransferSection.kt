package id.andriawan24.cofinance.andro.ui.presentation.addnew.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import id.andriawan24.cofinance.andro.R
import id.andriawan24.cofinance.andro.ui.components.PrimaryButton
import id.andriawan24.cofinance.andro.ui.presentation.addnew.components.AddNewSection
import id.andriawan24.cofinance.andro.ui.presentation.addnew.components.InputAmount
import id.andriawan24.cofinance.andro.ui.presentation.addnew.components.InputNote
import id.andriawan24.cofinance.andro.ui.presentation.addnew.viewmodels.AddNewDialogEvent
import id.andriawan24.cofinance.andro.ui.presentation.addnew.viewmodels.AddNewUiEvent
import id.andriawan24.cofinance.andro.ui.presentation.addnew.viewmodels.AddNewUiState
import id.andriawan24.cofinance.andro.ui.theme.CofinanceTheme
import id.andriawan24.cofinance.andro.utils.Dimensions
import id.andriawan24.cofinance.andro.utils.LocaleHelper
import id.andriawan24.cofinance.andro.utils.enums.AccountTransferType
import id.andriawan24.cofinance.andro.utils.ext.formatToString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferSection(
    uiState: AddNewUiState,
    onEvent: (AddNewUiEvent) -> Unit,
    onDialogEvent: (AddNewDialogEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_16)
    ) {
        InputAmount(
            modifier = Modifier
                .padding(horizontal = Dimensions.SIZE_16)
                .padding(top = Dimensions.SIZE_16),
            amount = uiState.amount,
            fee = uiState.fee,
            includeFee = uiState.includeFee,
            enableFee = true,
            onAmountChanged = { amount -> onEvent.invoke(AddNewUiEvent.SetAmount(amount)) },
            onFeeChanged = { fee -> onEvent.invoke(AddNewUiEvent.SetFee(fee)) },
            onIncludeFeeChanged = { isIncludeFee ->
                onEvent.invoke(AddNewUiEvent.SetIncludeFee(isIncludeFee))
                focusManager.clearFocus()
            }
        )

        AddNewSection(
            modifier = Modifier.padding(horizontal = Dimensions.SIZE_16),
            label = stringResource(R.string.label_sender_account),
            value = uiState.senderAccount?.name.orEmpty(),
            onSectionClicked = {
                onEvent.invoke(AddNewUiEvent.SetAccountChooserType(AccountTransferType.SENDER))
                onDialogEvent.invoke(AddNewDialogEvent.ToggleAccountDialog(true))
            },
            startIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_sender_account),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            endIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        )

        AddNewSection(
            modifier = Modifier.padding(horizontal = Dimensions.SIZE_16),
            label = stringResource(R.string.label_beneficiary_account),
            value = uiState.receiverAccount?.name.orEmpty(),
            onSectionClicked = {
                onEvent.invoke(AddNewUiEvent.SetAccountChooserType(AccountTransferType.RECEIVER))
                onDialogEvent.invoke(AddNewDialogEvent.ToggleAccountDialog(true))
            },
            startIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_receiver_account),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            endIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        )

        AddNewSection(
            modifier = Modifier.padding(horizontal = Dimensions.SIZE_16),
            label = stringResource(R.string.label_dates),
            value = uiState.dateTime.formatToString(locale = LocaleHelper.indonesian),
            onSectionClicked = {
                onDialogEvent.invoke(AddNewDialogEvent.ToggleDatePickerDialog(true))
            },
            startIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_calendar),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        )

        InputNote(
            modifier = Modifier.padding(horizontal = Dimensions.SIZE_16),
            note = uiState.notes,
            onNoteChanged = { note -> onEvent.invoke(AddNewUiEvent.SetNote(note)) }
        )

        Spacer(modifier = Modifier.weight(1f))

        PrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = Dimensions.SIZE_16),
            enabled = uiState.isValid,
            onClick = {
                onEvent.invoke(AddNewUiEvent.SaveTransaction)
            },
        ) {
            Text(
                text = stringResource(R.string.action_save),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Preview
@Composable
private fun TransferSectionPreview() {
    CofinanceTheme {
        Surface {
            TransferSection(
                uiState = AddNewUiState(),
                onEvent = { },
                onDialogEvent = { }
            )
        }
    }
}