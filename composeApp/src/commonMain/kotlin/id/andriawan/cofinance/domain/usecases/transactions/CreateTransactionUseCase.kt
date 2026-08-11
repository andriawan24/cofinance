package id.andriawan.cofinance.domain.usecases.transactions

import id.andriawan.cofinance.data.repository.TransactionRepository
import id.andriawan.cofinance.domain.model.request.AddTransactionParam
import id.andriawan.cofinance.domain.model.response.Transaction
import id.andriawan.cofinance.utils.ResultState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class CreateTransactionUseCase(private val transactionRepository: TransactionRepository) {
    fun execute(params: AddTransactionParam): Flow<ResultState<Transaction>> = flow {
        emit(ResultState.Loading)

        try {
            val response = transactionRepository.createTransaction(params)
            emit(ResultState.Success(response))
        } catch (e: Exception) {
            emit(ResultState.Error(e))
        }
    }
}
