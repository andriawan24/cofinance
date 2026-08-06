package id.andriawan.cofinance.data.remote

import id.andriawan.cofinance.data.model.response.TransactionResponse

interface TransactionRemoteDataSource {
    suspend fun getTransactions(): List<TransactionResponse>
    suspend fun upsertTransactions(transactions: List<TransactionResponse>)
}
