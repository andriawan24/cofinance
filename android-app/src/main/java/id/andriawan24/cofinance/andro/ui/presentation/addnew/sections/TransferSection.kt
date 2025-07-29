package id.andriawan24.cofinance.andro.ui.presentation.addnew.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import id.andriawan24.cofinance.andro.R
import id.andriawan24.cofinance.andro.ui.components.PrimaryButton
import id.andriawan24.cofinance.andro.ui.presentation.addnew.components.AccountBottomSheet
import id.andriawan24.cofinance.andro.ui.presentation.addnew.components.AddAccountBottomSheet
import id.andriawan24.cofinance.andro.ui.presentation.addnew.components.AddNewSection
import id.andriawan24.cofinance.andro.ui.presentation.addnew.components.InputAmount
import id.andriawan24.cofinance.andro.ui.presentation.addnew.components.InputNote
import id.andriawan24.cofinance.andro.ui.presentation.addnew.viewmodels.AddNewUiEvent
import id.andriawan24.cofinance.andro.ui.presentation.addnew.viewmodels.AddNewUiState
import id.andriawan24.cofinance.andro.ui.presentation.common.BaseBottomSheet
import id.andriawan24.cofinance.andro.ui.presentation.common.DialogDatePickerContent
import id.andriawan24.cofinance.andro.ui.theme.CofinanceTheme
import id.andriawan24.cofinance.andro.utils.Dimensions
import id.andriawan24.cofinance.andro.utils.LocaleHelper
import id.andriawan24.cofinance.andro.utils.ext.formatToString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

private enum class AccountTransferType {
    SENDER, RECEIVER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferSection(
    uiState: AddNewUiState,
    onEvent: (AddNewUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope: CoroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    val accountBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dateBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val addAccountBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val timePickerState = rememberTimePickerState(is24Hour = true)
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = uiState.dateTime.time)

    var lastAccountTypeOpened by remember { mutableStateOf(AccountTransferType.SENDER) }

    var showAccountBottomSheet by remember { mutableStateOf(false) }
    var showDateBottomSheet by remember { mutableStateOf(false) }
    var showTimePickerDialog by remember { mutableStateOf(false) }
    var showAddAccountBottomSheet by remember { mutableStateOf(false) }

    LaunchedEffect(true) {
        val calendar = Calendar.getInstance().apply {
            time = uiState.dateTime
        }

        timePickerState.apply {
            minute = calendar.get(Calendar.MINUTE)
            hour = calendar.get(Calendar.HOUR_OF_DAY)
        }
    }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
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
            value = uiState.account?.name.orEmpty(),
            onSectionClicked = {
                lastAccountTypeOpened = AccountTransferType.SENDER
                showAccountBottomSheet = true
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
                lastAccountTypeOpened = AccountTransferType.RECEIVER
                showAccountBottomSheet = true
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
            onSectionClicked = { showDateBottomSheet = true },
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

    if (showDateBottomSheet) {
        BaseBottomSheet(
            state = dateBottomSheetState,
            onDismissRequest = { showDateBottomSheet = false }
        ) {
            DialogDatePickerContent(
                currentDate = uiState.dateTime,
                datePickerState = datePickerState,
                onSavedDate = {
                    val chosenCal = Calendar.getInstance().apply {
                        time = datePickerState.selectedDateMillis?.let { Date(it) } ?: Date()
                    }

                    val calendar = Calendar.getInstance().apply {
                        time = uiState.dateTime
                        set(Calendar.YEAR, chosenCal.get(Calendar.YEAR))
                        set(Calendar.MONTH, chosenCal.get(Calendar.MONTH))
                        set(Calendar.DAY_OF_MONTH, chosenCal.get(Calendar.DAY_OF_MONTH))
                    }
                    onEvent.invoke(AddNewUiEvent.SetDateTime(calendar.time))
                    scope.launch {
                        dateBottomSheetState.hide()
                        showDateBottomSheet = false
                    }
                },
                onHourClicked = { showTimePickerDialog = true },
                onCloseDate = {
                    scope.launch {
                        dateBottomSheetState.hide()
                        showDateBottomSheet = false
                    }
                }
            )
        }
    }

    if (showAccountBottomSheet) {
        BaseBottomSheet(
            state = accountBottomSheetState,
            onDismissRequest = { showAccountBottomSheet = false }
        ) {
            AccountBottomSheet(
                isLoading = uiState.isLoading,
                accounts = uiState.accounts,
                selectedAccount = when (lastAccountTypeOpened) {
                    AccountTransferType.SENDER -> uiState.account
                    else -> uiState.receiverAccount
                },
                onAccountSaved = { account ->
                    when (lastAccountTypeOpened) {
                        AccountTransferType.SENDER -> {
                            onEvent(AddNewUiEvent.SetAccount(account))
                        }

                        AccountTransferType.RECEIVER -> {
                            onEvent(AddNewUiEvent.SetReceiverAccount(account))
                        }
                    }
                    scope.launch {
                        accountBottomSheetState.hide()
                        showAccountBottomSheet = false
                    }
                },
                onAddAccountClicked = {
                    scope.launch {
                        accountBottomSheetState.hide()
                        showAccountBottomSheet = false
                        showAddAccountBottomSheet = true
                    }
                },
                onCloseClicked = {
                    scope.launch {
                        accountBottomSheetState.hide()
                        showAccountBottomSheet = false
                    }
                }
            )
        }
    }

    if (showAddAccountBottomSheet) {
        BaseBottomSheet(
            state = addAccountBottomSheetState,
            onDismissRequest = { showAddAccountBottomSheet = false }
        ) {
            AddAccountBottomSheet(
                onAccountSaved = {
                    scope.launch {
                        addAccountBottomSheetState.hide()
                        showAddAccountBottomSheet = false
                        showAccountBottomSheet = true
                        onEvent.invoke(AddNewUiEvent.UpdateAccount)
                    }
                },
                onCloseClicked = {
                    scope.launch {
                        addAccountBottomSheetState.hide()
                        showAddAccountBottomSheet = false
                    }
                }
            )
        }
    }

    if (showTimePickerDialog) {
        AlertDialog(
            onDismissRequest = { showTimePickerDialog = false },
            text = { TimePicker(state = timePickerState) },
            dismissButton = {
                TextButton(onClick = { showTimePickerDialog = false }) {
                    Text(text = stringResource(R.string.label_cancel))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val calendar = Calendar.getInstance().apply {
                            time = uiState.dateTime
                            set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                            set(Calendar.MINUTE, timePickerState.minute)
                        }

                        onEvent.invoke(AddNewUiEvent.SetDateTime(calendar.time))
                        showTimePickerDialog = false
                    }
                ) {
                    Text(text = stringResource(R.string.label_ok))
                }
            }
        )
    }
}

@Preview
@Composable
private fun TransferSectionPreview() {
    CofinanceTheme {
        Surface {
            TransferSection(
                uiState = AddNewUiState(),
                onEvent = { }
            )
        }
    }
}