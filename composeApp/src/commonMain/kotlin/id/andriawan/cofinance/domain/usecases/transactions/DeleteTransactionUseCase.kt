package id.andriawan.cofinance.domain.usecases.transactions

import id.andriawan.cofinance.data.repository.TransactionRepository
import id.andriawan.cofinance.utils.None
import id.andriawan.cofinance.utils.ResultState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class DeleteTransactionUseCase(private val transactionRepository: TransactionRepository) {
    fun execute(id: String): Flow<ResultState<None>> = flow {
        emit(ResultState.Loading)

        try {
            transactionRepository.deleteTransaction(id)
            emit(ResultState.Success(None))
        } catch (e: Exception) {
            emit(ResultState.Error(e))
        }
    }
}
