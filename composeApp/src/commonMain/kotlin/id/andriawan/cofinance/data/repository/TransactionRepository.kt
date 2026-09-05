package id.andriawan.cofinance.data.repository

import id.andriawan.cofinance.data.datasource.ReceiptScanner
import id.andriawan.cofinance.data.local.transaction.TransactionLocalDataSource
import id.andriawan.cofinance.domain.model.request.AddTransactionParam
import id.andriawan.cofinance.domain.model.request.GetTransactionsParam
import id.andriawan.cofinance.domain.model.response.ReceiptScan
import id.andriawan.cofinance.domain.model.response.Transaction
import id.andriawan.cofinance.data.sync.FirebaseSyncCoordinator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid


interface TransactionRepository {
    suspend fun scanReceipt(image: ByteArray): ReceiptScan
    suspend fun getTransactions(param: GetTransactionsParam): List<Transaction>
    fun watchTransactions(param: GetTransactionsParam): Flow<List<Transaction>>
    suspend fun createTransaction(params: AddTransactionParam): Transaction
    suspend fun updateTransaction(
        oldTransaction: Transaction,
        params: AddTransactionParam
    ): Transaction

    suspend fun deleteTransaction(id: String)

    suspend fun deleteTransactionsForAccount(accountId: String)
}


class TransactionRepositoryImpl(
    private val receiptScanner: ReceiptScanner,
    private val database: TransactionLocalDataSource,
    private val syncCoordinator: FirebaseSyncCoordinator
) : TransactionRepository {

    override suspend fun scanReceipt(image: ByteArray): ReceiptScan {
        return ReceiptScan.from(receiptScanner.scanReceipt(image))
    }

    override suspend fun getTransactions(param: GetTransactionsParam): List<Transaction> {
        val response = database.getTransactions(
            startDate = param.startDate,
            endDate = param.endDate,
            isDraft = param.isDraft,
            transactionId = param.transactionId
        )

        return response.map(Transaction::from)
    }

    override fun watchTransactions(param: GetTransactionsParam): Flow<List<Transaction>> {
        return database.watchTransactions(
            startDate = param.startDate,
            endDate = param.endDate,
            isDraft = param.isDraft,
            transactionId = param.transactionId,
            expenseOnly = param.expenseOnly
        ).map { transactions ->
            transactions.map(Transaction::from)
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun createTransaction(params: AddTransactionParam): Transaction {
        val id = params.id ?: Uuid.random().toString()
        val inserted = database.insertTransaction(
            id = id,
            amount = params.amount ?: 0L,
            category = params.category.orEmpty(),
            date = params.date.orEmpty(),
            fee = params.fee ?: 0L,
            notes = params.notes.orEmpty(),
            accountsId = params.accountsId.orEmpty(),
            receiverAccountsId = params.receiverAccountsId,
            type = params.type.name
        )

        syncCoordinator.mirrorDataIfSignedIn()

        return Transaction.from(inserted)
    }

    override suspend fun updateTransaction(
        oldTransaction: Transaction,
        params: AddTransactionParam
    ): Transaction {
        val id = params.id ?: oldTransaction.id
        val updated = database.updateTransaction(
            id = id,
            amount = params.amount ?: 0L,
            category = params.category.orEmpty(),
            date = params.date.orEmpty(),
            fee = params.fee ?: 0L,
            notes = params.notes.orEmpty(),
            accountsId = params.accountsId.orEmpty(),
            receiverAccountsId = params.receiverAccountsId,
            type = params.type.name
        )

        syncCoordinator.mirrorDataIfSignedIn()

        return Transaction.from(updated)
    }

    override suspend fun deleteTransaction(id: String) {
        database.deleteTransaction(id)
        syncCoordinator.deleteTransactionIfSignedIn(id)
    }

    override suspend fun deleteTransactionsForAccount(accountId: String) {
        val deleted = database.deleteTransactionsForAccount(accountId)
        deleted.forEach { syncCoordinator.deleteTransactionIfSignedIn(it) }
    }
}
