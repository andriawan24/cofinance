package id.andriawan24.cofinance.andro.ui.presentation.activity

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.andriawan24.cofinance.andro.utils.emptyString
import id.andriawan24.cofinance.andro.utils.ext.getCurrentMonth
import id.andriawan24.cofinance.andro.utils.ext.getCurrentYear
import id.andriawan24.cofinance.andro.utils.ext.getMonthLabel
import id.andriawan24.cofinance.domain.model.request.GetTransactionsParam
import id.andriawan24.cofinance.domain.model.response.BalanceStats
import id.andriawan24.cofinance.domain.model.response.TransactionByDate
import id.andriawan24.cofinance.domain.usecase.transaction.GetBalanceStatsUseCase
import id.andriawan24.cofinance.domain.usecase.transaction.GetTransactionsGroupByMonthUseCase
import id.andriawan24.cofinance.utils.ResultState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@Stable
data class ActivityUiState(
    val year: Int = getCurrentYear(),
    val month: Int = getCurrentMonth(),
    val monthString: String = getMonthLabel(month),
    val balance: Long = 0,
    val income: Long = 0,
    val expense: Long = 0,
    val transactions: List<TransactionByDate> = emptyList(),
    var isLoading: Boolean = true,
    var message: String = emptyString()
)

sealed class ActivityUiEvent {
    data object OnNextMonth : ActivityUiEvent()
    data object OnPreviousMonth : ActivityUiEvent()
}


class ActivityViewModel(
    private val getTransactionsGroupByMonthUseCase: GetTransactionsGroupByMonthUseCase,
    private val getBalanceStateUseCase: GetBalanceStatsUseCase
) :
    ViewModel() {
    private val _uiState = MutableStateFlow(ActivityUiState())
    val uiState = _uiState.asStateFlow()

    fun onEvent(event: ActivityUiEvent) {
        when (event) {
            ActivityUiEvent.OnNextMonth -> {
                val currentMonth = uiState.value.month
                var nextYear = uiState.value.year
                val nextMonth = if (currentMonth == DECEMBER) {
                    // Move to the next year started from January
                    nextYear++
                    JANUARY
                } else currentMonth + 1

                _uiState.value = uiState.value.copy(
                    month = nextMonth,
                    monthString = getMonthLabel(nextMonth),
                    year = nextYear,
                    transactions = emptyList()
                )

                getBalance()
                fetchTransaction()
            }

            ActivityUiEvent.OnPreviousMonth -> {
                val currentMonth = uiState.value.month
                var nextYear = uiState.value.year
                val nextMonth = if (currentMonth == JANUARY) {
                    // Move to the previous year started from December
                    nextYear--
                    DECEMBER
                } else currentMonth - 1

                _uiState.value = uiState.value.copy(
                    month = nextMonth,
                    monthString = getMonthLabel(nextMonth),
                    year = nextYear,
                    transactions = emptyList()
                )

                getBalance()
                fetchTransaction()
            }
        }
    }

    fun getBalance() {
        viewModelScope.launch {
            val param = GetTransactionsParam(month = uiState.value.month, year = uiState.value.year)

            getBalanceStateUseCase.execute(param).collectLatest {
                when (it) {
                    ResultState.Loading -> {
                        // TODO: Handle loading
                    }

                    is ResultState.Error -> TODO()
                    is ResultState.Success<BalanceStats> -> {
                        _uiState.update { state ->
                            state.copy(
                                balance = it.data.balance,
                                expense = it.data.expenses,
                                income = it.data.income
                            )
                        }
                    }
                }
            }
        }
    }

    fun fetchTransaction() {
        viewModelScope.launch {
            val param = GetTransactionsParam(month = uiState.value.month, year = uiState.value.year)

            getTransactionsGroupByMonthUseCase.execute(param = param).collectLatest { result ->
                when (result) {
                    ResultState.Loading -> {
                        _uiState.value = uiState.value.copy(isLoading = true)
                    }

                    is ResultState.Success<List<TransactionByDate>> -> {
                        _uiState.value = uiState.value.copy(
                            isLoading = false,
                            transactions = result.data
                        )
                    }

                    is ResultState.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            message = result.exception.message.orEmpty()
                        )
                    }
                }
            }
        }
    }

    companion object {
        private const val JANUARY = 1
        private const val DECEMBER = 12
    }
}