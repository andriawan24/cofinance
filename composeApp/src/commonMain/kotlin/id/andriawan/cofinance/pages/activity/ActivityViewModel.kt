package id.andriawan.cofinance.pages.activity

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diamondedge.logging.logging
import id.andriawan.cofinance.domain.model.request.GetTransactionsParam
import id.andriawan.cofinance.domain.model.response.TransactionByDate
import id.andriawan.cofinance.domain.usecases.authentications.GetUserUseCase
import id.andriawan.cofinance.domain.usecases.transactions.GetBalanceStatsUseCase
import id.andriawan.cofinance.domain.usecases.transactions.GetTransactionsGroupByMonthUseCase
import id.andriawan.cofinance.data.repository.AccountRepository
import id.andriawan.cofinance.utils.emptyString
import id.andriawan.cofinance.utils.enums.AccountType
import id.andriawan.cofinance.utils.extensions.computeCycleDateRange
import id.andriawan.cofinance.utils.extensions.getCurrentCycleMonth
import id.andriawan.cofinance.utils.extensions.isCycleBoundaryPassed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import id.andriawan.cofinance.utils.collectResult
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


@Stable
data class ActivityUiState(
    val year: Int = 0,
    val month: Int = 0,
    val cycleStartDay: Int = 1,
    val dateLabel: String = "",
    val balance: Long = 0,
    val income: Long = 0,
    val expense: Long = 0,
    val transactions: List<TransactionByDate> = emptyList(),
    var isLoading: Boolean = true,
    var message: String = emptyString(),
    val shouldShowCycleReview: Boolean = false
)

sealed class ActivityUiEvent {
    data object OnNextMonth : ActivityUiEvent()
    data object OnPreviousMonth : ActivityUiEvent()
}

@Stable
class ActivityViewModel(
    private val getTransactionsGroupByMonthUseCase: GetTransactionsGroupByMonthUseCase,
    private val getBalanceStateUseCase: GetBalanceStatsUseCase,
    private val getUserUseCase: GetUserUseCase,
    private val accountRepository: AccountRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ActivityUiState())
    val uiState = _uiState.asStateFlow()

    init {
        val user = getUserUseCase.execute()
        val cycleStartDay = user.cycleStartDay
        val (month, year) = getCurrentCycleMonth(cycleStartDay)
        val range = computeCycleDateRange(month, year, cycleStartDay)

        _uiState.value = ActivityUiState(
            year = year,
            month = month,
            cycleStartDay = cycleStartDay,
            dateLabel = range.label
        )
    }

    fun onEvent(event: ActivityUiEvent) {
        when (event) {
            ActivityUiEvent.OnNextMonth -> {
                val currentMonth = uiState.value.month
                var nextYear = uiState.value.year
                val nextMonth = if (currentMonth == DECEMBER) {
                    nextYear++
                    JANUARY
                } else currentMonth + 1

                val range = computeCycleDateRange(nextMonth, nextYear, uiState.value.cycleStartDay)
                _uiState.value = uiState.value.copy(
                    month = nextMonth,
                    year = nextYear,
                    dateLabel = range.label,
                    transactions = emptyList()
                )

                getBalance()
                fetchTransaction()
            }

            ActivityUiEvent.OnPreviousMonth -> {
                val currentMonth = uiState.value.month
                var previousYear = uiState.value.year
                val previousMonth = if (currentMonth == JANUARY) {
                    previousYear--
                    DECEMBER
                } else currentMonth - 1

                val range = computeCycleDateRange(previousMonth, previousYear, uiState.value.cycleStartDay)
                _uiState.value = uiState.value.copy(
                    month = previousMonth,
                    year = previousYear,
                    dateLabel = range.label,
                    transactions = emptyList()
                )

                getBalance()
                fetchTransaction()
            }
        }
    }

    fun refreshCycleSettings() {
        val user = getUserUseCase.execute()
        val cycleStartDay = user.cycleStartDay
        val (month, year) = getCurrentCycleMonth(cycleStartDay)
        val range = computeCycleDateRange(month, year, cycleStartDay)

        _uiState.value = uiState.value.copy(
            month = month,
            year = year,
            cycleStartDay = cycleStartDay,
            dateLabel = range.label,
            transactions = emptyList()
        )

        getBalance()
        fetchTransaction()
    }

    fun getBalance() {
        viewModelScope.launch {
            val range = computeCycleDateRange(uiState.value.month, uiState.value.year, uiState.value.cycleStartDay)
            val param = GetTransactionsParam(startDate = range.startDate, endDate = range.endDate)

            getBalanceStateUseCase.execute(param).collectResult(
                onError = { exception ->
                    log.error { "Error getting transaction: ${exception.message}" }
                },
                onSuccess = { data ->
                    _uiState.update { state ->
                        state.copy(
                            balance = data.balance,
                            expense = data.expenses,
                            income = data.income
                        )
                    }
                }
            )
        }
    }

    fun fetchTransaction() {
        viewModelScope.launch {
            val range = computeCycleDateRange(uiState.value.month, uiState.value.year, uiState.value.cycleStartDay)
            val param = GetTransactionsParam(startDate = range.startDate, endDate = range.endDate)

            getTransactionsGroupByMonthUseCase.execute(param = param).collectResult(
                onLoading = {
                    _uiState.value = uiState.value.copy(isLoading = true)
                },
                onSuccess = { data ->
                    _uiState.value = uiState.value.copy(
                        isLoading = false,
                        transactions = data
                    )
                },
                onError = { exception ->
                    log.error { "Error getting transaction: ${exception.message}" }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = exception.message.orEmpty()
                    )
                }
            )
        }
    }

    fun checkCycleBoundary() {
        viewModelScope.launch {
            try {
                val user = getUserUseCase.execute()
                val cycleStartDay = user.cycleStartDay
                val lastResetDate = user.lastCycleResetDate

                if (isCycleBoundaryPassed(lastResetDate, cycleStartDay)) {
                    // Check if there are any Regular Balance accounts with non-zero balance
                    val accounts = accountRepository.getAccounts()
                    val regularAccountsWithBalance = accounts.filter {
                        it.accountType == AccountType.REGULAR_BALANCE && it.balance != 0L
                    }

                    if (regularAccountsWithBalance.isNotEmpty()) {
                        _uiState.update { it.copy(shouldShowCycleReview = true) }
                    }
                }
            } catch (e: Exception) {
                log.error { "Error checking cycle boundary: ${e.message}" }
            }
        }
    }

    fun onCycleReviewNavigated() {
        _uiState.update { it.copy(shouldShowCycleReview = false) }
    }

    companion object {
        private const val JANUARY = 1
        private const val DECEMBER = 12

        val log = logging(ActivityViewModel::class.simpleName)
    }
}
