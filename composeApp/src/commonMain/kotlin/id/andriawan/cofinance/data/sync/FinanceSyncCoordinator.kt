package id.andriawan.cofinance.data.sync

import id.andriawan.cofinance.data.local.CofinanceDatabase
import id.andriawan.cofinance.data.local.RemoteFinanceDataSource
import id.andriawan.cofinance.data.session.SessionPolicy

class FinanceSyncCoordinator(
    private val localDatabase: CofinanceDatabase,
    private val remoteDataSource: RemoteFinanceDataSource,
    private val sessionPolicy: SessionPolicy
) {
    suspend fun syncAfterSignIn() {
        sessionPolicy.requireUserId()

        val localAccounts = localDatabase.getAccounts()
        val localTransactions = localDatabase.getAllTransactions()
        val localAccountIds = localAccounts.mapTo(mutableSetOf()) { it.id }
        val localTransactionIds = localTransactions.mapTo(mutableSetOf()) { it.id }

        localDatabase.upsertAccounts(
            remoteDataSource.getAccounts().filterNot { it.id in localAccountIds })
        localDatabase.upsertTransactions(
            remoteDataSource.getTransactions().filterNot { it.id in localTransactionIds })

        mirrorAllIfSignedIn()
    }

    suspend fun mirrorAllIfSignedIn() {
        if (sessionPolicy.isSignedIn()) {
            remoteDataSource.upsertAccounts(localDatabase.getAccounts())
            remoteDataSource.upsertTransactions(localDatabase.getAllTransactions())
        }
    }

    suspend fun clearLocalAfterSignOut() {
        localDatabase.clearAll()
    }
}
