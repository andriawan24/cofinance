package id.andriawan24.cofinance.domain.usecase.transaction

import id.andriawan24.cofinance.data.repository.TransactionRepository
import id.andriawan24.cofinance.domain.model.request.GetTransactionsParam
import id.andriawan24.cofinance.domain.model.response.TransactionByDate
import id.andriawan24.cofinance.utils.ResultState
import id.andriawan24.cofinance.utils.enums.TransactionType
import id.andriawan24.cofinance.utils.ext.toInstant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
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
                    val date = it.date.toInstant().toLocalDateTime(TimeZone.of("UTC+7"))
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
        }.flowOn(Dispatchers.IO)
}