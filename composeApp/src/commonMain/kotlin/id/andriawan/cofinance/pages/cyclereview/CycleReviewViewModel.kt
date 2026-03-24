package id.andriawan.cofinance.pages.cyclereview

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.andriawan.cofinance.data.repository.AccountRepository
import id.andriawan.cofinance.data.repository.AuthenticationRepository
import id.andriawan.cofinance.domain.model.response.Account
import id.andriawan.cofinance.domain.usecases.accounts.ResetAccountBalanceUseCase
import id.andriawan.cofinance.domain.usecases.authentications.GetUserUseCase
import id.andriawan.cofinance.utils.enums.AccountType
import id.andriawan.cofinance.utils.extensions.getPreviousCycleEndDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

data class AccountCarryOverChoice(
    val account: Account,
    val carryOver: Boolean
)

@Stable
data class CycleReviewUiState(
    val accounts: List<AccountCarryOverChoice> = emptyList(),
    val isProcessing: Boolean = false,
    val isCompleted: Boolean = false,
    val cycleEndDateLabel: String = ""
)

class CycleReviewViewModel(
    private val accountRepository: AccountRepository,
    private val resetAccountBalanceUseCase: ResetAccountBalanceUseCase,
    private val getUserUseCase: GetUserUseCase,
    private val authenticationRepository: AuthenticationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CycleReviewUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadAccounts()
    }

    private fun loadAccounts() {
        viewModelScope.launch {
            val accounts = accountRepository.getAccounts()
            val regularAccountsWithBalance = accounts.filter {
                it.accountType == AccountType.REGULAR_BALANCE && it.balance != 0L
            }

            val choices = regularAccountsWithBalance.map { account ->
                AccountCarryOverChoice(
                    account = account,
                    // Default: carry over for positive, start fresh for negative
                    carryOver = account.balance > 0
                )
            }

            val user = getUserUseCase.execute()
            val cycleEndDate = getPreviousCycleEndDate(user.cycleStartDay)

            _uiState.update {
                it.copy(
                    accounts = choices,
                    cycleEndDateLabel = cycleEndDate
                )
            }
        }
    }

    fun toggleCarryOver(accountId: String) {
        _uiState.update { state ->
            state.copy(
                accounts = state.accounts.map {
                    if (it.account.id == accountId) it.copy(carryOver = !it.carryOver)
                    else it
                }
            )
        }
    }

    fun carryAll() {
        _uiState.update { state ->
            state.copy(
                accounts = state.accounts.map { it.copy(carryOver = true) }
            )
        }
    }

    @OptIn(ExperimentalTime::class)
    fun confirm() {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }

            val user = getUserUseCase.execute()
            val cycleEndDate = getPreviousCycleEndDate(user.cycleStartDay)

            // Process each account that chose "start fresh"
            val accountsToReset = _uiState.value.accounts.filter { !it.carryOver }
            for (choice in accountsToReset) {
                resetAccountBalanceUseCase.execute(choice.account, cycleEndDate)
            }

            // Record the reset date in user metadata
            val today = Clock.System.now()
                .toLocalDateTime(TimeZone.currentSystemDefault()).date
            authenticationRepository.updateLastCycleResetDate(today.toString())

            _uiState.update { it.copy(isProcessing = false, isCompleted = true) }
        }
    }
}
