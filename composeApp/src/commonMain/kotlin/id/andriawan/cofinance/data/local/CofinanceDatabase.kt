package id.andriawan.cofinance.data.local

import id.andriawan.cofinance.data.model.response.AccountResponse
import id.andriawan.cofinance.data.model.response.TransactionResponse
import kotlinx.coroutines.flow.Flow

/** Shared account and transaction persistence contract backed by Cloud Firestore. */
interface CofinanceDatabase {
    // Account reads
    fun watchAccounts(userId: String): Flow<List<AccountResponse>>
    suspend fun getAccounts(userId: String): List<AccountResponse>

    // Account writes
    suspend fun insertAccount(
        id: String,
        name: String,
        group: String,
        balance: Long,
        accountType: String,
        userId: String
    )

    suspend fun updateAccountBalance(accountId: String, delta: Long)

    suspend fun updateAccountType(accountId: String, accountType: String)

    suspend fun updateAccount(accountId: String, name: String, balance: Long, group: String, accountType: String)

    suspend fun deleteAccount(accountId: String)

    // Transaction reads
    fun watchTransactions(
        userId: String,
        startDate: String? = null,
        endDate: String? = null,
        isDraft: Boolean = false,
        transactionId: String? = null
    ): Flow<List<TransactionResponse>>

    suspend fun getTransactions(
        userId: String,
        startDate: String? = null,
        endDate: String? = null,
        isDraft: Boolean = false,
        transactionId: String? = null
    ): List<TransactionResponse>

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
        type: String,
        userId: String
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
        type: String,
        userId: String
    ): TransactionResponse
}
