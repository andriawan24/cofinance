package id.andriawan.cofinance.data.repository

import id.andriawan.cofinance.data.datasource.GeminiDataSource
import id.andriawan.cofinance.data.local.CofinanceDatabase
import id.andriawan.cofinance.domain.model.request.AddTransactionParam
import id.andriawan.cofinance.domain.model.request.GetTransactionsParam
import id.andriawan.cofinance.domain.model.response.ReceiptScan
import id.andriawan.cofinance.domain.model.response.Transaction
import dev.gitlive.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid


interface TransactionRepository {
    suspend fun scanReceipt(image: ByteArray): ReceiptScan
    suspend fun getTransactions(param: GetTransactionsParam): List<Transaction>
    fun watchTransactions(param: GetTransactionsParam): Flow<List<Transaction>>
    suspend fun createTransaction(params: AddTransactionParam): Transaction
    suspend fun updateTransaction(oldTransaction: Transaction, params: AddTransactionParam): Transaction
}


class TransactionRepositoryImpl(
    private val geminiDataSource: GeminiDataSource,
    private val database: CofinanceDatabase,
    private val firebaseAuth: FirebaseAuth
) : TransactionRepository {

    private fun getUserId(): String =
        firebaseAuth.currentUser?.uid ?: error("No authenticated Firebase user")

    override suspend fun scanReceipt(image: ByteArray): ReceiptScan {
        val response = geminiDataSource.scanReceipt(image)
        return ReceiptScan.from(response)
    }

    override suspend fun getTransactions(param: GetTransactionsParam): List<Transaction> {
        val response = database.getTransactions(
            userId = getUserId(),
            startDate = param.startDate,
            endDate = param.endDate,
            isDraft = param.isDraft,
            transactionId = param.transactionId
        )
        return response.map(Transaction::from)
    }

    override fun watchTransactions(param: GetTransactionsParam): Flow<List<Transaction>> {
        return database.watchTransactions(
            userId = getUserId(),
            startDate = param.startDate,
            endDate = param.endDate,
            isDraft = param.isDraft,
            transactionId = param.transactionId
        ).map { transactions ->
            transactions.map(Transaction::from)
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun createTransaction(params: AddTransactionParam): Transaction {
        val userId = getUserId()
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
            type = params.type.name,
            userId = userId
        )

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
            type = params.type.name,
            userId = getUserId()
        )
        return Transaction.from(updated)
    }
}
