package id.andriawan.cofinance.data.local

import id.andriawan.cofinance.data.model.response.AccountResponse
import id.andriawan.cofinance.data.model.response.TransactionResponse
import kotlinx.coroutines.flow.Flow

/** Durable local account and transaction persistence contract. */
interface CofinanceDatabase {
    // Account reads
    fun watchAccounts(): Flow<List<AccountResponse>>
    suspend fun getAccounts(): List<AccountResponse>

    // Account writes
    suspend fun insertAccount(
        id: String,
        name: String,
        group: String,
        balance: Long,
        accountType: String
    )

    suspend fun updateAccountBalance(accountId: String, delta: Long)

    suspend fun updateAccountType(accountId: String, accountType: String)

    suspend fun updateAccount(accountId: String, name: String, balance: Long, group: String, accountType: String)

    suspend fun deleteAccount(accountId: String)

    // Transaction reads
    fun watchTransactions(
        startDate: String? = null,
        endDate: String? = null,
        isDraft: Boolean = false,
        transactionId: String? = null
    ): Flow<List<TransactionResponse>>

    suspend fun getTransactions(
        startDate: String? = null,
        endDate: String? = null,
        isDraft: Boolean = false,
        transactionId: String? = null
    ): List<TransactionResponse>

    suspend fun getAllTransactions(): List<TransactionResponse>

    // Transaction writes
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

    suspend fun upsertAccounts(accounts: List<AccountResponse>)
    suspend fun upsertTransactions(transactions: List<TransactionResponse>)

    /** Wipes all locally persisted accounts and transactions, e.g. on logout. */
    suspend fun clearAll()
}
