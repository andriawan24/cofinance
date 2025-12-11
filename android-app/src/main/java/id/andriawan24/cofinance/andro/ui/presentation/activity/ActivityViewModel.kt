package id.andriawan24.cofinance.andro.ui.presentation.activity

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.andriawan24.cofinance.andro.ui.presentation.activity.models.TransactionByDate
import id.andriawan24.cofinance.andro.utils.LocaleHelper
import id.andriawan24.cofinance.andro.utils.emptyString
import id.andriawan24.cofinance.andro.utils.ext.FORMAT_DAY_MONTH_YEAR
import id.andriawan24.cofinance.andro.utils.ext.formatToString
import id.andriawan24.cofinance.andro.utils.ext.getCurrentMonth
import id.andriawan24.cofinance.andro.utils.ext.getCurrentYear
import id.andriawan24.cofinance.andro.utils.ext.getMonthLabel
import id.andriawan24.cofinance.andro.utils.ext.toDate
import id.andriawan24.cofinance.domain.model.request.GetTransactionsParam
import id.andriawan24.cofinance.domain.model.response.Transaction
import id.andriawan24.cofinance.domain.usecase.transaction.GetTransactionsUseCase
import id.andriawan24.cofinance.utils.ResultState
import id.andriawan24.cofinance.utils.enums.TransactionType
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
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

class ActivityViewModel(private val getTransactionsUseCase: GetTransactionsUseCase) : ViewModel() {
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
                    transactions = emptyList(),
                    isLoading = true
                )

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

                fetchTransaction()
            }
        }
    }

    fun fetchTransaction() {
        viewModelScope.launch {
            val param = GetTransactionsParam(month = uiState.value.month, year = uiState.value.year)

            withContext(Dispatchers.IO) {
                getTransactionsUseCase.execute(param = param).collectLatest { result ->
                    when (result) {
                        ResultState.Loading -> {
                            _uiState.value = uiState.value.copy(isLoading = true)
                        }

                        is ResultState.Success<List<Transaction>> -> {
                            val transactionGrouped = result.data.orEmpty().groupBy {
                                it.date.toDate()
                                    .formatToString(
                                        format = FORMAT_DAY_MONTH_YEAR,
                                        locale = LocaleHelper.getCurrentLocale()
                                    )
                            }

                            var expense = 0L
                            var income = 0L
                            val transactionByDate = mutableListOf<TransactionByDate>()

                            transactionGrouped.forEach {
                                val expenseThisMonth = it.value
                                    .filter { transaction -> transaction.type == TransactionType.EXPENSE }
                                    .sumOf { transaction -> transaction.amount }

                                val incomeThisMonth = it.value
                                    .filter { transaction -> transaction.type == TransactionType.INCOME }
                                    .sumOf { transaction -> transaction.amount }

                                expense += expenseThisMonth
                                income += incomeThisMonth

                                transactionByDate.add(
                                    TransactionByDate(
                                        dateLabel = it.key,
                                        transactions = it.value,
                                        totalAmount = expenseThisMonth + incomeThisMonth
                                    )
                                )
                            }

                            _uiState.value = uiState.value.copy(
                                isLoading = false,
                                transactions = transactionByDate,
                                expense = expense,
                                income = income,
                                balance = income - expense
                            )
                        }

                        is ResultState.Error -> {
                            Napier.e { "Failed to get activities: ${result.exception.message}" }

                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                message = result.exception.message.orEmpty()
                            )
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val FETCH_DELAY = 500L
        private const val JANUARY = 1
        private const val DECEMBER = 12
    }
}