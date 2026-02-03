package id.andriawan.cofinance.pages.addnew.sections

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import cofinance.composeapp.generated.resources.Res
import cofinance.composeapp.generated.resources.action_save
import cofinance.composeapp.generated.resources.ic_account
import cofinance.composeapp.generated.resources.ic_calendar
import cofinance.composeapp.generated.resources.ic_category
import cofinance.composeapp.generated.resources.ic_chevron_right
import cofinance.composeapp.generated.resources.label_account
import cofinance.composeapp.generated.resources.label_category
import cofinance.composeapp.generated.resources.label_dates
import id.andriawan.cofinance.components.AddNewSection
import id.andriawan.cofinance.components.InputAmount
import id.andriawan.cofinance.components.InputNote
import id.andriawan.cofinance.components.PrimaryButton
import id.andriawan.cofinance.components.UploadPhotoCardButton
import id.andriawan.cofinance.pages.addnew.AddNewDialogEvent
import id.andriawan.cofinance.pages.addnew.AddNewUiEvent
import id.andriawan.cofinance.pages.addnew.AddNewUiState
import id.andriawan.cofinance.theme.CofinanceTheme
import id.andriawan.cofinance.utils.Dimensions
import id.andriawan.cofinance.utils.enums.AccountTransferType
import id.andriawan.cofinance.utils.extensions.formatToString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseSection(
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
        UploadPhotoCardButton(onInputPictureClicked = { onEvent.invoke(AddNewUiEvent.OnPictureClicked) })

        InputAmount(
            modifier = Modifier.padding(horizontal = Dimensions.SIZE_16),
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
            label = stringResource(Res.string.label_account),
            value = uiState.senderAccount?.name.orEmpty(),
            onSectionClicked = {
                onEvent.invoke(AddNewUiEvent.SetAccountChooserType(AccountTransferType.SENDER))
                onDialogEvent.invoke(AddNewDialogEvent.ToggleAccountDialog(true))
            },
            startIcon = {
                Icon(
                    painter = painterResource(Res.drawable.ic_account),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            endIcon = {
                Icon(
                    painter = painterResource(Res.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        )

        AddNewSection(
            modifier = Modifier.padding(horizontal = Dimensions.SIZE_16),
            label = stringResource(Res.string.label_category),
            value = uiState.expenseCategory?.label?.let { stringResource(it) }.orEmpty(),
            onSectionClicked = { onDialogEvent.invoke(AddNewDialogEvent.ToggleCategoryDialog(true)) },
            startIcon = {
                Icon(
                    painter = painterResource(Res.drawable.ic_category),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            endIcon = {
                Icon(
                    painter = painterResource(Res.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        )

        AddNewSection(
            modifier = Modifier.padding(horizontal = Dimensions.SIZE_16),
            label = stringResource(Res.string.label_dates),
            value = uiState.dateTime.formatToString(),
            onSectionClicked = { onDialogEvent.invoke(AddNewDialogEvent.ToggleDatePickerDialog(true)) },
            startIcon = {
                Icon(
                    painter = painterResource(Res.drawable.ic_calendar),
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
                text = stringResource(Res.string.action_save),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Preview
@Composable
private fun ExpenseSectionPreview() {
    CofinanceTheme {
        Surface {
            ExpenseSection(
                uiState = AddNewUiState(),
                onEvent = { },
                onDialogEvent = { }
            )
        }
    }
}
