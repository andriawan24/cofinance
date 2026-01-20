package id.andriawan.cofinance.pages.account

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.andriawan.cofinance.domain.model.response.AccountByGroup
import id.andriawan.cofinance.domain.usecases.accounts.GetAccountsUseCase
import id.andriawan.cofinance.utils.ResultState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Stable
data class UiState(
    val accounts: List<AccountByGroup> = listOf(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val balance: Long = 0L
)

@Stable
class AccountViewModel(private val getAccountsUseCase: GetAccountsUseCase) : ViewModel() {
    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    init {
        getAccounts()
    }

    fun refresh() {
        _uiState.update {
            it.copy(isRefreshing = true)
        }

        getAccounts()
    }

    fun getAccounts() {
        viewModelScope.launch {
            getAccountsUseCase.execute().collectLatest {
                when (it) {
                    ResultState.Loading -> {
                        _uiState.update { state ->
                            state.copy(isLoading = false)
                        }
                    }

                    is ResultState.Error -> {
                        _uiState.update { state ->
                            state.copy(
                                isLoading = false,
                                isRefreshing = false
                            )
                        }
                    }

                    is ResultState.Success<List<AccountByGroup>> -> {
                        val totalAssets = it.data.sumOf { data ->
                            data.accounts.sumOf { account -> account.balance }
                        }

                        _uiState.update { state ->
                            state.copy(
                                accounts = it.data,
                                isLoading = false,
                                isRefreshing = false,
                                balance = totalAssets
                            )
                        }
                    }
                }
            }
        }
    }
}
