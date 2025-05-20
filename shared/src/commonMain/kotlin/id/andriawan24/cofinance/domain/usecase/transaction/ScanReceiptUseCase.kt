package id.andriawan24.cofinance.domain.usecase.transaction

import id.andriawan24.cofinance.data.repository.TransactionRepository
import id.andriawan24.cofinance.domain.model.response.ReceiptScan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class ScanReceiptUseCase(private val transactionRepository: TransactionRepository) {
    fun execute(image: ByteArray): Flow<Result<ReceiptScan>> = flow {
        try {
            val response = transactionRepository.scanReceipt(image)
            emit(Result.success(response))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)
}