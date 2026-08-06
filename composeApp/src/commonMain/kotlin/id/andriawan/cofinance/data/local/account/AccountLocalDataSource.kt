package id.andriawan.cofinance.data.local.account

import id.andriawan.cofinance.data.model.response.AccountResponse
import kotlinx.coroutines.flow.Flow

/** Durable local account persistence contract. */
interface AccountLocalDataSource {
    fun watchAccounts(): Flow<List<AccountResponse>>
    suspend fun getAccounts(): List<AccountResponse>

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

    suspend fun upsertAccounts(accounts: List<AccountResponse>)

    /** Wipes all locally persisted accounts, e.g. on logout. */
    suspend fun clearAccounts()
}
