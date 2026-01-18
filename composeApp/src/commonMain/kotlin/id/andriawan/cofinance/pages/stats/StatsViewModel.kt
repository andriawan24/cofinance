package id.andriawan.cofinance.pages.stats

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diamondedge.logging.logging
import id.andriawan.cofinance.domain.model.request.GetTransactionsParam
import id.andriawan.cofinance.domain.model.response.Transaction
import id.andriawan.cofinance.domain.usecases.transactions.GetTransactionsUseCase
import id.andriawan.cofinance.utils.ResultState
import id.andriawan.cofinance.utils.emptyString
import id.andriawan.cofinance.utils.enums.TransactionCategory
import id.andriawan.cofinance.utils.extensions.getCurrentMonth
import id.andriawan.cofinance.utils.extensions.getCurrentYear
import id.andriawan.cofinance.utils.extensions.getMonthLabel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Stable
data class StatItem(
    val category: TransactionCategory,
    val amount: Long,
    val percentage: Float,
    val sweepAngle: Float,
)

data class StatsUiState(
    val year: Int = getCurrentYear(),
    val month: Int = getCurrentMonth(),
    val monthString: String = getMonthLabel(month),
    var isLoading: Boolean = true,
    var message: String = emptyString(),
    var totalExpenses: Long = 0L,
    var stats: List<StatItem> = emptyList()
)

sealed class StatsUiEvent {
    data object OnNextMonth : StatsUiEvent()
    data object OnPreviousMonth : StatsUiEvent()
}


class StatsViewModel(private val getTransactionsUseCase: GetTransactionsUseCase) : ViewModel() {
    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState = _uiState.asStateFlow()

    private var fetchJob: Job? = null

    init {
        viewModelScope.launch {
            fetchTransaction()
        }
    }

    fun onEvent(event: StatsUiEvent) {
        when (event) {
            StatsUiEvent.OnNextMonth -> {
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
                    isLoading = true
                )

                fetchJob?.cancel()
                fetchJob = viewModelScope.launch {
                    delay(FETCH_DELAY)
                    fetchTransaction()
                }
            }

            StatsUiEvent.OnPreviousMonth -> {
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
                    year = nextYear
                )

                fetchTransaction()
            }
        }
    }

    private fun fetchTransaction() {
        viewModelScope.launch {
            val param = GetTransactionsParam(month = uiState.value.month, year = uiState.value.year)

            getTransactionsUseCase.execute(param = param).collectLatest { result ->
                when (result) {
                    ResultState.Loading -> {
                        _uiState.value = uiState.value.copy(isLoading = true)
                    }

                    is ResultState.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            message = result.exception.message.orEmpty()
                        )
                    }

                    is ResultState.Success<List<Transaction>> -> {
                        val totalSum = result.data.sumOf { transaction -> transaction.amount }

                        val transactionsByCategory = result.data
                            .groupBy { TransactionCategory.getCategoryByName(it.category) }
                            .mapValues { (_, list) -> list.sumOf { it.amount } }

                        val statItems = transactionsByCategory.map { entry ->
                            val sweepAngle = 360 * entry.value / totalSum.toFloat()
                            val percentage = (sweepAngle / 360) * 100

                            StatItem(
                                percentage = percentage,
                                category = entry.key,
                                sweepAngle = sweepAngle,
                                amount = entry.value
                            )
                        }

                        _uiState.update { state ->
                            state.copy(
                                isLoading = false,
                                totalExpenses = totalSum,
                                stats = statItems
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
        val logger = logging()
    }
}
