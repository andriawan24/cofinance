package id.andriawan.cofinance.data.repository

import id.andriawan.cofinance.data.local.account.AccountLocalDataSource
import id.andriawan.cofinance.data.sync.FinanceSyncCoordinator
import id.andriawan.cofinance.domain.model.request.AccountParam
import id.andriawan.cofinance.domain.model.response.Account
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid


interface AccountRepository {
    suspend fun getAccounts(): List<Account>
    fun watchAccounts(): Flow<List<Account>>
    suspend fun addAccount(param: AccountParam)
    suspend fun updateAccountBalance(accountId: String, delta: Long)
    suspend fun updateAccountType(accountId: String, accountType: String)
    suspend fun updateAccount(
        accountId: String,
        name: String,
        balance: Long,
        group: String,
        accountType: String
    )

    suspend fun deleteAccount(accountId: String)
}


class AccountRepositoryImpl(
    private val database: AccountLocalDataSource,
    private val syncCoordinator: FinanceSyncCoordinator
) : AccountRepository {

    override suspend fun getAccounts(): List<Account> {
        return database.getAccounts().map { Account.from(it) }
    }

    override fun watchAccounts(): Flow<List<Account>> {
        return database.watchAccounts().map { accounts ->
            accounts.map { Account.from(it) }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun addAccount(param: AccountParam) {
        database.insertAccount(
            id = Uuid.random().toString(),
            name = param.name,
            group = param.group,
            balance = param.balance,
            accountType = param.accountType
        )
        syncCoordinator.mirrorAllIfSignedIn()
    }

    override suspend fun updateAccountBalance(accountId: String, delta: Long) {
        database.updateAccountBalance(accountId, delta)
        syncCoordinator.mirrorAllIfSignedIn()
    }

    override suspend fun updateAccountType(accountId: String, accountType: String) {
        database.updateAccountType(accountId, accountType)
        syncCoordinator.mirrorAllIfSignedIn()
    }

    override suspend fun updateAccount(
        accountId: String,
        name: String,
        balance: Long,
        group: String,
        accountType: String
    ) {
        database.updateAccount(accountId, name, balance, group, accountType)
        syncCoordinator.mirrorAllIfSignedIn()
    }

    override suspend fun deleteAccount(accountId: String) {
        database.deleteAccount(accountId)
        syncCoordinator.mirrorAllIfSignedIn()
    }
}
