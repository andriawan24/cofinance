package id.andriawan24.cofinance.data.repository

import id.andriawan24.cofinance.data.datasource.GeminiDataSource
import id.andriawan24.cofinance.data.datasource.SupabaseDataSource
import id.andriawan24.cofinance.domain.model.request.AddTransactionParam
import id.andriawan24.cofinance.domain.model.request.AddTransactionParam.Companion.toRequest
import id.andriawan24.cofinance.domain.model.request.GetTransactionsParam
import id.andriawan24.cofinance.domain.model.request.GetTransactionsParam.Companion.toRequest
import id.andriawan24.cofinance.domain.model.response.ReceiptScan
import id.andriawan24.cofinance.domain.model.response.Transaction

interface TransactionRepository {
    suspend fun scanReceipt(image: ByteArray): ReceiptScan
    suspend fun getTransactions(param: GetTransactionsParam): List<Transaction>
    suspend fun createTransaction(params: AddTransactionParam): Transaction
}

class TransactionRepositoryImpl(
    private val geminiDataSource: GeminiDataSource,
    private val supabaseDataSource: SupabaseDataSource
) : TransactionRepository {

    override suspend fun scanReceipt(image: ByteArray): ReceiptScan {
        val response = geminiDataSource.scanReceipt(image)
        return ReceiptScan.from(response)
    }

    override suspend fun getTransactions(param: GetTransactionsParam): List<Transaction> {
        val response = supabaseDataSource.getTransactions(param.toRequest())
        return response.map(Transaction::from)
    }

    override suspend fun createTransaction(params: AddTransactionParam): Transaction {
        val response = supabaseDataSource.createTransaction(params.toRequest())
        return Transaction.from(response)
    }
}