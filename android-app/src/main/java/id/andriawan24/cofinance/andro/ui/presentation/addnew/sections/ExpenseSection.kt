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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
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
import id.andriawan24.cofinance.andro.ui.presentation.addnew.components.BaseBottomSheet
import id.andriawan24.cofinance.andro.ui.presentation.addnew.components.CategoryBottomSheet
import id.andriawan24.cofinance.andro.ui.presentation.addnew.components.DialogDatePickerContent
import id.andriawan24.cofinance.andro.ui.presentation.addnew.components.InputAmount
import id.andriawan24.cofinance.andro.ui.presentation.addnew.components.InputNote
import id.andriawan24.cofinance.andro.ui.presentation.addnew.components.UploadPhotoCardButton
import id.andriawan24.cofinance.andro.ui.presentation.addnew.viewmodels.AddNewUiState
import id.andriawan24.cofinance.andro.ui.theme.CofinanceTheme
import id.andriawan24.cofinance.andro.utils.Dimensions
import id.andriawan24.cofinance.andro.utils.enums.ExpenseCategory
import id.andriawan24.cofinance.andro.utils.ext.formatToString
import id.andriawan24.cofinance.domain.model.response.Account
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseSection(
    uiState: AddNewUiState,
    onInputPictureClicked: () -> Unit,
    onAmountChanged: (String) -> Unit,
    onFeeChanged: (String) -> Unit,
    onNoteChanged: (String) -> Unit,
    onCategoryChanged: (ExpenseCategory) -> Unit,
    onAccountChanged: (Account) -> Unit,
    onDateTimeChanged: (Date) -> Unit,
    onIncludeFeeChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope: CoroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    val categoryBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val accountBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dateBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val addAccountBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val timePickerState = rememberTimePickerState(is24Hour = true)
    val datePickerState = rememberDatePickerState()

    var showCategoryBottomSheet by remember { mutableStateOf(false) }
    var showAccountBottomSheet by remember { mutableStateOf(false) }
    var showDateBottomSheet by remember { mutableStateOf(false) }
    var showTimePickerDialog by remember { mutableStateOf(false) }
    var showAddAccountBottomSheet by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_16)
    ) {
        UploadPhotoCardButton(onInputPictureClicked = onInputPictureClicked)

        InputAmount(
            modifier = Modifier.padding(horizontal = Dimensions.SIZE_16),
            amount = uiState.amount,
            fee = uiState.fee,
            includeFee = uiState.includeFee,
            onAmountChanged = onAmountChanged,
            onFeeChanged = onFeeChanged,
            onIncludeFeeChanged = {
                onIncludeFeeChanged(it)
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
            value = uiState.category?.label.orEmpty(),
            onSectionClicked = {
                showCategoryBottomSheet = true
            },
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
            value = uiState.dateTime.formatToString(),
            onSectionClicked = {
                showDateBottomSheet = true
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
            onNoteChanged = onNoteChanged
        )

        Spacer(modifier = Modifier.weight(1f))

        PrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = Dimensions.SIZE_16),
            enabled = uiState.isValid,
            onClick = { },
        ) {
            Text(
                text = stringResource(R.string.action_save),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }

    if (showCategoryBottomSheet) {
        BaseBottomSheet(
            state = categoryBottomSheetState,
            onDismissRequest = {
                showCategoryBottomSheet = false
            }
        ) {
            CategoryBottomSheet(
                selectedCategory = uiState.category,
                onCategorySaved = { category ->
                    onCategoryChanged(category)
                    scope.launch {
                        categoryBottomSheetState.hide()
                        showCategoryBottomSheet = false
                    }
                },
                onCloseCategoryClicked = {
                    scope.launch {
                        categoryBottomSheetState.hide()
                        showCategoryBottomSheet = false
                    }
                }
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
                    onDateTimeChanged(calendar.time)
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
                accounts = uiState.accounts,
                selectedAccount = uiState.account,
                onAccountSaved = { account ->
                    onAccountChanged(account)
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
            dismissButton = {
                TextButton(
                    onClick = {
                        showTimePickerDialog = false
                    }
                ) {
                    Text(stringResource(R.string.label_cancel))
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
                        onDateTimeChanged(calendar.time)
                        showTimePickerDialog = false
                    }
                ) {
                    Text("OK")
                }
            },
            text = { TimePicker(state = timePickerState) }
        )
    }
}

@Preview
@Composable
private fun ExpenseSectionPreview() {
    CofinanceTheme {
        ExpenseSection(
            uiState = AddNewUiState(),
            onInputPictureClicked = { },
            onAmountChanged = { },
            onFeeChanged = { },
            onNoteChanged = { },
            onCategoryChanged = { },
            onAccountChanged = { },
            onDateTimeChanged = { },
            onIncludeFeeChanged = { },
        )
    }
}