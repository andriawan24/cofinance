package id.andriawan.cofinance

import id.andriawan.cofinance.data.datasource.ReceiptScanner
import id.andriawan.cofinance.data.local.account.AccountLocalDataSource
import id.andriawan.cofinance.data.local.transaction.TransactionLocalDataSource
import id.andriawan.cofinance.data.model.response.AccountResponse
import id.andriawan.cofinance.data.model.response.ReceiptScanResponse
import id.andriawan.cofinance.data.model.response.TransactionResponse
import id.andriawan.cofinance.data.remote.AccountRemoteDataSource
import id.andriawan.cofinance.data.remote.TransactionRemoteDataSource
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
        val localAccounts = FakeAccountLocalDataSource()
        val localTransactions = FakeTransactionLocalDataSource()
        val remoteAccounts = FakeAccountRemoteDataSource(failOnAccess = true)
        val remoteTransactions = FakeTransactionRemoteDataSource(failOnAccess = true)
        val session = FakeSessionPolicy(userId = null)
        val repository = AccountRepositoryImpl(
            database = localAccounts,
            syncCoordinator = FinanceSyncCoordinator(
                localAccounts,
                localTransactions,
                remoteAccounts,
                remoteTransactions,
                session
            )
        )

        repository.addAccount(AccountParam("Cash", 50_000, "Wallet"))

        assertEquals("Cash", localAccounts.getAccounts().single().name)
        assertEquals(0, remoteAccounts.accessCount)
    }

    @Test
    fun signInMergeImportsRemoteOnlyRecordsAndKeepsLocalCollision() = runTest {
        val localRecord = account("shared", "Local")
        val remoteCollision = account("shared", "Remote")
        val remoteOnly = account("remote-only", "Cloud savings")
        val localAccounts = FakeAccountLocalDataSource(accounts = listOf(localRecord))
        val localTransactions = FakeTransactionLocalDataSource()
        val remoteAccounts = FakeAccountRemoteDataSource(accounts = listOf(remoteCollision, remoteOnly))
        val remoteTransactions = FakeTransactionRemoteDataSource()
        val coordinator = FinanceSyncCoordinator(
            localAccounts,
            localTransactions,
            remoteAccounts,
            remoteTransactions,
            FakeSessionPolicy("firebase-user")
        )

        coordinator.syncAfterSignIn()

        assertEquals(setOf("shared", "remote-only"), localAccounts.getAccounts().mapNotNull { it.id }.toSet())
        assertEquals("Local", localAccounts.getAccounts().first { it.id == "shared" }.name)
        assertEquals("Local", remoteAccounts.uploadedAccounts.first { it.id == "shared" }.name)
        assertTrue(remoteAccounts.uploadedAccounts.any { it.id == "remote-only" })
    }

    @Test
    fun signedOutReceiptScanIsRejectedBeforeScannerInvocation() = runTest {
        val localAccounts = FakeAccountLocalDataSource()
        val localTransactions = FakeTransactionLocalDataSource()
        val session = FakeSessionPolicy(null)
        val scanner = FakeReceiptScanner()
        val remoteAccounts = FakeAccountRemoteDataSource()
        val remoteTransactions = FakeTransactionRemoteDataSource()
        val repository = TransactionRepositoryImpl(
            receiptScanner = scanner,
            database = localTransactions,
            sessionPolicy = session,
            syncCoordinator = FinanceSyncCoordinator(
                localAccounts,
                localTransactions,
                remoteAccounts,
                remoteTransactions,
                session
            )
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

private class FakeAccountRemoteDataSource(
    accounts: List<AccountResponse> = emptyList(),
    private val failOnAccess: Boolean = false
) : AccountRemoteDataSource {
    private val storedAccounts = accounts.toMutableList()
    var accessCount = 0
    var uploadedAccounts: List<AccountResponse> = emptyList()

    override suspend fun getAccounts(): List<AccountResponse> = access { storedAccounts.toList() }

    override suspend fun upsertAccounts(accounts: List<AccountResponse>) {
        access {
            uploadedAccounts = accounts
            storedAccounts.clear()
            storedAccounts.addAll(accounts)
        }
    }

    private fun <T> access(block: () -> T): T {
        accessCount++
        if (failOnAccess) error("Cloud must not be used")
        return block()
    }
}

private class FakeTransactionRemoteDataSource(
    transactions: List<TransactionResponse> = emptyList(),
    private val failOnAccess: Boolean = false
) : TransactionRemoteDataSource {
    private val storedTransactions = transactions.toMutableList()
    var accessCount = 0

    override suspend fun getTransactions(): List<TransactionResponse> = access { storedTransactions.toList() }

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

private class FakeAccountLocalDataSource(
    accounts: List<AccountResponse> = emptyList()
) : AccountLocalDataSource {
    private val accountState = MutableStateFlow(accounts)

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

    override suspend fun upsertAccounts(accounts: List<AccountResponse>) {
        val existing = accountState.value.associateBy { it.id }.toMutableMap()
        accounts.forEach { existing[it.id] = it }
        accountState.value = existing.values.toList()
    }

    override suspend fun clearAccounts() {
        accountState.value = emptyList()
    }
}

private class FakeTransactionLocalDataSource(
    transactions: List<TransactionResponse> = emptyList()
) : TransactionLocalDataSource {
    private val transactionState = MutableStateFlow(transactions)

    override fun watchTransactions(
        startDate: String?,
        endDate: String?,
        isDraft: Boolean,
        transactionId: String?,
        expenseOnly: Boolean?
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

    override suspend fun upsertTransactions(transactions: List<TransactionResponse>) {
        val existing = transactionState.value.associateBy { it.id }.toMutableMap()
        transactions.forEach { existing[it.id] = it }
        transactionState.value = existing.values.toList()
    }

    override suspend fun clearTransactions() {
        transactionState.value = emptyList()
    }
}
