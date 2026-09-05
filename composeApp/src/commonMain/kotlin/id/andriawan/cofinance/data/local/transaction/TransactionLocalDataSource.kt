package id.andriawan.cofinance.data.local.transaction

import id.andriawan.cofinance.data.model.TransactionResponse
import kotlinx.coroutines.flow.Flow

/** Durable local transaction persistence contract. */
interface TransactionLocalDataSource {
    fun watchTransactions(
        startDate: String? = null,
        endDate: String? = null,
        isDraft: Boolean = false,
        transactionId: String? = null,
        expenseOnly: Boolean? = null
    ): Flow<List<TransactionResponse>>

    suspend fun getTransactions(
        startDate: String? = null,
        endDate: String? = null,
        isDraft: Boolean = false,
        transactionId: String? = null
    ): List<TransactionResponse>

    suspend fun getAllTransactions(): List<TransactionResponse>

    suspend fun updateTransaction(
        id: String,
        amount: Long,
        category: String,
        date: String,
        fee: Long,
        notes: String,
        accountsId: String,
        receiverAccountsId: String?,
        type: String
    ): TransactionResponse

    suspend fun insertTransaction(
        id: String,
        amount: Long,
        category: String,
        date: String,
        fee: Long,
        notes: String,
        accountsId: String,
        receiverAccountsId: String?,
        type: String
    ): TransactionResponse

    suspend fun upsertTransactions(transactions: List<TransactionResponse>)

    /** Removes one transaction and gives back the balance it took from its accounts. */
    suspend fun deleteTransaction(id: String)

    /**
     * Removes every transaction that names [accountId] as sender or receiver, returning their
     * identifiers so the caller can carry the same removals to the cloud mirror.
     */
    suspend fun deleteTransactionsForAccount(accountId: String): List<String>

    /** Wipes all locally persisted transactions, e.g. on logout. */
    suspend fun clearTransactions()
}
