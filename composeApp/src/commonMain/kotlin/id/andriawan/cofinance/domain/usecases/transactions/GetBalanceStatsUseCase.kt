package id.andriawan.cofinance.domain.usecases.transactions

import id.andriawan.cofinance.data.repository.TransactionRepository
import id.andriawan.cofinance.domain.model.request.GetTransactionsParam
import id.andriawan.cofinance.domain.model.response.BalanceStats
import id.andriawan.cofinance.utils.ResultState
import id.andriawan.cofinance.utils.enums.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class GetBalanceStatsUseCase(private val transactionRepository: TransactionRepository) {
    fun execute(param: GetTransactionsParam): Flow<ResultState<BalanceStats>> =
        flow {
            emit(ResultState.Loading)
            try {
                val response = transactionRepository.getTransactions(param)

                val income = response.filter {
                    it.type == TransactionType.INCOME
                }.sumOf { it.amount }

                val expense = response.filter {
                    it.type == TransactionType.EXPENSE
                }.sumOf { it.amount }

                val balance = income - expense

                emit(
                    ResultState.Success(
                        BalanceStats(
                            income = income,
                            expenses = expense,
                            balance = balance
                        )
                    )
                )
            } catch (e: Exception) {
                emit(ResultState.Error(e))
            }
        }
}
