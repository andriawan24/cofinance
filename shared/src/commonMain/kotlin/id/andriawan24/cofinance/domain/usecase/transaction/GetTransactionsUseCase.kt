package id.andriawan24.cofinance.domain.usecase.transaction

import id.andriawan24.cofinance.data.repository.TransactionRepository
import id.andriawan24.cofinance.domain.model.request.GetTransactionsParam
import id.andriawan24.cofinance.domain.model.response.Transaction
import id.andriawan24.cofinance.utils.ResultState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class GetTransactionsUseCase(private val transactionRepository: TransactionRepository) {
    fun execute(param: GetTransactionsParam): Flow<ResultState<List<Transaction>>> = flow {
        emit(ResultState.Loading)

        try {
            val response = transactionRepository.getTransactions(param = param)
            emit(ResultState.Success(response))
        } catch (e: Exception) {
            emit(ResultState.Error(e))
        }
    }.flowOn(Dispatchers.IO)
}