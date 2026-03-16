package id.andriawan.cofinance.data.repository

import id.andriawan.cofinance.data.datasource.ReceiptScannerService
import id.andriawan.cofinance.data.local.CofinanceDatabase
import id.andriawan.cofinance.domain.model.request.AddTransactionParam
import id.andriawan.cofinance.domain.model.request.GetTransactionsParam
import id.andriawan.cofinance.domain.model.response.ReceiptScan
import id.andriawan.cofinance.domain.model.response.Transaction
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid


interface TransactionRepository {
    suspend fun scanReceipt(image: ByteArray): ReceiptScan
    suspend fun getTransactions(param: GetTransactionsParam): List<Transaction>
    fun watchTransactions(param: GetTransactionsParam): Flow<List<Transaction>>
    suspend fun createTransaction(params: AddTransactionParam): Transaction
}


class TransactionRepositoryImpl(
    private val receiptScanner: ReceiptScannerService,
    private val database: CofinanceDatabase,
    private val supabaseClient: SupabaseClient
) : TransactionRepository {

    private fun getUserId(): String =
        supabaseClient.auth.currentUserOrNull()?.id.orEmpty()

    override suspend fun scanReceipt(image: ByteArray): ReceiptScan {
        val response = receiptScanner.scanReceipt(image)
        return ReceiptScan.from(response)
    }

    override suspend fun getTransactions(param: GetTransactionsParam): List<Transaction> {
        val response = database.getTransactions(
            userId = getUserId(),
            month = param.month,
            year = param.year,
            isDraft = param.isDraft,
            transactionId = param.transactionId
        )
        return response.map(Transaction::from)
    }

    override fun watchTransactions(param: GetTransactionsParam): Flow<List<Transaction>> {
        return database.watchTransactions(
            userId = getUserId(),
            month = param.month,
            year = param.year,
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

        database.insertTransaction(
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

        // Get the inserted transaction back from local database
        val inserted = database.getTransactions(
            userId = userId,
            transactionId = id
        )
        return if (inserted.isNotEmpty()) {
            Transaction.from(inserted.first())
        } else {
            // Return a minimal transaction object
            Transaction(id = id, amount = params.amount ?: 0L, type = params.type)
        }
    }
}
