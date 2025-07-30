package id.andriawan24.cofinance.andro.ui.presentation.addnew.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.andriawan24.cofinance.andro.utils.emptyString
import id.andriawan24.cofinance.domain.model.request.AccountParam
import id.andriawan24.cofinance.domain.usecase.accounts.AddAccountUseCase
import id.andriawan24.cofinance.utils.None
import id.andriawan24.cofinance.utils.enums.AccountGroupType
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddAccountUiState(
    val category: AccountGroupType? = null,
    val name: String = emptyString(),
    val amount: String = emptyString(),
    val openCategoryChooser: Boolean = false,
    val isLoading: Boolean = false
)

class AddAccountViewModel(private val addAccountUseCase: AddAccountUseCase) : ViewModel() {
    private val _uiState = MutableStateFlow(AddAccountUiState())
    val uiState: StateFlow<AddAccountUiState> = _uiState.asStateFlow()

    private val _closeBottomSheet = Channel<None>(Channel.BUFFERED)
    val closeBottomSheet = _closeBottomSheet.receiveAsFlow()

    private val _showMessage = Channel<String>(Channel.BUFFERED)
    val showMessage = _showMessage.receiveAsFlow()

    fun openCategoryChooser() {
        _uiState.update { it.copy(openCategoryChooser = true) }
    }

    fun closeCategoryChooser() {
        _uiState.update { it.copy(openCategoryChooser = false) }
    }

    fun onCategorySelected(category: AccountGroupType) {
        _uiState.update { it.copy(category = category) }
    }

    fun onNameChanged(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    fun onAmountChanged(amount: String) {
        _uiState.value = _uiState.value.copy(amount = amount)
    }

    fun saveAccount() {
        val category = _uiState.value.category
        val name = _uiState.value.name
        val amount = _uiState.value.amount

        if (category != null && name.isNotBlank() && amount.isNotBlank()) {
            val account = AccountParam(name = name, balance = amount.toInt(), group = category.name)

            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }

                addAccountUseCase.execute(account).collectLatest {
                    if (it.isSuccess) {
                        _uiState.update { state -> state.copy(isLoading = false) }
                        _closeBottomSheet.send(None)
                    }

                    if (it.isFailure) {
                        _showMessage.send(it.exceptionOrNull()?.message.orEmpty())
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
            }
        }
    }
}