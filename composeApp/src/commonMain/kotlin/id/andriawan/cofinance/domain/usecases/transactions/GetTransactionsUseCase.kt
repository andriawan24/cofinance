package id.andriawan.cofinance.domain.usecases.transactions

import id.andriawan.cofinance.data.repository.TransactionRepository
import id.andriawan.cofinance.domain.model.request.GetTransactionsParam
import id.andriawan.cofinance.domain.model.response.Transaction
import id.andriawan.cofinance.utils.ResultState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class GetTransactionsUseCase(private val transactionRepository: TransactionRepository) {
    fun execute(param: GetTransactionsParam): Flow<ResultState<List<Transaction>>> =
        flow {
            emit(ResultState.Loading)
            try {
                val response = transactionRepository.getTransactions(param)
                emit(ResultState.Success(response))
            } catch (e: Exception) {
                emit(ResultState.Error(e))
            }
        }
}
