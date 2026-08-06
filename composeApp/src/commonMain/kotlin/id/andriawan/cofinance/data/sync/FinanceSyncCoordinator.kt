package id.andriawan.cofinance.data.sync

import id.andriawan.cofinance.data.local.account.AccountLocalDataSource
import id.andriawan.cofinance.data.local.transaction.TransactionLocalDataSource
import id.andriawan.cofinance.data.remote.AccountRemoteDataSource
import id.andriawan.cofinance.data.remote.TransactionRemoteDataSource
import id.andriawan.cofinance.data.session.SessionPolicy

class FinanceSyncCoordinator(
    private val localAccounts: AccountLocalDataSource,
    private val localTransactions: TransactionLocalDataSource,
    private val remoteAccounts: AccountRemoteDataSource,
    private val remoteTransactions: TransactionRemoteDataSource,
    private val sessionPolicy: SessionPolicy
) {
    suspend fun syncAfterSignIn() {
        sessionPolicy.requireUserId()

        val localAccountRecords = localAccounts.getAccounts()
        val localTransactionRecords = localTransactions.getAllTransactions()
        val localAccountIds = localAccountRecords.mapTo(mutableSetOf()) { it.id }
        val localTransactionIds = localTransactionRecords.mapTo(mutableSetOf()) { it.id }

        localAccounts.upsertAccounts(
            remoteAccounts.getAccounts().filterNot { it.id in localAccountIds })
        localTransactions.upsertTransactions(
            remoteTransactions.getTransactions().filterNot { it.id in localTransactionIds })

        mirrorAllIfSignedIn()
    }

    suspend fun mirrorAllIfSignedIn() {
        if (sessionPolicy.isSignedIn()) {
            remoteAccounts.upsertAccounts(localAccounts.getAccounts())
            remoteTransactions.upsertTransactions(localTransactions.getAllTransactions())
        }
    }

    suspend fun clearLocalAfterSignOut() {
        localAccounts.clearAccounts()
        localTransactions.clearTransactions()
    }
}
