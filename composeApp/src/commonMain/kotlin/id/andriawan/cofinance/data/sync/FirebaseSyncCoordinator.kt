package id.andriawan.cofinance.data.sync

import id.andriawan.cofinance.data.local.account.AccountLocalDataSource
import id.andriawan.cofinance.data.local.transaction.TransactionLocalDataSource
import id.andriawan.cofinance.data.remote.AccountRemoteDataSource
import id.andriawan.cofinance.data.remote.TransactionRemoteDataSource
import id.andriawan.cofinance.data.session.SessionPolicy

class FirebaseSyncCoordinator(
    private val localAccountSource: AccountLocalDataSource,
    private val localTransactionSource: TransactionLocalDataSource,
    private val remoteAccountSource: AccountRemoteDataSource,
    private val remoteTransactionSource: TransactionRemoteDataSource,
    private val sessionPolicy: SessionPolicy
) {
    suspend fun syncDataAfterSignIn() {
        sessionPolicy.requireUserId()

        val localAccountRecords = localAccountSource.getAccounts()
        val localTransactionRecords = localTransactionSource.getAllTransactions()
        val localAccountIds = localAccountRecords.mapTo(mutableSetOf()) { it.id }
        val localTransactionIds = localTransactionRecords.mapTo(mutableSetOf()) { it.id }

        val localAccounts = remoteAccountSource.getAccounts().filterNot { it.id in localAccountIds }
        localAccountSource.upsertAccounts(localAccounts)

        val localTransactions = remoteTransactionSource.getTransactions().filterNot {
            it.id in localTransactionIds
        }
        localTransactionSource.upsertTransactions(localTransactions)

        mirrorDataIfSignedIn()
    }

    suspend fun mirrorDataIfSignedIn() {
        if (sessionPolicy.isSignedIn()) {
            remoteAccountSource.upsertAccounts(localAccountSource.getAccounts())
            remoteTransactionSource.upsertTransactions(localTransactionSource.getAllTransactions())
        }
    }

    suspend fun clearLocalData() {
        localAccountSource.clearAccounts()
        localTransactionSource.clearTransactions()
    }
}
