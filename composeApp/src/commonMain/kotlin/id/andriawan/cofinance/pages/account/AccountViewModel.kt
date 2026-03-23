package id.andriawan.cofinance.pages.account

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diamondedge.logging.logging
import id.andriawan.cofinance.domain.model.response.AccountByGroup
import id.andriawan.cofinance.domain.usecases.accounts.GetAccountsUseCase
import id.andriawan.cofinance.utils.ResultState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import id.andriawan.cofinance.utils.collectResult
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
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            getAccountsUseCase.execute().collectResult(
                onLoading = {
                    _uiState.update { state -> state.copy(isLoading = true) }
                },
                onError = {
                    _uiState.update { state ->
                        state.copy(isLoading = false, isRefreshing = false)
                    }
                },
                onSuccess = { data ->
                    val totalAssets = data.sumOf { group ->
                        group.accounts.sumOf { account -> account.balance }
                    }

                    _uiState.update { state ->
                        state.copy(
                            accounts = data,
                            isLoading = false,
                            isRefreshing = false,
                            balance = totalAssets
                        )
                    }
                }
            )
        }
    }
}
