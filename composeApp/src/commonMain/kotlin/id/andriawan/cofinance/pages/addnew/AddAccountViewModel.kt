package id.andriawan.cofinance.pages.addnew

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.andriawan.cofinance.domain.model.request.AccountParam
import id.andriawan.cofinance.domain.usecases.accounts.AddAccountUseCase
import id.andriawan.cofinance.utils.None
import id.andriawan.cofinance.utils.emptyString
import id.andriawan.cofinance.utils.enums.AccountGroupType
import id.andriawan.cofinance.utils.extensions.isDigitOnly
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddAccountUiState(
    val category: AccountGroupType = AccountGroupType.CASH,
    val name: String = emptyString(),
    val amount: String = emptyString(),
    val openCategoryChooser: Boolean = false,
    val isLoading: Boolean = false
)

sealed interface AddAccountEvent {
    data object OpenCategoryChooser : AddAccountEvent
    data object CloseCategoryChooser : AddAccountEvent
    data class CategoryChosen(val category: AccountGroupType) : AddAccountEvent
    data class NameChanged(val name: String) : AddAccountEvent
    data class AmountChanged(val amount: String) : AddAccountEvent
    data object SaveAccount : AddAccountEvent
    data object BackClicked : AddAccountEvent
}


class AddAccountViewModel(private val addAccountUseCase: AddAccountUseCase) : ViewModel() {
    private val _uiState = MutableStateFlow(AddAccountUiState())
    val uiState: StateFlow<AddAccountUiState> = _uiState.asStateFlow()

    private val _accountAdded = Channel<None>(Channel.BUFFERED)
    val accountAdded = _accountAdded.receiveAsFlow()

    private val _showMessage = Channel<String>(Channel.BUFFERED)
    val showMessage = _showMessage.receiveAsFlow()

    fun onEvent(event: AddAccountEvent) {
        when (event) {
            is AddAccountEvent.OpenCategoryChooser -> openCategoryChooser()
            is AddAccountEvent.CloseCategoryChooser -> closeCategoryChooser()
            is AddAccountEvent.CategoryChosen -> onCategorySelected(event.category)
            is AddAccountEvent.NameChanged -> onNameChanged(event.name)
            is AddAccountEvent.AmountChanged -> onAmountChanged(event.amount)
            is AddAccountEvent.SaveAccount -> saveAccount()
            is AddAccountEvent.BackClicked -> { /* no-op */
            }
        }
    }

    fun openCategoryChooser() {
        _uiState.update { it.copy(openCategoryChooser = true) }
    }

    private fun closeCategoryChooser() {
        _uiState.update { it.copy(openCategoryChooser = false) }
    }

    private fun onCategorySelected(category: AccountGroupType) {
        _uiState.update { it.copy(category = category) }
    }

    private fun onNameChanged(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    private fun onAmountChanged(amount: String) {
        if (amount.isDigitOnly() && amount.length < 13) {
            _uiState.update { it.copy(amount = amount) }
        }
    }

    private fun saveAccount() {
        viewModelScope.launch {
            val category = _uiState.value.category
            val name = _uiState.value.name
            val amount = _uiState.value.amount.toLongOrNull() ?: 0

            if (name.isNotBlank()) {
                val account = AccountParam(
                    name = name,
                    balance = amount,
                    group = category.name
                )

                _uiState.update { it.copy(isLoading = true) }

                addAccountUseCase.execute(account).collectLatest {
                    if (it.isSuccess) {
                        _uiState.update { state -> state.copy(isLoading = false) }
                        _accountAdded.send(None)
                    }

                    if (it.isFailure) {
                        _showMessage.send(it.exceptionOrNull()?.message.orEmpty())
                        _uiState.update { state -> state.copy(isLoading = false) }
                    }
                }
            } else {
                _showMessage.send("Name is required")
            }
        }
    }
}
