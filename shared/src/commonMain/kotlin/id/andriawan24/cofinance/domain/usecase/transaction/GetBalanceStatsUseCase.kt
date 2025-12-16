package id.andriawan24.cofinance.domain.usecase.transaction

import id.andriawan24.cofinance.data.repository.TransactionRepository
import id.andriawan24.cofinance.domain.model.request.GetTransactionsParam
import id.andriawan24.cofinance.domain.model.response.BalanceStats
import id.andriawan24.cofinance.domain.model.response.Transaction
import id.andriawan24.cofinance.utils.ResultState
import id.andriawan24.cofinance.utils.enums.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn


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
        }.flowOn(Dispatchers.IO)
}