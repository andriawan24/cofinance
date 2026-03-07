package id.andriawan.cofinance.domain.usecases.transactions

import id.andriawan.cofinance.data.repository.TransactionRepository
import id.andriawan.cofinance.domain.model.request.GetTransactionsParam
import id.andriawan.cofinance.domain.model.response.BalanceStats
import id.andriawan.cofinance.utils.ResultState
import id.andriawan.cofinance.utils.enums.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

class GetBalanceStatsUseCase(private val transactionRepository: TransactionRepository) {
    fun execute(param: GetTransactionsParam): Flow<ResultState<BalanceStats>> {
        return transactionRepository.watchTransactions(param)
            .map { response ->
                val income = response.filter {
                    it.type == TransactionType.INCOME
                }.sumOf { it.amount }

                val expense = response.filter {
                    it.type == TransactionType.EXPENSE
                }.sumOf { it.amount }

                val balance = income - expense

                ResultState.Success(
                    BalanceStats(
                        income = income,
                        expenses = expense,
                        balance = balance
                    )
                ) as ResultState<BalanceStats>
            }
            .onStart { emit(ResultState.Loading) }
            .catch { emit(ResultState.Error(it as Exception)) }
    }
}
