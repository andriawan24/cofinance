package id.andriawan.cofinance.domain.usecases.transactions

import id.andriawan.cofinance.data.repository.TransactionRepository
import id.andriawan.cofinance.domain.model.request.GetTransactionsParam
import id.andriawan.cofinance.domain.model.response.TransactionByDate
import id.andriawan.cofinance.utils.ResultState
import id.andriawan.cofinance.utils.enums.TransactionType
import id.andriawan.cofinance.utils.extensions.toDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime

class GetTransactionsGroupByMonthUseCase(private val transactionRepository: TransactionRepository) {
    @OptIn(ExperimentalTime::class)
    fun execute(param: GetTransactionsParam): Flow<ResultState<List<TransactionByDate>>> {
        return transactionRepository.watchTransactions(param)
            .map { response ->
                val transactionGrouped = response.groupBy {
                    val date = it.date.toDate().toLocalDateTime(TimeZone.currentSystemDefault())
                    "${date.year}-${date.month}-${date.day}"
                }

                val transactionByDate: List<TransactionByDate> = transactionGrouped.map {
                    val expenses = it.value
                        .filter { transaction -> transaction.type == TransactionType.EXPENSE }
                        .sumOf { transaction -> transaction.amount }

                    val income = it.value
                        .filter { transaction -> transaction.type == TransactionType.INCOME }
                        .sumOf { transaction -> transaction.amount }

                    TransactionByDate(
                        dateLabel = it.value.first().date,
                        transactions = it.value,
                        totalAmount = expenses + income
                    )
                }

                ResultState.Success(transactionByDate) as ResultState<List<TransactionByDate>>
            }
            .onStart { emit(ResultState.Loading) }
            .catch { emit(ResultState.Error(it as Exception)) }
    }
}
