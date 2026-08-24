package id.andriawan.cofinance.data.local.transaction

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import id.andriawan.cofinance.data.local.CofinanceRoomDatabase
import id.andriawan.cofinance.data.local.account.toResponse
import id.andriawan.cofinance.data.model.entity.LocalAccountEntity
import id.andriawan.cofinance.data.model.entity.LocalTransactionEntity
import id.andriawan.cofinance.data.model.AccountResponse
import id.andriawan.cofinance.data.model.TransactionResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class RoomTransactionLocalDataSource(
    private val roomDatabase: CofinanceRoomDatabase
) : TransactionLocalDataSource {
    private val transactionDao = roomDatabase.transactionDao()
    private val accountDao = roomDatabase.accountDao()

    override fun watchTransactions(
        startDate: String?,
        endDate: String?,
        isDraft: Boolean,
        transactionId: String?,
        expenseOnly: Boolean?
    ): Flow<List<TransactionResponse>> = combine(
        transactionDao.watchTransactions(),
        accountDao.watchAccounts()
    ) { transactions, accounts ->
        hydrate(transactions, accounts, startDate, endDate, isDraft, transactionId, expenseOnly)
    }

    override suspend fun getTransactions(
        startDate: String?,
        endDate: String?,
        isDraft: Boolean,
        transactionId: String?
    ): List<TransactionResponse> = hydrate(
        transactionDao.getTransactions(),
        accountDao.getAccounts(),
        startDate,
        endDate,
        isDraft,
        transactionId,
        null
    )

    override suspend fun getAllTransactions(): List<TransactionResponse> {
        val accounts = accountDao.getAccounts().associateBy(LocalAccountEntity::id)
        return transactionDao.getTransactions().map { transaction ->
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
            id = id,
            amount = amount,
            category = category,
            date = date,
            fee = fee,
            notes = notes,
            senderAccountId = accountsId,
            receiverAccountId = receiverAccountsId,
            type = type,
            createdAt = now,
            updatedAt = now
        )

        roomDatabase.useWriterConnection {
            it.immediateTransaction {
                applyBalanceDeltas(entity)
                transactionDao.upsertTransaction(entity)
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
                val old = transactionDao.getTransaction(id) ?: error("Transaction does not exist")
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
                transactionDao.upsertTransaction(replacement)
            }
        }

        return getTransactions(transactionId = id, isDraft = type == TYPE_DRAFT).first()
    }

    override suspend fun upsertTransactions(transactions: List<TransactionResponse>) {
        if (transactions.isNotEmpty()) transactionDao.upsertTransactions(
            transactions.map(TransactionResponse::toEntity)
        )
    }

    override suspend fun clearTransactions() {
        transactionDao.deleteAllTransactions()
    }

    private suspend fun applyBalanceDeltas(
        transaction: LocalTransactionEntity,
        multiplier: Long = 1
    ) {
        balanceDeltas(transaction).forEach { (accountId, delta) ->
            val account = accountDao.getAccount(accountId) ?: error("Account does not exist")
            accountDao.updateAccount(account.copy(balance = account.balance + delta * multiplier))
        }
    }

    private fun hydrate(
        transactions: List<LocalTransactionEntity>,
        accounts: List<LocalAccountEntity>,
        startDate: String? = null,
        endDate: String? = null,
        isDraft: Boolean = false,
        transactionId: String? = null,
        expenseOnly: Boolean?
    ): List<TransactionResponse> {
        val accountMap = accounts.associateBy(LocalAccountEntity::id)

        fun matchesId(transaction: LocalTransactionEntity) =
            transactionId == null || transaction.id == transactionId

        fun matchesDateRange(transaction: LocalTransactionEntity) =
            startDate == null || endDate == null || transaction.date in startDate..<endDate

        fun matchesType(transaction: LocalTransactionEntity): Boolean {
            if (transactionId != null) return true
            if (isDraft) return transaction.type == TYPE_DRAFT

            return transaction.type != TYPE_DRAFT && transaction.type != TYPE_CYCLE_RESET
        }

        fun matchesExpenseOnly(transaction: LocalTransactionEntity) =
            expenseOnly != true || transaction.type != TYPE_INCOME

        return transactions.filter { transaction ->
            matchesId(transaction) &&
                    matchesDateRange(transaction) &&
                    matchesType(transaction) &&
                    matchesExpenseOnly(transaction)
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

    companion object {
        private const val TYPE_INCOME = "INCOME"
        private const val TYPE_EXPENSE = "EXPENSE"
        private const val TYPE_TRANSFER = "TRANSFER"
        private const val TYPE_DRAFT = "DRAFT"
        private const val TYPE_CYCLE_RESET = "CYCLE_RESET"
    }
}

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
