package id.andriawan24.cofinance.andro.ui.presentation.addnew.viewmodels

import android.net.Uri
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.andriawan24.cofinance.andro.utils.None
import id.andriawan24.cofinance.andro.utils.emptyString
import id.andriawan24.cofinance.andro.utils.enums.AccountTransferType
import id.andriawan24.cofinance.andro.utils.enums.TransactionCategory
import id.andriawan24.cofinance.andro.utils.ext.FORMAT_ISO_8601
import id.andriawan24.cofinance.andro.utils.ext.formatToString
import id.andriawan24.cofinance.andro.utils.ext.toDate
import id.andriawan24.cofinance.domain.model.request.AddTransactionParam
import id.andriawan24.cofinance.domain.model.request.GetTransactionsParam
import id.andriawan24.cofinance.domain.model.response.Account
import id.andriawan24.cofinance.domain.usecase.accounts.GetAccountsUseCase
import id.andriawan24.cofinance.domain.usecase.transaction.CreateTransactionUseCase
import id.andriawan24.cofinance.domain.usecase.transaction.GetTransactionsUseCase
import id.andriawan24.cofinance.utils.enums.TransactionType
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.internal.toLongOrDefault
import java.util.Date

@Stable
data class AddNewUiState(
    val amount: String = emptyString(),
    val fee: String = emptyString(),
    val includeFee: Boolean = false,
    val imageUri: Uri? = null,
    val accountChooserType: AccountTransferType = AccountTransferType.SENDER,
    val senderAccount: Account? = null,
    val receiverAccount: Account? = null,
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val expenseCategory: TransactionCategory? = null,
    val incomeCategory: TransactionCategory? = null,
    val dateTime: Date = Date(),
    val notes: String = emptyString(),
    val isValid: Boolean = false,
    val accounts: List<Account> = emptyList(),
    val loadingAccount: Boolean = false,
    val isLoading: Boolean = false,
    val transactionId: String? = null
)

@Stable
data class AddNewDialogState(
    val showCategoryBottomSheet: Boolean = false,
    val showDateBottomSheet: Boolean = false,
    var showAccountBottomSheet: Boolean = false,
    var showAddAccountBottomSheet: Boolean = false,
    var showTimePickerDialog: Boolean = false,
)

sealed class AddNewDialogEvent {
    data class ToggleCategoryDialog(val isShow: Boolean) : AddNewDialogEvent()
    data class ToggleDatePickerDialog(val isShow: Boolean) : AddNewDialogEvent()
    data class ToggleTimePickerDialog(val isShow: Boolean) : AddNewDialogEvent()
    data class ToggleAccountDialog(val isShow: Boolean) : AddNewDialogEvent()
    data class ToggleAddAccountDialog(val isShow: Boolean) : AddNewDialogEvent()
}

sealed class AddNewUiEvent {
    data object OnBackPressed : AddNewUiEvent()
    data object OnPictureClicked : AddNewUiEvent()
    data object UpdateAccount : AddNewUiEvent()
    data class SetTransactionType(val type: TransactionType) : AddNewUiEvent()
    data object SaveTransaction : AddNewUiEvent()
    data class SetIncludeFee(val includeFee: Boolean) : AddNewUiEvent()
    data class SetAmount(val amount: String) : AddNewUiEvent()
    data class SetCategory(val category: TransactionCategory) : AddNewUiEvent()
    data class SetAccount(val account: Account) : AddNewUiEvent()
    data class SetReceiverAccount(val account: Account) : AddNewUiEvent()
    data class SetDateTime(val dateTime: Date) : AddNewUiEvent()
    data class SetFee(val fee: String) : AddNewUiEvent()
    data class SetNote(val note: String) : AddNewUiEvent()
    data class SetAccountChooserType(val accountTransferType: AccountTransferType) : AddNewUiEvent()
}

class AddNewViewModel(
    private val getAccountsUseCase: GetAccountsUseCase,
    private val createTransactionUseCase: CreateTransactionUseCase,
    private val getTransactionsUseCase: GetTransactionsUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(AddNewUiState())
    val uiState = _uiState.asStateFlow()

    private val _dialogState = MutableStateFlow(AddNewDialogState())
    val dialogState = _dialogState.asStateFlow()

    private val _onSuccessSaved = Channel<None>(Channel.BUFFERED)
    val onSuccessSaved = _onSuccessSaved.receiveAsFlow()

    private val _showMessage = Channel<String>(Channel.BUFFERED)
    val showMessage = _showMessage.receiveAsFlow()

    init {
        getAccounts()
    }

    private fun getAccounts() {
        _uiState.value = uiState.value.copy(loadingAccount = true)
        viewModelScope.launch {
            getAccountsUseCase.execute().collectLatest {
                if (it.isSuccess) {
                    _uiState.update { currentState ->
                        currentState.copy(
                            accounts = it.getOrDefault(emptyList()),
                            loadingAccount = false
                        )
                    }
                }

                if (it.isFailure) {
                    _uiState.update { currentState -> currentState.copy(loadingAccount = false) }
                    _showMessage.send(it.exceptionOrNull()?.message.orEmpty())
                }
            }
        }
    }

    fun getDraftedTransaction(id: String) {
        viewModelScope.launch {
            getTransactionsUseCase.execute(GetTransactionsParam(transactionId = id, isDraft = true))
                .collectLatest { response ->
                    if (response.isSuccess) {
                        response.getOrNull()?.firstOrNull()?.let { transaction ->
                            _uiState.update {
                                it.copy(
                                    transactionId = transaction.id,
                                    amount = it.amount.ifBlank { transaction.amount.toString() },
                                    dateTime = transaction.date.toDate()
                                )
                            }
                        }
                    }
                }
        }
    }

    fun onDialogEvent(dialogEvent: AddNewDialogEvent) {
        when (dialogEvent) {
            is AddNewDialogEvent.ToggleCategoryDialog -> {
                _dialogState.update { it.copy(showCategoryBottomSheet = dialogEvent.isShow) }
            }

            is AddNewDialogEvent.ToggleDatePickerDialog -> {
                _dialogState.update { it.copy(showDateBottomSheet = dialogEvent.isShow) }
            }

            is AddNewDialogEvent.ToggleAccountDialog -> {
                _dialogState.update { it.copy(showAccountBottomSheet = dialogEvent.isShow) }
            }

            is AddNewDialogEvent.ToggleAddAccountDialog -> {
                _dialogState.update { it.copy(showAddAccountBottomSheet = dialogEvent.isShow) }
            }

            is AddNewDialogEvent.ToggleTimePickerDialog -> {
                _dialogState.update { it.copy(showTimePickerDialog = dialogEvent.isShow) }
            }
        }
    }

    fun onEvent(event: AddNewUiEvent) {
        when (event) {
            is AddNewUiEvent.SetIncludeFee -> _uiState.update {
                it.copy(
                    includeFee = event.includeFee,
                    fee = if (!event.includeFee) emptyString() else it.fee
                )
            }

            is AddNewUiEvent.SetAmount -> {
                _uiState.update { it.copy(amount = event.amount) }
                validateInputs()
            }

            is AddNewUiEvent.SetFee -> {
                _uiState.update { it.copy(fee = event.fee) }
                validateInputs()
            }

            is AddNewUiEvent.SetCategory -> {
                _uiState.update {
                    if (it.transactionType == TransactionType.INCOME) {
                        it.copy(incomeCategory = event.category)
                    } else {
                        it.copy(expenseCategory = event.category)
                    }
                }
                validateInputs()
            }

            is AddNewUiEvent.SetAccount -> {
                _uiState.update { it.copy(senderAccount = event.account) }
                validateInputs()
            }

            is AddNewUiEvent.SetReceiverAccount -> {
                _uiState.update { it.copy(receiverAccount = event.account) }
                validateInputs()
            }

            is AddNewUiEvent.SetDateTime -> {
                _uiState.update { it.copy(dateTime = event.dateTime) }
                validateInputs()
            }

            is AddNewUiEvent.SetNote -> _uiState.update {
                it.copy(notes = event.note)
            }

            is AddNewUiEvent.UpdateAccount -> getAccounts()
            is AddNewUiEvent.SetTransactionType -> _uiState.update {
                it.copy(transactionType = event.type)
            }

            is AddNewUiEvent.SaveTransaction -> viewModelScope.launch {
                _uiState.value = uiState.value.copy(isLoading = true)

                val category = when (uiState.value.transactionType) {
                    TransactionType.EXPENSE -> uiState.value.expenseCategory?.name.orEmpty()
                    else -> uiState.value.incomeCategory?.name.orEmpty()
                }

                val param = AddTransactionParam(
                    id = uiState.value.transactionId,
                    amount = uiState.value.amount.toLong(),
                    category = category,
                    date = uiState.value.dateTime.formatToString(FORMAT_ISO_8601),
                    fee = uiState.value.fee.toLongOrDefault(0),
                    notes = uiState.value.notes,
                    accountsId = uiState.value.senderAccount?.id,
                    receiverAccountsId = uiState.value.receiverAccount?.id,
                    type = uiState.value.transactionType,
                    isDraft = false
                )

                createTransactionUseCase.execute(param).collectLatest {
                    if (it.isSuccess) {
                        _uiState.value = uiState.value.copy(isLoading = false)
                        _onSuccessSaved.send(None)
                    } else {
                        _uiState.value = uiState.value.copy(isLoading = false)
                        _showMessage.send(it.exceptionOrNull()?.message.orEmpty())
                    }
                }
            }

            is AddNewUiEvent.SetAccountChooserType -> _uiState.update {
                it.copy(accountChooserType = event.accountTransferType)
            }

            is AddNewUiEvent.OnBackPressed,
            is AddNewUiEvent.OnPictureClicked -> Unit
        }
    }

    private fun validateInputs() {
        _uiState.update { currentState ->
            val isCategoryValid = when (currentState.transactionType) {
                TransactionType.EXPENSE -> currentState.expenseCategory != null
                TransactionType.INCOME -> currentState.incomeCategory != null
                else -> true
            }

            val isValid = currentState.amount.isNotBlank() &&
                    isCategoryValid && currentState.senderAccount?.id.orEmpty().isNotBlank()

            currentState.copy(isValid = isValid)
        }
    }
}