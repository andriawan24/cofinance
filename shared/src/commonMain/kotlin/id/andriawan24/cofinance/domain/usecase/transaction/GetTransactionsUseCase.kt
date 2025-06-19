package id.andriawan24.cofinance.domain.usecase.transaction

import id.andriawan24.cofinance.data.repository.TransactionRepository
import id.andriawan24.cofinance.domain.model.request.GetTransactionsParam
import id.andriawan24.cofinance.domain.model.response.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class GetTransactionsUseCase(private val transactionRepository: TransactionRepository) {
    fun execute(param: GetTransactionsParam): Flow<Result<List<Transaction>>> = flow {
        try {
            val response = transactionRepository.getTransactions(param = param)
            emit(Result.success(response))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)
}