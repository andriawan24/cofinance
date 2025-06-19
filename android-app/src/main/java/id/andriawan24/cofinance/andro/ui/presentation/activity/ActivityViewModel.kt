package id.andriawan24.cofinance.andro.ui.presentation.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.andriawan24.cofinance.andro.utils.emptyString
import id.andriawan24.cofinance.andro.utils.ext.formatToString
import id.andriawan24.cofinance.andro.utils.ext.getCurrentMonth
import id.andriawan24.cofinance.andro.utils.ext.getCurrentYear
import id.andriawan24.cofinance.andro.utils.ext.getMonthLabel
import id.andriawan24.cofinance.andro.utils.ext.toDate
import id.andriawan24.cofinance.domain.model.request.GetTransactionsParam
import id.andriawan24.cofinance.domain.model.response.Transaction
import id.andriawan24.cofinance.domain.usecase.transaction.GetTransactionsUseCase
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TransactionByDate(
    val dateLabel: String = emptyString(),
    val totalAmount: Long = 0L,
    val transactions: List<Transaction> = emptyList()
)

data class ActivityUiState(
    val year: Int = getCurrentYear(),
    val month: Int = getCurrentMonth(),
    val monthString: String = getMonthLabel(month),
    val balance: Long = 0,
    val income: Long = 0,
    val expense: Long = 0,
    val transactions: Map<String, List<Transaction>> = emptyMap(),
    var isLoading: Boolean = false,
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
                var nextMonth = if (currentMonth == 12) {
                    nextYear++
                    1
                } else {
                    currentMonth + 1
                }

                _uiState.value = uiState.value.copy(
                    month = nextMonth,
                    monthString = getMonthLabel(nextMonth),
                    year = nextYear
                )

                fetchTransaction()
            }

            ActivityUiEvent.OnPreviousMonth -> {
                val currentMonth = uiState.value.month

                var nextYear = uiState.value.year
                var nextMonth = if (currentMonth == 1) {
                    nextYear--
                    12
                } else {
                    currentMonth - 1
                }

                _uiState.value = uiState.value.copy(
                    month = nextMonth,
                    monthString = getMonthLabel(nextMonth),
                    year = nextYear
                )

                fetchTransaction()
            }
        }
    }

    fun fetchTransaction() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true)
            }

            val param = GetTransactionsParam(month = uiState.value.month, year = uiState.value.year)

            withContext(Dispatchers.IO) {
                getTransactionsUseCase.execute(param = param).collectLatest { result ->
                    if (result.isSuccess) {
                        val transactionByDate = result.getOrNull().orEmpty().groupBy {
                            it.date.toDate().formatToString("EEE, dd MM yyyy")
                        }

                        val expense = result.getOrNull()?.sumOf { it.amount } ?: 0

                        Napier.d { "Transaction by date $transactionByDate" }

                        withContext(Dispatchers.Main) {
                            _uiState.value = uiState.value.copy(
                                isLoading = false,
                                transactions = transactionByDate,
                                expense = expense
                            )
                        }
                    }

                    if (result.isFailure) {
                        withContext(Dispatchers.Main) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                message = result.exceptionOrNull()?.message.orEmpty()
                            )
                        }
                    }
                }
            }
        }
    }
}