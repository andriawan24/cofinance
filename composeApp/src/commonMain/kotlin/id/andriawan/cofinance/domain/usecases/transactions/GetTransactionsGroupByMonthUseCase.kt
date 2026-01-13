package id.andriawan.cofinance.domain.usecases.transactions

import id.andriawan.cofinance.data.repository.TransactionRepository
import id.andriawan.cofinance.domain.model.request.GetTransactionsParam
import id.andriawan.cofinance.domain.model.response.TransactionByDate
import id.andriawan.cofinance.utils.ResultState
import id.andriawan.cofinance.utils.enums.TransactionType
import id.andriawan.cofinance.utils.extensions.toDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime

class GetTransactionsGroupByMonthUseCase(private val transactionRepository: TransactionRepository) {
    @OptIn(ExperimentalTime::class)
    fun execute(param: GetTransactionsParam): Flow<ResultState<List<TransactionByDate>>> =
        flow {
            emit(ResultState.Loading)
            try {
                val response = transactionRepository.getTransactions(param = param)
                val transactionGrouped = response.groupBy {
                    val date = it.date.toDate().toLocalDateTime(TimeZone.currentSystemDefault())
                    date.year to date.month.ordinal
                }

                var expense = 0L
                var income = 0L
                val transactionByDate: List<TransactionByDate> = transactionGrouped.map {
                    val expenseThisMonth = it.value
                        .filter { transaction -> transaction.type == TransactionType.EXPENSE }
                        .sumOf { transaction -> transaction.amount }

                    val incomeThisMonth = it.value
                        .filter { transaction -> transaction.type == TransactionType.INCOME }
                        .sumOf { transaction -> transaction.amount }

                    expense += expenseThisMonth
                    income += incomeThisMonth

                    TransactionByDate(
                        dateLabel = it.key,
                        transactions = it.value,
                        totalAmount = expenseThisMonth + incomeThisMonth
                    )
                }

                emit(ResultState.Success(transactionByDate))
            } catch (e: Exception) {
                emit(ResultState.Error(e))
            }
        }
}
