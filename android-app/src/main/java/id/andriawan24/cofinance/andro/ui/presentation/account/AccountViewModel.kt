package id.andriawan24.cofinance.andro.ui.presentation.account

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.andriawan24.cofinance.andro.R
import id.andriawan24.cofinance.andro.ui.presentation.account.models.AccountByGroup
import id.andriawan24.cofinance.domain.usecase.accounts.GetAccountsUseCase
import id.andriawan24.cofinance.utils.enums.AccountGroupType
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
            _uiState.value = _uiState.value.copy(isLoading = true)
            getAccountsUseCase.execute().collectLatest {
                val accounts = it.getOrDefault(emptyList()).groupBy { account -> account.group }
                var totalAssets = 0L

                val accountsByGroup = accounts.map { accountByGroup ->
                    val backgroundColor = when (accountByGroup.key) {
                        AccountGroupType.CASH -> Color(0xFFEEF9F8)
                        AccountGroupType.DEBIT -> Color(0xFFEFFAFD)
                        AccountGroupType.CREDIT -> Color(0xFFEFFAFD)
                        AccountGroupType.SAVINGS -> Color(0xFFFFF4FD)
                    }
                    val displayName = accountByGroup.key.displayName
                    val totalAmount = accountByGroup.value.sumOf { account -> account.balance }
                    totalAssets += totalAmount
                    AccountByGroup(
                        groupLabel = displayName,
                        backgroundColor = backgroundColor,
                        imageRes = when (accountByGroup.key) {
                            AccountGroupType.CASH -> R.drawable.ic_money
                            AccountGroupType.DEBIT -> R.drawable.ic_card
                            AccountGroupType.CREDIT -> R.drawable.ic_card
                            AccountGroupType.SAVINGS -> R.drawable.ic_saving
                        },
                        totalAmount = totalAmount,
                        accounts = accountByGroup.value
                    )
                }

                _uiState.update { state ->
                    state.copy(
                        accounts = accountsByGroup,
                        isLoading = false,
                        isRefreshing = false,
                        balance = totalAssets
                    )
                }
            }
        }
    }
}