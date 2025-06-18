package id.andriawan24.cofinance.domain.usecase.transaction

import id.andriawan24.cofinance.data.repository.TransactionRepository
import id.andriawan24.cofinance.domain.model.request.TransactionParam
import id.andriawan24.cofinance.domain.model.response.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class CreateTransactionUseCase(private val transactionRepository: TransactionRepository) {
    fun execute(params: TransactionParam): Flow<Result<Transaction>> = flow {
        try {
            val response = transactionRepository.createTransaction(params)
            emit(Result.success(response))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)
}