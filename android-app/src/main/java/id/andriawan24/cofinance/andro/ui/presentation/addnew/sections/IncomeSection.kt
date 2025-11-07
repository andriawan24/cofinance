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
import id.andriawan24.cofinance.andro.ui.presentation.addnew.components.TransactionCategoryBottomSheet
import id.andriawan24.cofinance.andro.ui.presentation.addnew.viewmodels.AddNewUiEvent
import id.andriawan24.cofinance.andro.ui.presentation.addnew.viewmodels.AddNewUiState
import id.andriawan24.cofinance.andro.ui.presentation.common.BaseBottomSheet
import id.andriawan24.cofinance.andro.ui.presentation.common.DialogDatePickerContent
import id.andriawan24.cofinance.andro.ui.theme.CofinanceTheme
import id.andriawan24.cofinance.andro.utils.Dimensions
import id.andriawan24.cofinance.andro.utils.LocaleHelper
import id.andriawan24.cofinance.andro.utils.enums.TransactionCategory
import id.andriawan24.cofinance.andro.utils.ext.formatToString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeSection(
    uiState: AddNewUiState,
    onEvent: (AddNewUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope: CoroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    val transactionCategories = remember { TransactionCategory.getIncomeCategories() }

    val categoryBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val accountBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dateBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val addAccountBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val timePickerState = rememberTimePickerState(is24Hour = true)
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = uiState.dateTime.time)

    var showCategoryBottomSheet by remember { mutableStateOf(false) }
    var showAccountBottomSheet by remember { mutableStateOf(false) }
    var showDateBottomSheet by remember { mutableStateOf(false) }
    var showTimePickerDialog by remember { mutableStateOf(false) }
    var showAddAccountBottomSheet by remember { mutableStateOf(false) }

    LaunchedEffect(true) {
        val calendar = Calendar.getInstance().apply {
            time = uiState.dateTime
        }

        timePickerState.apply {
            minute = calendar[Calendar.MINUTE]
            hour = calendar[Calendar.HOUR_OF_DAY]
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
            enableFee = false,
            includeFee = uiState.includeFee,
            onAmountChanged = { amount -> onEvent.invoke(AddNewUiEvent.SetAmount(amount)) },
            onFeeChanged = { fee -> onEvent.invoke(AddNewUiEvent.SetFee(fee)) },
            onIncludeFeeChanged = { isIncludeFee ->
                onEvent.invoke(AddNewUiEvent.SetIncludeFee(isIncludeFee))
                focusManager.clearFocus()
            }
        )

        AddNewSection(
            modifier = Modifier.padding(horizontal = Dimensions.SIZE_16),
            label = stringResource(R.string.label_account),
            value = uiState.account?.name.orEmpty(),
            onSectionClicked = { showAccountBottomSheet = true },
            startIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_account),
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
            label = stringResource(R.string.label_category),
            value = uiState.incomeCategory?.labelRes?.let { stringResource(it) }.orEmpty(),
            onSectionClicked = { showCategoryBottomSheet = true },
            startIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_category),
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

    if (showCategoryBottomSheet) {
        BaseBottomSheet(
            state = categoryBottomSheetState,
            onDismissRequest = { }
        ) {
            TransactionCategoryBottomSheet(
                categories = transactionCategories,
                selectedCategory = uiState.incomeCategory,
                onCategorySaved = { category ->
                    onEvent.invoke(AddNewUiEvent.SetIncomeCategory(category))
                    scope.launch {
                        categoryBottomSheetState.hide()
                    }
                },
                onCloseCategoryClicked = {
                    scope.launch {
                        categoryBottomSheetState.hide()
                    }
                }
            )
        }
    }

    if (showDateBottomSheet) {
        BaseBottomSheet(
            state = dateBottomSheetState,
            onDismissRequest = { }
        ) {
            DialogDatePickerContent(
                currentDate = uiState.dateTime.formatToString("HH:mm z"),
                datePickerState = datePickerState,
                onSavedDate = {
                    val chosenCal = Calendar.getInstance().apply {
                        time = datePickerState.selectedDateMillis?.let { Date(it) } ?: Date()
                    }

                    val calendar = Calendar.getInstance().apply {
                        time = uiState.dateTime
                        set(Calendar.YEAR, chosenCal[Calendar.YEAR])
                        set(Calendar.MONTH, chosenCal[Calendar.MONTH])
                        set(Calendar.DAY_OF_MONTH, chosenCal[Calendar.DAY_OF_MONTH])
                    }
                    onEvent.invoke(AddNewUiEvent.SetDateTime(calendar.time))
                    scope.launch {
                        dateBottomSheetState.hide()
                    }
                },
                onHourClicked = { showTimePickerDialog = true },
                onCloseDate = {
                    scope.launch {
                        dateBottomSheetState.hide()
                    }
                }
            )
        }
    }

    if (showAccountBottomSheet) {
        BaseBottomSheet(
            state = accountBottomSheetState,
            onDismissRequest = { }
        ) {
            AccountBottomSheet(
                isLoading = uiState.loadingAccount,
                accounts = uiState.accounts,
                selectedAccount = uiState.account,
                onAccountSaved = { account ->
                    onEvent(AddNewUiEvent.SetAccount(account))
                    scope.launch {
                        accountBottomSheetState.hide()
                    }
                },
                onAddAccountClicked = {
                    scope.launch {
                        accountBottomSheetState.hide()
                        showAddAccountBottomSheet = true
                    }
                },
                onCloseClicked = {
                    scope.launch {
                        accountBottomSheetState.hide()
                    }
                }
            )
        }
    }

    if (showAddAccountBottomSheet) {
        BaseBottomSheet(
            state = addAccountBottomSheetState,
            onDismissRequest = { }
        ) {
            AddAccountBottomSheet(
                onAccountSaved = {
                    scope.launch {
                        addAccountBottomSheetState.hide()
                        onEvent.invoke(AddNewUiEvent.UpdateAccount)
                    }
                },
                onCloseClicked = {
                    scope.launch {
                        addAccountBottomSheetState.hide()
                    }
                }
            )
        }
    }

    if (showTimePickerDialog) {
        AlertDialog(
            onDismissRequest = { },
            text = { TimePicker(state = timePickerState) },
            dismissButton = {
                TextButton(onClick = { }) {
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
private fun IncomeSectionPreview() {
    CofinanceTheme {
        Surface {
            IncomeSection(
                uiState = AddNewUiState(),
                onEvent = { }
            )
        }
    }
}