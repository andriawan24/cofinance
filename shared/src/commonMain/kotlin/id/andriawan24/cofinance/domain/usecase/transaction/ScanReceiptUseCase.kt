package id.andriawan24.cofinance.domain.usecase.transaction

import id.andriawan24.cofinance.data.repository.TransactionRepository
import id.andriawan24.cofinance.domain.model.response.ReceiptScan
import id.andriawan24.cofinance.utils.ResultState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class ScanReceiptUseCase(private val transactionRepository: TransactionRepository) {
    fun execute(image: ByteArray): Flow<ResultState<ReceiptScan>> = flow {
        emit(ResultState.Loading)
        try {
            val response = transactionRepository.scanReceipt(image)
            emit(ResultState.Success(response))
        } catch (e: Exception) {
            emit(ResultState.Error(e))
        }
    }.flowOn(Dispatchers.IO)
}