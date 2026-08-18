package id.andriawan.cofinance.data.local.account

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import id.andriawan.cofinance.data.local.CofinanceRoomDatabase
import id.andriawan.cofinance.data.model.entity.LocalAccountEntity
import id.andriawan.cofinance.data.model.response.AccountResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomAccountLocalDataSource(
    private val roomDatabase: CofinanceRoomDatabase
) : AccountLocalDataSource {
    private val dao = roomDatabase.accountDao()

    override fun watchAccounts(): Flow<List<AccountResponse>> =
        dao.watchAccounts().map { accounts -> accounts.map(LocalAccountEntity::toResponse) }

    override suspend fun getAccounts(): List<AccountResponse> =
        dao.getAccounts().map(LocalAccountEntity::toResponse)

    override suspend fun insertAccount(
        id: String,
        name: String,
        group: String,
        balance: Long,
        accountType: String
    ) {
        dao.upsertAccount(
            LocalAccountEntity(
                id,
                name,
                group,
                balance,
                accountType,
                kotlin.time.Clock.System.now().toString()
            )
        )
    }

    override suspend fun updateAccountBalance(accountId: String, delta: Long) {
        mutateAccount(accountId) { it.copy(balance = it.balance + delta) }
    }

    override suspend fun updateAccountType(accountId: String, accountType: String) {
        mutateAccount(accountId) { it.copy(accountType = accountType) }
    }

    override suspend fun updateAccount(
        accountId: String,
        name: String,
        balance: Long,
        group: String,
        accountType: String
    ) {
        mutateAccount(accountId) {
            it.copy(name = name, balance = balance, group = group, accountType = accountType)
        }
    }

    override suspend fun deleteAccount(accountId: String) {
        dao.deleteAccount(accountId)
    }

    override suspend fun upsertAccounts(accounts: List<AccountResponse>) {
        if (accounts.isNotEmpty()) dao.upsertAccounts(accounts.map(AccountResponse::toEntity))
    }

    override suspend fun clearAccounts() {
        dao.deleteAllAccounts()
    }

    private suspend fun mutateAccount(
        id: String,
        transform: (LocalAccountEntity) -> LocalAccountEntity
    ) {
        roomDatabase.useWriterConnection {
            it.immediateTransaction {
                dao.updateAccount(transform(dao.getAccount(id) ?: error("Account does not exist")))
            }
        }
    }
}

fun LocalAccountEntity.toResponse() =
    AccountResponse(id, name, group, balance, accountType, createdAt)

fun AccountResponse.toEntity() = LocalAccountEntity(
    id.orEmpty(),
    name.orEmpty(),
    group.orEmpty(),
    balance ?: 0,
    accountType.orEmpty(),
    createdAt.orEmpty()
)
