package id.andriawan.cofinance.data.local

import androidx.room.ConstructedBy
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.Update
import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import id.andriawan.cofinance.data.model.response.AccountResponse
import id.andriawan.cofinance.data.model.response.TransactionResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

@Entity(tableName = "accounts")
data class LocalAccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val group: String,
    val balance: Long,
    val accountType: String,
    val createdAt: String
)

@Entity(tableName = "transactions")
data class LocalTransactionEntity(
    @PrimaryKey val id: String,
    val amount: Long,
    val category: String,
    val date: String,
    val fee: Long,
    val notes: String,
    val senderAccountId: String,
    val receiverAccountId: String?,
    val type: String,
    val createdAt: String,
    val updatedAt: String
)

@Dao
interface LocalFinanceDao {
    @Query("SELECT * FROM accounts ORDER BY createdAt DESC")
    fun watchAccounts(): Flow<List<LocalAccountEntity>>

    @Query("SELECT * FROM accounts ORDER BY createdAt DESC")
    suspend fun getAccounts(): List<LocalAccountEntity>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccount(id: String): LocalAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAccount(account: LocalAccountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAccounts(accounts: List<LocalAccountEntity>)

    @Update
    suspend fun updateAccount(account: LocalAccountEntity)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteAccount(id: String)

    @Query("DELETE FROM accounts")
    suspend fun deleteAllAccounts()

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun watchTransactions(): Flow<List<LocalTransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    suspend fun getTransactions(): List<LocalTransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransaction(id: String): LocalTransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTransaction(transaction: LocalTransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTransactions(transactions: List<LocalTransactionEntity>)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()
}

@Database(
    entities = [LocalAccountEntity::class, LocalTransactionEntity::class],
    version = 1,
    exportSchema = true
)
@ConstructedBy(CofinanceRoomDatabaseConstructor::class)
abstract class CofinanceRoomDatabase : RoomDatabase() {
    abstract fun financeDao(): LocalFinanceDao
}

expect object CofinanceRoomDatabaseConstructor : RoomDatabaseConstructor<CofinanceRoomDatabase> {
    override fun initialize(): CofinanceRoomDatabase
}

class RoomCofinanceDatabase(
    private val roomDatabase: CofinanceRoomDatabase
) : CofinanceDatabase {
    private val dao = roomDatabase.financeDao()

    override fun watchAccounts(): Flow<List<AccountResponse>> =
        dao.watchAccounts().mapAccounts()

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
        roomDatabase.useWriterConnection {
            it.immediateTransaction {
                val account = dao.getAccount(accountId) ?: error("Account does not exist")
                dao.updateAccount(account.copy(balance = account.balance + delta))
            }
        }
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
            it.copy(
                name = name,
                balance = balance,
                group = group,
                accountType = accountType
            )
        }
    }

    override suspend fun deleteAccount(accountId: String) {
        dao.deleteAccount(accountId)
    }

    override fun watchTransactions(
        startDate: String?,
        endDate: String?,
        isDraft: Boolean,
        transactionId: String?
    ): Flow<List<TransactionResponse>> = combine(
        dao.watchTransactions(),
        dao.watchAccounts()
    ) { transactions, accounts ->
        hydrate(transactions, accounts, startDate, endDate, isDraft, transactionId)
    }

    override suspend fun getTransactions(
        startDate: String?,
        endDate: String?,
        isDraft: Boolean,
        transactionId: String?
    ): List<TransactionResponse> = hydrate(
        dao.getTransactions(), dao.getAccounts(), startDate, endDate, isDraft, transactionId
    )

    override suspend fun getAllTransactions(): List<TransactionResponse> {
        val accounts = dao.getAccounts().associateBy(LocalAccountEntity::id)
        return dao.getTransactions().map { transaction ->
            transaction.toResponse(
                sender = accounts[transaction.senderAccountId]?.toResponse(),
                receiver = transaction.receiverAccountId?.let(accounts::get)?.toResponse()
            )
        }
    }

    override suspend fun insertTransaction(
        id: String,
        amount: Long,
        category: String,
        date: String,
        fee: Long,
        notes: String,
        accountsId: String,
        receiverAccountsId: String?,
        type: String
    ): TransactionResponse {
        val now = kotlin.time.Clock.System.now().toString()
        val entity = LocalTransactionEntity(
            id, amount, category, date, fee, notes, accountsId, receiverAccountsId, type, now, now
        )
        roomDatabase.useWriterConnection {
            it.immediateTransaction {
                applyBalanceDeltas(entity)
                dao.upsertTransaction(entity)
            }
        }
        return getTransactions(transactionId = id, isDraft = type == TYPE_DRAFT).first()
    }

    override suspend fun updateTransaction(
        id: String,
        amount: Long,
        category: String,
        date: String,
        fee: Long,
        notes: String,
        accountsId: String,
        receiverAccountsId: String?,
        type: String
    ): TransactionResponse {
        roomDatabase.useWriterConnection {
            it.immediateTransaction {
                val old = dao.getTransaction(id) ?: error("Transaction does not exist")
                val replacement = old.copy(
                    amount = amount,
                    category = category,
                    date = date,
                    fee = fee,
                    notes = notes,
                    senderAccountId = accountsId,
                    receiverAccountId = receiverAccountsId,
                    type = type,
                    updatedAt = kotlin.time.Clock.System.now().toString()
                )
                applyBalanceDeltas(old, -1)
                applyBalanceDeltas(replacement)
                dao.upsertTransaction(replacement)
            }
        }
        return getTransactions(transactionId = id, isDraft = type == TYPE_DRAFT).first()
    }

    override suspend fun upsertAccounts(accounts: List<AccountResponse>) {
        if (accounts.isNotEmpty()) dao.upsertAccounts(accounts.map(AccountResponse::toEntity))
    }

    override suspend fun upsertTransactions(transactions: List<TransactionResponse>) {
        if (transactions.isNotEmpty()) dao.upsertTransactions(transactions.map(TransactionResponse::toEntity))
    }

    override suspend fun clearAll() {
        roomDatabase.useWriterConnection {
            it.immediateTransaction {
                dao.deleteAllTransactions()
                dao.deleteAllAccounts()
            }
        }
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

    private suspend fun applyBalanceDeltas(
        transaction: LocalTransactionEntity,
        multiplier: Long = 1
    ) {
        balanceDeltas(transaction).forEach { (accountId, delta) ->
            val account = dao.getAccount(accountId) ?: error("Account does not exist")
            dao.updateAccount(account.copy(balance = account.balance + delta * multiplier))
        }
    }

    private fun hydrate(
        transactions: List<LocalTransactionEntity>,
        accounts: List<LocalAccountEntity>,
        startDate: String? = null,
        endDate: String? = null,
        isDraft: Boolean = false,
        transactionId: String? = null
    ): List<TransactionResponse> {
        val accountMap = accounts.associateBy(LocalAccountEntity::id)
        return transactions.filter { transaction ->
            (transactionId == null || transaction.id == transactionId) &&
                    (startDate == null || endDate == null || transaction.date >= startDate && transaction.date < endDate) &&
                    if (transactionId != null) true
                    else if (isDraft) transaction.type == TYPE_DRAFT
                    else transaction.type != TYPE_DRAFT && transaction.type != TYPE_CYCLE_RESET
        }.map { transaction ->
            transaction.toResponse(
                sender = accountMap[transaction.senderAccountId]?.toResponse(),
                receiver = transaction.receiverAccountId?.let(accountMap::get)?.toResponse()
            )
        }
    }

    private fun balanceDeltas(transaction: LocalTransactionEntity): Map<String, Long> {
        val deltas = mutableMapOf<String, Long>()
        fun add(id: String?, delta: Long) {
            if (!id.isNullOrBlank()) deltas[id] = deltas.getOrElse(id) { 0 } + delta
        }
        when (transaction.type) {
            TYPE_INCOME -> add(transaction.senderAccountId, transaction.amount)
            TYPE_EXPENSE -> add(
                transaction.senderAccountId,
                -(transaction.amount + transaction.fee)
            )

            TYPE_TRANSFER -> {
                add(transaction.senderAccountId, -(transaction.amount + transaction.fee))
                add(transaction.receiverAccountId, transaction.amount)
            }
        }
        return deltas
    }

    private fun Flow<List<LocalAccountEntity>>.mapAccounts(): Flow<List<AccountResponse>> =
        map { accounts -> accounts.map(LocalAccountEntity::toResponse) }

    companion object {
        private const val TYPE_INCOME = "INCOME"
        private const val TYPE_EXPENSE = "EXPENSE"
        private const val TYPE_TRANSFER = "TRANSFER"
        private const val TYPE_DRAFT = "DRAFT"
        private const val TYPE_CYCLE_RESET = "CYCLE_RESET"
    }
}

private fun LocalAccountEntity.toResponse() =
    AccountResponse(id, name, group, balance, accountType, createdAt)

private fun AccountResponse.toEntity() = LocalAccountEntity(
    id.orEmpty(),
    name.orEmpty(),
    group.orEmpty(),
    balance ?: 0,
    accountType.orEmpty(),
    createdAt.orEmpty()
)

private fun LocalTransactionEntity.toResponse(
    sender: AccountResponse?,
    receiver: AccountResponse?
) = TransactionResponse(
    id = id,
    amount = amount,
    category = category,
    date = date,
    fee = fee,
    notes = notes,
    senderAccountId = senderAccountId,
    receiverAccountId = receiverAccountId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    type = type,
    sender = sender,
    receiver = receiver
)

private fun TransactionResponse.toEntity() = LocalTransactionEntity(
    id.orEmpty(),
    amount ?: 0,
    category.orEmpty(),
    date.orEmpty(),
    fee ?: 0,
    notes.orEmpty(),
    senderAccountId.orEmpty(),
    receiverAccountId,
    type.orEmpty(),
    createdAt.orEmpty(),
    updatedAt.orEmpty()
)
