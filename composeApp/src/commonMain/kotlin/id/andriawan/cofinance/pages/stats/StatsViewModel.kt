package id.andriawan.cofinance.pages.stats

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diamondedge.logging.logging
import id.andriawan.cofinance.domain.model.request.GetTransactionsParam
import id.andriawan.cofinance.domain.usecases.authentications.GetUserUseCase
import id.andriawan.cofinance.domain.usecases.transactions.GetTransactionsUseCase
import id.andriawan.cofinance.utils.emptyString
import id.andriawan.cofinance.utils.enums.TransactionCategory
import id.andriawan.cofinance.utils.extensions.computeCycleDateRange
import id.andriawan.cofinance.utils.extensions.getCurrentCycleMonth
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import id.andriawan.cofinance.utils.collectResult
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
    val year: Int = 0,
    val month: Int = 0,
    val cycleStartDay: Int = 1,
    val dateLabel: String = "",
    var isLoading: Boolean = true,
    var message: String = emptyString(),
    var totalExpenses: Long = 0L,
    var stats: List<StatItem> = emptyList()
)

sealed class StatsUiEvent {
    data object OnNextMonth : StatsUiEvent()
    data object OnPreviousMonth : StatsUiEvent()
}


class StatsViewModel(
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val getUserUseCase: GetUserUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState = _uiState.asStateFlow()

    private var fetchJob: Job? = null

    init {
        val user = getUserUseCase.execute()
        val cycleStartDay = user.cycleStartDay
        val (month, year) = getCurrentCycleMonth(cycleStartDay)
        val range = computeCycleDateRange(month, year, cycleStartDay)

        _uiState.value = StatsUiState(
            year = year,
            month = month,
            cycleStartDay = cycleStartDay,
            dateLabel = range.label
        )

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
                    nextYear++
                    JANUARY
                } else currentMonth + 1

                val range = computeCycleDateRange(nextMonth, nextYear, uiState.value.cycleStartDay)
                _uiState.value = uiState.value.copy(
                    month = nextMonth,
                    year = nextYear,
                    dateLabel = range.label,
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
                    nextYear--
                    DECEMBER
                } else currentMonth - 1

                val range = computeCycleDateRange(nextMonth, nextYear, uiState.value.cycleStartDay)
                _uiState.value = uiState.value.copy(
                    month = nextMonth,
                    year = nextYear,
                    dateLabel = range.label
                )

                fetchTransaction()
            }
        }
    }

    private fun fetchTransaction() {
        viewModelScope.launch {
            val range = computeCycleDateRange(uiState.value.month, uiState.value.year, uiState.value.cycleStartDay)
            val param = GetTransactionsParam(startDate = range.startDate, endDate = range.endDate)

            getTransactionsUseCase.execute(param = param).collectResult(
                onLoading = {
                    _uiState.value = uiState.value.copy(isLoading = true)
                },
                onError = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = exception.message.orEmpty()
                    )
                },
                onSuccess = { data ->
                    val totalSum = data.sumOf { transaction -> transaction.amount }

                    val transactionsByCategory = data
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
            )
        }
    }

    companion object {
        private const val FETCH_DELAY = 500L
        private const val JANUARY = 1
        private const val DECEMBER = 12
        val logger = logging()
    }
}
