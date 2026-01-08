package id.andriawan.cofinance.domain.usecases.transactions

import id.andriawan.cofinance.data.repository.TransactionRepository
import id.andriawan.cofinance.domain.model.response.ReceiptScan
import id.andriawan.cofinance.utils.ResultState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ScanReceiptUseCase(private val transactionRepository: TransactionRepository) {
    fun execute(image: ByteArray): Flow<ResultState<ReceiptScan>> = flow {
        emit(ResultState.Loading)
        try {
            val response = transactionRepository.scanReceipt(image)
            emit(ResultState.Success(response))
        } catch (e: Exception) {
            emit(ResultState.Error(e))
        }
    }
}
