package id.andriawan.cofinance.domain.usecases.transactions

import id.andriawan.cofinance.data.repository.TransactionRepository
import id.andriawan.cofinance.domain.model.request.GetTransactionsParam
import id.andriawan.cofinance.domain.model.response.Transaction
import id.andriawan.cofinance.utils.ResultState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

class GetTransactionsUseCase(private val transactionRepository: TransactionRepository) {
    fun execute(param: GetTransactionsParam): Flow<ResultState<List<Transaction>>> {
        return transactionRepository.watchTransactions(param)
            .map<List<Transaction>, ResultState<List<Transaction>>> { ResultState.Success(it) }
            .onStart { emit(ResultState.Loading) }
            .catch { emit(ResultState.Error(it as Exception)) }
    }
}
