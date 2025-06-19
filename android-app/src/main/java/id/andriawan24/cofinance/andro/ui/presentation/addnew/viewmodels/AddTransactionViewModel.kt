package id.andriawan24.cofinance.andro.ui.presentation.addnew.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.andriawan24.cofinance.andro.utils.None
import id.andriawan24.cofinance.andro.utils.emptyString
import id.andriawan24.cofinance.andro.utils.enums.ExpenseCategory
import id.andriawan24.cofinance.domain.model.request.AddTransactionParam
import id.andriawan24.cofinance.domain.model.response.Account
import id.andriawan24.cofinance.domain.usecase.accounts.GetAccountsUseCase
import id.andriawan24.cofinance.domain.usecase.transaction.CreateTransactionUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.internal.toLongOrDefault
import java.util.Date

data class AddNewUiState(
    var amount: String = emptyString(),
    var fee: String = emptyString(),
    var includeFee: Boolean = false,
    var imageUri: Uri? = null,
    var account: Account? = null,
    var category: ExpenseCategory? = null,
    var dateTime: Date = Date(),
    var notes: String = emptyString(),
    var isValid: Boolean = false,
    var accounts: List<Account> = emptyList(),
    var isLoading: Boolean = false
)

sealed class AddNewUiEvent {
    data object OnBackPressed : AddNewUiEvent()
    data object OnPictureClicked : AddNewUiEvent()
    data object UpdateAccount : AddNewUiEvent()
    data object SaveTransaction : AddNewUiEvent()
    data class SetIncludeFee(val includeFee: Boolean) : AddNewUiEvent()
    data class SetAmount(val amount: String) : AddNewUiEvent()
    data class SetCategory(val category: ExpenseCategory) : AddNewUiEvent()
    data class SetAccount(val account: Account) : AddNewUiEvent()
    data class SetDateTime(val dateTime: Date) : AddNewUiEvent()
    data class SetFee(val fee: String) : AddNewUiEvent()
    data class SetNote(val note: String) : AddNewUiEvent()
}

class AddNewViewModel(
    private val getAccountsUseCase: GetAccountsUseCase,
    private val createTransactionUseCase: CreateTransactionUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(AddNewUiState())
    val uiState = _uiState.asStateFlow()

    private val _onSuccessSaved = Channel<None>(Channel.BUFFERED)
    val onSuccessSaved = _onSuccessSaved.receiveAsFlow()

    private val _showMessage = Channel<String>(Channel.BUFFERED)
    val showMessage = _showMessage.receiveAsFlow()

    init {
        getAccounts()
    }

    private fun getAccounts() {
        viewModelScope.launch {
            getAccountsUseCase.execute().collectLatest {
                if (it.isSuccess) {
                    _uiState.update { currentState ->
                        currentState.copy(accounts = it.getOrDefault(emptyList()))
                    }
                }
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
                _uiState.update { it.copy(category = event.category) }
                validateInputs()
            }

            is AddNewUiEvent.SetAccount -> {
                _uiState.update { it.copy(account = event.account) }
                validateInputs()
            }

            is AddNewUiEvent.SetDateTime -> {
                _uiState.update { it.copy(dateTime = event.dateTime) }
                validateInputs()
            }

            is AddNewUiEvent.SetNote -> _uiState.update { it.copy(notes = event.note) }
            is AddNewUiEvent.UpdateAccount -> getAccounts()

            is AddNewUiEvent.SaveTransaction -> {
                viewModelScope.launch {
                    _uiState.update { it.copy(isLoading = true) }
                    val param = AddTransactionParam(
                        amount = uiState.value.amount.toLong(),
                        category = uiState.value.category?.name.orEmpty(),
                        date = uiState.value.dateTime.toString(),
                        fee = uiState.value.fee.toLongOrDefault(0),
                        notes = uiState.value.notes,
                        accountsId = uiState.value.account?.id ?: 0
                    )

                    createTransactionUseCase.execute(param).collectLatest {
                        if (it.isSuccess) {
                            _uiState.update { currentState ->
                                currentState.copy(isLoading = false)
                            }

                            _onSuccessSaved.send(None)
                        } else {
                            _uiState.update { currentState ->
                                currentState.copy(isLoading = false)
                            }
                            _showMessage.send(it.exceptionOrNull()?.message.orEmpty())
                        }
                    }
                }
            }

            is AddNewUiEvent.OnBackPressed,
            is AddNewUiEvent.OnPictureClicked -> Unit
        }
    }

    private fun validateInputs() {
        _uiState.update { currentState ->
            val isValid = currentState.amount.isNotBlank() &&
                    currentState.category != null &&
                    currentState.account != null
            currentState.copy(isValid = isValid)
        }
    }
}