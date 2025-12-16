package id.andriawan24.cofinance.andro.ui.presentation.addnew

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.andriawan24.cofinance.andro.R
import id.andriawan24.cofinance.andro.ui.presentation.addnew.components.AccountBottomSheetContent
import id.andriawan24.cofinance.andro.ui.presentation.addnew.components.AddAccountBottomSheet
import id.andriawan24.cofinance.andro.ui.presentation.addnew.components.CategoryBottomSheetDialog
import id.andriawan24.cofinance.andro.ui.presentation.addnew.models.TransactionTabType
import id.andriawan24.cofinance.andro.ui.presentation.addnew.sections.ExpenseSection
import id.andriawan24.cofinance.andro.ui.presentation.addnew.sections.IncomeSection
import id.andriawan24.cofinance.andro.ui.presentation.addnew.sections.TransferSection
import id.andriawan24.cofinance.andro.ui.presentation.addnew.viewmodels.AddNewDialogEvent
import id.andriawan24.cofinance.andro.ui.presentation.addnew.viewmodels.AddNewDialogState
import id.andriawan24.cofinance.andro.ui.presentation.addnew.viewmodels.AddNewUiEvent
import id.andriawan24.cofinance.andro.ui.presentation.addnew.viewmodels.AddNewUiState
import id.andriawan24.cofinance.andro.ui.presentation.addnew.viewmodels.AddNewViewModel
import id.andriawan24.cofinance.andro.ui.presentation.common.BaseBottomSheet
import id.andriawan24.cofinance.andro.ui.presentation.common.DialogDatePickerContent
import id.andriawan24.cofinance.andro.ui.presentation.common.FancyTabIndicator
import id.andriawan24.cofinance.andro.utils.CollectAsEffect
import id.andriawan24.cofinance.andro.utils.Dimensions
import id.andriawan24.cofinance.andro.utils.ext.formatToString
import id.andriawan24.cofinance.andro.utils.ext.hideAndDismiss
import id.andriawan24.cofinance.andro.utils.ext.setDate
import id.andriawan24.cofinance.andro.utils.ext.setTime
import id.andriawan24.cofinance.utils.enums.TransactionType
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.util.Calendar

@Composable
fun AddTransactionScreen(
    transactionId: String?,
    onBackPressed: () -> Unit,
    onSuccessSave: () -> Unit,
    onInputPictureClicked: () -> Unit,
) {
    val addNewViewModel: AddNewViewModel = koinViewModel()
    val uiState by addNewViewModel.uiState.collectAsStateWithLifecycle()
    val dialogState by addNewViewModel.dialogState.collectAsStateWithLifecycle()
    val snackState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(true) {
        if (transactionId != null) {
            addNewViewModel.checkDraftTransaction(transactionId)
        }
    }

    addNewViewModel.onSuccessSaved.CollectAsEffect {
        onSuccessSave.invoke()
    }

    addNewViewModel.showMessage.CollectAsEffect {
        scope.launch { snackState.showSnackbar(it) }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackState) }) { contentPadding ->
        AddNewContent(
            modifier = Modifier.padding(contentPadding),
            uiState = uiState,
            dialogState = dialogState,
            onEvent = {
                when (it) {
                    AddNewUiEvent.OnBackPressed -> onBackPressed.invoke()
                    AddNewUiEvent.OnPictureClicked -> onInputPictureClicked.invoke()
                    else -> addNewViewModel.onEvent(it)
                }
            },
            onDialogEvent = { addNewViewModel.onDialogEvent(it) }
        )
    }
}

@Composable
fun AddNewContent(
    modifier: Modifier = Modifier,
    uiState: AddNewUiState,
    dialogState: AddNewDialogState,
    onEvent: (AddNewUiEvent) -> Unit,
    onDialogEvent: (AddNewDialogEvent) -> Unit
) {
    val pagerState = rememberPagerState { TransactionTabType.entries.size }

    Column(modifier = modifier.fillMaxSize()) {
        AddTransactionHeader(onEvent = onEvent)
        TransactionTypeTabs(pagerState = pagerState, onEvent = onEvent)
        TransactionPagerContent(
            modifier = Modifier.weight(1f),
            pagerState = pagerState,
            uiState = uiState,
            onEvent = onEvent,
            onDialogEvent = onDialogEvent
        )
    }

    // Dialogs
    CategoryBottomSheetDialog(dialogState, uiState, onEvent, onDialogEvent)
    DateBottomSheetDialog(dialogState, uiState, onEvent, onDialogEvent)
    TimePickerDialog(dialogState, uiState, onEvent, onDialogEvent)
    AccountBottomSheetDialog(dialogState, uiState, onEvent, onDialogEvent)
    AddAccountBottomSheetDialog(dialogState, onEvent, onDialogEvent)
}

@Composable
private fun AddTransactionHeader(onEvent: (AddNewUiEvent) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Dimensions.SIZE_12, bottom = Dimensions.SIZE_24)
            .padding(horizontal = Dimensions.SIZE_16)
    ) {
        IconButton(
            onClick = { onEvent(AddNewUiEvent.OnBackPressed) },
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_left),
                contentDescription = null
            )
        }

        Text(
            modifier = Modifier.align(Alignment.Center),
            text = stringResource(R.string.title_add_activity),
            style = MaterialTheme.typography.labelMedium.copy(
                color = MaterialTheme.colorScheme.onBackground
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionTypeTabs(pagerState: PagerState, onEvent: (AddNewUiEvent) -> Unit) {
    val scope = rememberCoroutineScope()

    SecondaryTabRow(
        modifier = Modifier
            .padding(horizontal = Dimensions.SIZE_16)
            .clip(MaterialTheme.shapes.extraLarge),
        containerColor = MaterialTheme.colorScheme.onPrimary,
        selectedTabIndex = pagerState.currentPage,
        indicator = {
            FancyTabIndicator(
                modifier = Modifier.tabIndicatorOffset(pagerState.currentPage),
                label = stringResource(TransactionTabType.getByIndex(pagerState.currentPage).labelResId)
            )
        },
        divider = {
            /* no-op */
        }
    ) {
        TransactionTabType.entries.forEachIndexed { index, type ->
            Tab(
                modifier = Modifier.clip(MaterialTheme.shapes.extraLarge),
                selected = pagerState.currentPage == index,
                onClick = {
                    scope.launch {
                        pagerState.animateScrollToPage(index)
                        onEvent(AddNewUiEvent.SetTransactionType(TransactionType.entries[index]))
                    }
                },
                text = {
                    Text(
                        text = stringResource(id = type.labelResId),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (index == pagerState.currentPage) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    )
                }
            )
        }
    }
}

@Composable
private fun TransactionPagerContent(
    modifier: Modifier = Modifier,
    pagerState: PagerState,
    uiState: AddNewUiState,
    onEvent: (AddNewUiEvent) -> Unit,
    onDialogEvent: (AddNewDialogEvent) -> Unit
) {
    HorizontalPager(modifier = modifier, state = pagerState) {
        when (it) {
            TransactionTabType.EXPENSES.ordinal -> ExpenseSection(
                uiState = uiState,
                onEvent = onEvent,
                onDialogEvent = onDialogEvent
            )

            TransactionTabType.INCOME.ordinal -> IncomeSection(
                uiState = uiState,
                onEvent = onEvent,
                onDialogEvent = onDialogEvent
            )

            TransactionTabType.TRANSFER.ordinal -> TransferSection(
                uiState = uiState,
                onEvent = onEvent,
                onDialogEvent = onDialogEvent
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateBottomSheetDialog(
    dialogState: AddNewDialogState,
    uiState: AddNewUiState,
    onEvent: (AddNewUiEvent) -> Unit,
    onDialogEvent: (AddNewDialogEvent) -> Unit
) {
    if (!dialogState.showDateBottomSheet) return

    val scope = rememberCoroutineScope()
    val dateBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = uiState.dateTime.time)

    BaseBottomSheet(
        state = dateBottomSheetState,
        onDismissRequest = {
            onDialogEvent(AddNewDialogEvent.ToggleDatePickerDialog(false))
        }
    ) {
        DialogDatePickerContent(
            currentDate = uiState.dateTime.formatToString("HH:mm z"),
            datePickerState = datePickerState,
            onSavedDate = {
                val newDate = datePickerState.selectedDateMillis?.let { millis ->
                    uiState.dateTime.setDate(millis)
                } ?: uiState.dateTime

                onEvent(AddNewUiEvent.SetDateTime(newDate))
                scope.launch {
                    dateBottomSheetState.hideAndDismiss {
                        onDialogEvent(AddNewDialogEvent.ToggleDatePickerDialog(false))
                    }
                }
            },
            onHourClicked = {
                onDialogEvent(AddNewDialogEvent.ToggleTimePickerDialog(true))
            },
            onCloseDate = {
                scope.launch {
                    dateBottomSheetState.hideAndDismiss {
                        onDialogEvent(AddNewDialogEvent.ToggleDatePickerDialog(false))
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    dialogState: AddNewDialogState,
    uiState: AddNewUiState,
    onEvent: (AddNewUiEvent) -> Unit,
    onDialogEvent: (AddNewDialogEvent) -> Unit
) {
    if (!dialogState.showTimePickerDialog) return

    val timePickerState = rememberTimePickerState(is24Hour = true)

    LaunchedEffect(uiState.dateTime) {
        val calendar = Calendar.getInstance().apply {
            time = uiState.dateTime
        }
        timePickerState.apply {
            minute = calendar[Calendar.MINUTE]
            hour = calendar[Calendar.HOUR_OF_DAY]
        }
    }

    AlertDialog(
        onDismissRequest = {
            onDialogEvent(AddNewDialogEvent.ToggleTimePickerDialog(false))
        },
        text = { TimePicker(state = timePickerState) },
        dismissButton = {
            TextButton(onClick = {
                onDialogEvent(AddNewDialogEvent.ToggleTimePickerDialog(false))
            }) {
                Text(text = stringResource(R.string.label_cancel))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val newDateTime = uiState.dateTime.setTime(
                        hour = timePickerState.hour,
                        minute = timePickerState.minute
                    )
                    onEvent(AddNewUiEvent.SetDateTime(newDateTime))
                    onDialogEvent(AddNewDialogEvent.ToggleTimePickerDialog(false))
                }
            ) {
                Text(text = stringResource(R.string.label_ok))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountBottomSheetDialog(
    dialogState: AddNewDialogState,
    uiState: AddNewUiState,
    onEvent: (AddNewUiEvent) -> Unit,
    onDialogEvent: (AddNewDialogEvent) -> Unit
) {
    if (!dialogState.showAccountBottomSheet) return

    val scope = rememberCoroutineScope()
    val accountBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    BaseBottomSheet(
        state = accountBottomSheetState,
        onDismissRequest = {
            onDialogEvent(AddNewDialogEvent.ToggleAccountDialog(false))
        }
    ) {
        AccountBottomSheetContent(
            uiState = uiState,
            onAccountSaved = { account ->
                onEvent(AddNewUiEvent.SetAccount(account))
                onDialogEvent(AddNewDialogEvent.ToggleAccountDialog(false))
            },
            onAddAccountClicked = {
                scope.launch {
                    accountBottomSheetState.hideAndDismiss {
                        onDialogEvent(AddNewDialogEvent.ToggleAccountDialog(false))
                        onDialogEvent(AddNewDialogEvent.ToggleAddAccountDialog(true))
                    }
                }
            },
            onCloseClicked = {
                scope.launch {
                    accountBottomSheetState.hideAndDismiss {
                        onDialogEvent(AddNewDialogEvent.ToggleAccountDialog(false))
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddAccountBottomSheetDialog(
    dialogState: AddNewDialogState,
    onEvent: (AddNewUiEvent) -> Unit,
    onDialogEvent: (AddNewDialogEvent) -> Unit
) {
    if (!dialogState.showAddAccountBottomSheet) return

    val scope = rememberCoroutineScope()
    val addAccountBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    BaseBottomSheet(
        state = addAccountBottomSheetState,
        onDismissRequest = {
            onDialogEvent(AddNewDialogEvent.ToggleAddAccountDialog(false))
        }
    ) {
        AddAccountBottomSheet(
            onAccountSaved = {
                scope.launch {
                    addAccountBottomSheetState.hide()
                    onDialogEvent(AddNewDialogEvent.ToggleAddAccountDialog(false))
                    onEvent(AddNewUiEvent.UpdateAccount)
                    onDialogEvent(AddNewDialogEvent.ToggleAccountDialog(true))
                }
            },
            onCloseClicked = {
                scope.launch {
                    addAccountBottomSheetState.hideAndDismiss {
                        onDialogEvent(AddNewDialogEvent.ToggleAddAccountDialog(false))
                    }
                }
            }
        )
    }
}
