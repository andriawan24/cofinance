package id.andriawan.cofinance.data.local

import id.andriawan.cofinance.data.datasource.SupabaseDataSource
import id.andriawan.cofinance.data.model.request.AccountRequest
import id.andriawan.cofinance.data.model.request.AddTransactionRequest
import id.andriawan.cofinance.data.model.request.GetTransactionsRequest
import id.andriawan.cofinance.data.model.response.AccountResponse
import id.andriawan.cofinance.data.model.response.TransactionResponse
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Online-only implementation for web targets (JS/WasmJS).
 * All operations go directly through Supabase. No offline support.
 */
class OnlineOnlyDatabase(
    private val supabaseDataSource: SupabaseDataSource
) : CofinanceDatabase {

    override fun watchAccounts(userId: String): Flow<List<AccountResponse>> = flow {
        emit(supabaseDataSource.getAccounts())
    }

    override suspend fun getAccounts(userId: String): List<AccountResponse> {
        return supabaseDataSource.getAccounts()
    }

    override suspend fun insertAccount(
        id: String,
        name: String,
        group: String,
        balance: Long,
        userId: String
    ) {
        supabaseDataSource.addAccount(
            AccountRequest(
                name = name,
                balance = balance,
                group = group,
                usersId = userId
            )
        )
    }

    override fun watchTransactions(
        userId: String,
        startDate: String?,
        endDate: String?,
        isDraft: Boolean,
        transactionId: String?
    ): Flow<List<TransactionResponse>> = flow {
        emit(supabaseDataSource.getTransactions(
            GetTransactionsRequest(startDate = startDate, endDate = endDate, isDraft = isDraft, transactionId = transactionId)
        ))
    }

    override suspend fun getTransactions(
        userId: String,
        startDate: String?,
        endDate: String?,
        isDraft: Boolean,
        transactionId: String?
    ): List<TransactionResponse> {
        return supabaseDataSource.getTransactions(
            GetTransactionsRequest(startDate = startDate, endDate = endDate, isDraft = isDraft, transactionId = transactionId)
        )
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
        type: String,
        userId: String
    ) {
        supabaseDataSource.createTransaction(
            AddTransactionRequest(
                id = id,
                amount = amount,
                category = category,
                date = date,
                fee = fee,
                notes = notes,
                accountsId = accountsId,
                receiverAccountsId = receiverAccountsId,
                type = type,
                usersId = userId
            )
        )
    }

    override suspend fun connectSync(supabaseClient: SupabaseClient, powerSyncUrl: String) {
        // No-op for online-only
    }

    override suspend fun disconnectSync() {
        // No-op for online-only
    }

    override suspend fun disconnectAndClearSync() {
        // No-op for online-only
    }

    override suspend fun pauseSync() {
        // No-op for online-only
    }

    override suspend fun resumeSync() {
        // No-op for online-only
    }

}
