package id.andriawan24.cofinance.data.repository

import id.andriawan24.cofinance.data.datasource.GeminiDataSource
import id.andriawan24.cofinance.domain.model.response.ReceiptScan

interface TransactionRepository {
    suspend fun scanReceipt(image: ByteArray): ReceiptScan
}

class TransactionRepositoryImpl(
    private val geminiDataSource: GeminiDataSource
) : TransactionRepository {
    override suspend fun scanReceipt(image: ByteArray): ReceiptScan {
        val response = geminiDataSource.scanReceipt(image)
        return ReceiptScan.from(response)
    }
}