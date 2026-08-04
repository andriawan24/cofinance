package id.andriawan.cofinance

import id.andriawan.cofinance.data.datasource.ReceiptScanner
import id.andriawan.cofinance.data.local.CofinanceDatabase
import id.andriawan.cofinance.data.local.RemoteFinanceDataSource
import id.andriawan.cofinance.data.model.response.AccountResponse
import id.andriawan.cofinance.data.model.response.ReceiptScanResponse
import id.andriawan.cofinance.data.model.response.TransactionResponse
import id.andriawan.cofinance.data.repository.AccountRepositoryImpl
import id.andriawan.cofinance.data.repository.TransactionRepositoryImpl
import id.andriawan.cofinance.data.session.SessionPolicy
import id.andriawan.cofinance.data.session.SignedInSessionRequiredException
import id.andriawan.cofinance.data.sync.FinanceSyncCoordinator
import id.andriawan.cofinance.domain.model.request.AccountParam
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class OfflineFirstAccessTest {
    @Test
    fun signedOutAccountWriteStaysLocalAndDoesNotTouchCloud() = runTest {
        val local = FakeDatabase()
        val remote = FakeRemote(failOnAccess = true)
        val session = FakeSessionPolicy(userId = null)
        val repository = AccountRepositoryImpl(
            database = local,
            syncCoordinator = FinanceSyncCoordinator(local, remote, session)
        )

        repository.addAccount(AccountParam("Cash", 50_000, "Wallet"))

        assertEquals("Cash", local.getAccounts().single().name)
        assertEquals(0, remote.accessCount)
    }

    @Test
    fun signInMergeImportsRemoteOnlyRecordsAndKeepsLocalCollision() = runTest {
        val localRecord = account("shared", "Local")
        val remoteCollision = account("shared", "Remote")
        val remoteOnly = account("remote-only", "Cloud savings")
        val local = FakeDatabase(accounts = listOf(localRecord))
        val remote = FakeRemote(accounts = listOf(remoteCollision, remoteOnly))
        val coordinator = FinanceSyncCoordinator(local, remote, FakeSessionPolicy("firebase-user"))

        coordinator.syncAfterSignIn()

        assertEquals(setOf("shared", "remote-only"), local.getAccounts().mapNotNull { it.id }.toSet())
        assertEquals("Local", local.getAccounts().first { it.id == "shared" }.name)
        assertEquals("Local", remote.uploadedAccounts.first { it.id == "shared" }.name)
        assertTrue(remote.uploadedAccounts.any { it.id == "remote-only" })
    }

    @Test
    fun signedOutReceiptScanIsRejectedBeforeScannerInvocation() = runTest {
        val local = FakeDatabase()
        val session = FakeSessionPolicy(null)
        val scanner = FakeReceiptScanner()
        val remote = FakeRemote()
        val repository = TransactionRepositoryImpl(
            receiptScanner = scanner,
            database = local,
            sessionPolicy = session,
            syncCoordinator = FinanceSyncCoordinator(local, remote, session)
        )

        assertFailsWith<SignedInSessionRequiredException> {
            repository.scanReceipt(byteArrayOf(1, 2, 3))
        }
        assertEquals(0, scanner.invocationCount)
    }

    private fun account(id: String, name: String) = AccountResponse(
        id = id,
        name = name,
        group = "Test",
        balance = 100,
        accountType = "REGULAR_BALANCE",
        createdAt = "2026-01-01T00:00:00Z"
    )
}

private class FakeSessionPolicy(private val userId: String?) : SessionPolicy {
    override fun isSignedIn(): Boolean = userId != null
    override fun userIdOrNull(): String? = userId
}

private class FakeReceiptScanner : ReceiptScanner {
    var invocationCount = 0
    override suspend fun scanReceipt(image: ByteArray): ReceiptScanResponse {
        invocationCount++
        return ReceiptScanResponse()
    }
}

private class FakeRemote(
    accounts: List<AccountResponse> = emptyList(),
    transactions: List<TransactionResponse> = emptyList(),
    private val failOnAccess: Boolean = false
) : RemoteFinanceDataSource {
    private val storedAccounts = accounts.toMutableList()
    private val storedTransactions = transactions.toMutableList()
    var accessCount = 0
    var uploadedAccounts: List<AccountResponse> = emptyList()

    override suspend fun getAccounts(): List<AccountResponse> = access { storedAccounts.toList() }
    override suspend fun getTransactions(): List<TransactionResponse> = access { storedTransactions.toList() }

    override suspend fun upsertAccounts(accounts: List<AccountResponse>) {
        access {
            uploadedAccounts = accounts
            storedAccounts.clear()
            storedAccounts.addAll(accounts)
        }
    }

    override suspend fun upsertTransactions(transactions: List<TransactionResponse>) {
        access {
            storedTransactions.clear()
            storedTransactions.addAll(transactions)
        }
    }

    private fun <T> access(block: () -> T): T {
        accessCount++
        if (failOnAccess) error("Cloud must not be used")
        return block()
    }
}

private class FakeDatabase(
    accounts: List<AccountResponse> = emptyList(),
    transactions: List<TransactionResponse> = emptyList()
) : CofinanceDatabase {
    private val accountState = MutableStateFlow(accounts)
    private val transactionState = MutableStateFlow(transactions)

    override fun watchAccounts(): Flow<List<AccountResponse>> = accountState
    override suspend fun getAccounts(): List<AccountResponse> = accountState.value

    override suspend fun insertAccount(
        id: String,
        name: String,
        group: String,
        balance: Long,
        accountType: String
    ) {
        accountState.value += AccountResponse(id, name, group, balance, accountType, "now")
    }

    override suspend fun updateAccountBalance(accountId: String, delta: Long) {
        accountState.value = accountState.value.map {
            if (it.id == accountId) it.copy(balance = (it.balance ?: 0) + delta) else it
        }
    }

    override suspend fun updateAccountType(accountId: String, accountType: String) {
        accountState.value = accountState.value.map { if (it.id == accountId) it.copy(accountType = accountType) else it }
    }

    override suspend fun updateAccount(
        accountId: String,
        name: String,
        balance: Long,
        group: String,
        accountType: String
    ) {
        accountState.value = accountState.value.map {
            if (it.id == accountId) it.copy(name = name, balance = balance, group = group, accountType = accountType) else it
        }
    }

    override suspend fun deleteAccount(accountId: String) {
        accountState.value = accountState.value.filterNot { it.id == accountId }
    }

    override fun watchTransactions(
        startDate: String?,
        endDate: String?,
        isDraft: Boolean,
        transactionId: String?
    ): Flow<List<TransactionResponse>> = transactionState

    override suspend fun getTransactions(
        startDate: String?,
        endDate: String?,
        isDraft: Boolean,
        transactionId: String?
    ): List<TransactionResponse> = transactionState.value

    override suspend fun getAllTransactions(): List<TransactionResponse> = transactionState.value

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
    ): TransactionResponse = error("Not used by these tests")

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
    ): TransactionResponse = error("Not used by these tests")

    override suspend fun upsertAccounts(accounts: List<AccountResponse>) {
        val existing = accountState.value.associateBy { it.id }.toMutableMap()
        accounts.forEach { existing[it.id] = it }
        accountState.value = existing.values.toList()
    }

    override suspend fun upsertTransactions(transactions: List<TransactionResponse>) {
        val existing = transactionState.value.associateBy { it.id }.toMutableMap()
        transactions.forEach { existing[it.id] = it }
        transactionState.value = existing.values.toList()
    }

    override suspend fun clearAll() {
        accountState.value = emptyList()
        transactionState.value = emptyList()
    }
}
