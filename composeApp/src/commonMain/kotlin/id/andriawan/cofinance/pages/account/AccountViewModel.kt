package id.andriawan.cofinance.pages.account

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.andriawan.cofinance.data.repository.AccountRepository
import id.andriawan.cofinance.domain.model.response.Account
import id.andriawan.cofinance.domain.model.response.AccountByGroup
import id.andriawan.cofinance.domain.usecases.accounts.DeleteAccountUseCase
import id.andriawan.cofinance.domain.usecases.accounts.GetAccountsUseCase
import id.andriawan.cofinance.utils.enums.AccountGroupType.Companion.getBackgroundColor
import id.andriawan.cofinance.utils.enums.AccountGroupType
import id.andriawan.cofinance.utils.enums.AccountType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import id.andriawan.cofinance.utils.collectResult
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Stable
data class UiState(
    val accounts: List<AccountByGroup> = listOf(),
    val assetAccounts: List<AccountByGroup> = listOf(),
    val regularAccounts: List<AccountByGroup> = listOf(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val balance: Long = 0L,
    val editingAccount: Account? = null
)

@Stable
class AccountViewModel(
    private val getAccountsUseCase: GetAccountsUseCase,
    private val accountRepository: AccountRepository,
    private val deleteAccountUseCase: DeleteAccountUseCase
) : ViewModel() {
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
                    _uiState.update { state -> buildUiState(state, data) }
                }
            )
        }
    }

    fun refreshAccounts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }

            runCatching { accountRepository.getAccounts() }
                .onSuccess { accounts ->
                    val groupedAccounts = accounts
                        .groupBy { it.group }
                        .map { (groupType, accountsByGroup) ->
                            AccountByGroup(
                                groupLabel = groupType.displayName,
                                backgroundColor = groupType.getBackgroundColor(),
                                totalAmount = accountsByGroup.sumOf { it.balance },
                                accountGroupType = groupType,
                                accounts = accountsByGroup
                            )
                        }

                    _uiState.update { state -> buildUiState(state, groupedAccounts) }
                }
                .onFailure {
                    _uiState.update { it.copy(isRefreshing = false) }
                }
        }
    }

    fun onAccountClicked(account: Account) {
        _uiState.update { it.copy(editingAccount = account) }
    }

    fun onDismissEditAccount() {
        _uiState.update { it.copy(editingAccount = null) }
    }

    fun onSaveAccount(accountId: String, name: String, balance: Long, group: AccountGroupType, accountType: AccountType) {
        viewModelScope.launch {
            accountRepository.updateAccount(accountId, name, balance, group.name, accountType.name)
            _uiState.update { it.copy(editingAccount = null) }
            refreshAccounts()
        }
    }

    fun onDeleteAccount(accountId: String) {
        viewModelScope.launch {
            deleteAccountUseCase.execute(accountId).collect { }
            _uiState.update { it.copy(editingAccount = null) }
            refreshAccounts()
        }
    }

    private fun buildUiState(
        currentState: UiState,
        groupedAccounts: List<AccountByGroup>
    ): UiState {
        val totalAssets = groupedAccounts.sumOf { group ->
            group.accounts.sumOf { account -> account.balance }
        }

        val allAccounts = groupedAccounts.flatMap { it.accounts }
        val assetAccountsList = allAccounts.filter { it.accountType == AccountType.ASSET }
        val regularAccountsList = allAccounts.filter { it.accountType == AccountType.REGULAR_BALANCE }

        fun groupAccounts(accounts: List<Account>): List<AccountByGroup> {
            return accounts.groupBy { it.group }.map { (groupType, accountsByType) ->
                AccountByGroup(
                    groupLabel = groupType.displayName,
                    backgroundColor = groupType.getBackgroundColor(),
                    totalAmount = accountsByType.sumOf { it.balance },
                    accountGroupType = groupType,
                    accounts = accountsByType
                )
            }
        }

        return currentState.copy(
            accounts = groupedAccounts,
            assetAccounts = groupAccounts(assetAccountsList),
            regularAccounts = groupAccounts(regularAccountsList),
            isLoading = false,
            isRefreshing = false,
            balance = totalAssets
        )
    }
}
