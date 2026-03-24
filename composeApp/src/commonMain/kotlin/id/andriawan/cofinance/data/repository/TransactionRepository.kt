package id.andriawan.cofinance.data.repository

import id.andriawan.cofinance.data.datasource.GeminiDataSource
import id.andriawan.cofinance.data.local.CofinanceDatabase
import id.andriawan.cofinance.domain.model.request.AddTransactionParam
import id.andriawan.cofinance.domain.model.request.GetTransactionsParam
import id.andriawan.cofinance.domain.model.response.ReceiptScan
import id.andriawan.cofinance.domain.model.response.Transaction
import id.andriawan.cofinance.utils.enums.TransactionType
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
    suspend fun updateTransaction(oldTransaction: Transaction, params: AddTransactionParam): Transaction
}


class TransactionRepositoryImpl(
    private val geminiDataSource: GeminiDataSource,
    private val database: CofinanceDatabase,
    private val supabaseClient: SupabaseClient
) : TransactionRepository {

    private fun getUserId(): String =
        supabaseClient.auth.currentUserOrNull()?.id.orEmpty()

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

        // Optimistically update account balances locally
        val amount = params.amount ?: 0L
        val fee = params.fee ?: 0L
        when (params.type) {
            TransactionType.INCOME -> {
                if (!params.accountsId.isNullOrEmpty()) {
                    database.updateAccountBalance(params.accountsId, amount)
                }
            }
            TransactionType.EXPENSE -> {
                if (!params.accountsId.isNullOrEmpty()) {
                    database.updateAccountBalance(params.accountsId, -(amount + fee))
                }
            }
            TransactionType.TRANSFER -> {
                if (!params.accountsId.isNullOrEmpty()) {
                    database.updateAccountBalance(params.accountsId, -(amount + fee))
                }
                if (!params.receiverAccountsId.isNullOrEmpty()) {
                    database.updateAccountBalance(params.receiverAccountsId, amount)
                }
            }
            else -> { /* DRAFT, CYCLE_RESET — no balance change */ }
        }

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

    override suspend fun updateTransaction(
        oldTransaction: Transaction,
        params: AddTransactionParam
    ): Transaction {
        val id = params.id ?: oldTransaction.id

        // 1. Reverse the old transaction's balance impact
        reverseBalanceImpact(oldTransaction)

        // 2. Update the transaction row
        database.updateTransaction(
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

        // 3. Apply the new transaction's balance impact
        val newAmount = params.amount ?: 0L
        val newFee = params.fee ?: 0L
        when (params.type) {
            TransactionType.INCOME -> {
                if (!params.accountsId.isNullOrEmpty()) {
                    database.updateAccountBalance(params.accountsId, newAmount)
                }
            }
            TransactionType.EXPENSE -> {
                if (!params.accountsId.isNullOrEmpty()) {
                    database.updateAccountBalance(params.accountsId, -(newAmount + newFee))
                }
            }
            TransactionType.TRANSFER -> {
                if (!params.accountsId.isNullOrEmpty()) {
                    database.updateAccountBalance(params.accountsId, -(newAmount + newFee))
                }
                if (!params.receiverAccountsId.isNullOrEmpty()) {
                    database.updateAccountBalance(params.receiverAccountsId, newAmount)
                }
            }
            else -> { /* DRAFT, CYCLE_RESET — no balance change */ }
        }

        // 4. Read back the updated transaction
        val userId = getUserId()
        val updated = database.getTransactions(userId = userId, transactionId = id)
        return if (updated.isNotEmpty()) {
            Transaction.from(updated.first())
        } else {
            Transaction(id = id, amount = params.amount ?: 0L, type = params.type)
        }
    }

    private suspend fun reverseBalanceImpact(transaction: Transaction) {
        val amount = transaction.amount
        val fee = transaction.fee
        when (transaction.type) {
            TransactionType.INCOME -> {
                if (transaction.account.id.isNotEmpty()) {
                    database.updateAccountBalance(transaction.account.id, -amount)
                }
            }
            TransactionType.EXPENSE -> {
                if (transaction.account.id.isNotEmpty()) {
                    database.updateAccountBalance(transaction.account.id, amount + fee)
                }
            }
            TransactionType.TRANSFER -> {
                if (transaction.account.id.isNotEmpty()) {
                    database.updateAccountBalance(transaction.account.id, amount + fee)
                }
                if (transaction.receiverAccount.id.isNotEmpty()) {
                    database.updateAccountBalance(transaction.receiverAccount.id, -amount)
                }
            }
            else -> { /* DRAFT, CYCLE_RESET — no balance change */ }
        }
    }
}
