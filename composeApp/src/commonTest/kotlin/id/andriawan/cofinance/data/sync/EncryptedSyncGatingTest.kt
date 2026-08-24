package id.andriawan.cofinance.data.sync

import id.andriawan.cofinance.data.crypto.DataKey
import id.andriawan.cofinance.data.crypto.KeyWrapType
import id.andriawan.cofinance.data.keyring.InMemoryEncryptionSession
import id.andriawan.cofinance.data.local.account.AccountLocalDataSource
import id.andriawan.cofinance.data.local.transaction.TransactionLocalDataSource
import id.andriawan.cofinance.data.model.AccountResponse
import id.andriawan.cofinance.data.model.TransactionResponse
import id.andriawan.cofinance.data.remote.EncryptedRemoteFixture
import id.andriawan.cofinance.data.remote.FinanceCollection
import id.andriawan.cofinance.data.remote.keyMaterialWith
import id.andriawan.cofinance.data.session.SessionPolicy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest

/**
 * What the coordinator does when the data key is not available.
 *
 * The three paths that matter are setup-incomplete, locked, and unlocked. The first two must reach
 * the backend with nothing at all — not plaintext, and not a partial encrypted write — while leaving
 * the local records intact, because the local database is the source of truth and a mutation that
 * already committed must not be undone by a sync that cannot run. The third must mirror, including
 * everything that accumulated while locked.
 *
 * The remote sources here are the real encrypted ones over a fake store, so "no plaintext was
 * uploaded" is asserted against what a backend would actually hold.
 */
class EncryptedSyncGatingTest {

    @Test
    fun setupIncompleteUploadsNothing() = runTest {
        val fixture = SyncFixture(InMemoryEncryptionSession())

        fixture.coordinator.syncDataAfterSignIn()
        fixture.coordinator.mirrorDataIfSignedIn()

        assertTrue(fixture.remote.store.writeLog.isEmpty(), "A record left a device without setup")
        assertEquals(1, fixture.localAccounts.getAccounts().size, "The local record was disturbed")
    }

    @Test
    fun lockedSessionUploadsNothing() = runTest {
        val session = InMemoryEncryptionSession().apply { markSetUp() }
        val fixture = SyncFixture(session)

        fixture.coordinator.syncDataAfterSignIn()
        fixture.coordinator.mirrorDataIfSignedIn()

        assertTrue(fixture.remote.store.writeLog.isEmpty(), "A record left a locked device")
        assertEquals(1, fixture.localAccounts.getAccounts().size, "The local record was disturbed")
    }

    @Test
    fun unlockedSessionMirrorsEncryptedRecords() = runTest {
        val dataKey = DataKey.generate()
        val session = InMemoryEncryptionSession().apply { unlock(dataKey) }
        val fixture = SyncFixture(session)
        fixture.publishKeyMaterial(dataKey)

        fixture.coordinator.mirrorDataIfSignedIn()

        assertEquals(setOf("account-1"), fixture.remote.store.documentIds(FinanceCollection.ACCOUNTS))
        assertEquals(
            setOf("transaction-1"),
            fixture.remote.store.documentIds(FinanceCollection.TRANSACTIONS)
        )
        val storedAccount = fixture.remote.store.storedText(FinanceCollection.ACCOUNTS, "account-1")
        assertFalse(storedAccount.contains("Bank Mandiri"), "The mirror uploaded a readable name")
        assertFalse(storedAccount.contains("12450000"), "The mirror uploaded a readable balance")
    }

    @Test
    fun aMutationMadeWhileLockedIsMirroredAfterTheNextUnlock() = runTest {
        val dataKey = DataKey.generate()
        val session = InMemoryEncryptionSession().apply { markSetUp() }
        val fixture = SyncFixture(session)

        fixture.localTransactions.upsertTransactions(
            listOf(TransactionResponse(id = "recorded-while-locked", amount = 45_000, category = "FOOD"))
        )
        fixture.coordinator.mirrorDataIfSignedIn()

        assertTrue(fixture.remote.store.writeLog.isEmpty(), "A locked mirror reached the backend")
        assertEquals(2, fixture.localTransactions.getAllTransactions().size, "The local write was lost")

        session.unlock(dataKey)
        fixture.publishKeyMaterial(dataKey)
        fixture.coordinator.mirrorDataIfSignedIn()

        assertTrue(
            "recorded-while-locked" in fixture.remote.store.documentIds(FinanceCollection.TRANSACTIONS),
            "The record made while locked never reached the backend after unlocking"
        )
    }

    @Test
    fun lockingBetweenMirrorsStopsFurtherUploads() = runTest {
        val dataKey = DataKey.generate()
        val session = InMemoryEncryptionSession().apply { unlock(dataKey) }
        val fixture = SyncFixture(session)
        fixture.publishKeyMaterial(dataKey)

        fixture.coordinator.mirrorDataIfSignedIn()
        val writesWhileUnlocked = fixture.remote.store.writeLog.size

        session.lock()
        fixture.localAccounts.upsertAccounts(listOf(AccountResponse(id = "account-2", name = "Cash")))
        fixture.coordinator.mirrorDataIfSignedIn()

        assertEquals(writesWhileUnlocked, fixture.remote.store.writeLog.size)
    }

    @Test
    fun signInSyncImportsNothingWhileLocked() = runTest {
        val dataKey = DataKey.generate()
        val session = InMemoryEncryptionSession().apply { unlock(dataKey) }
        val seeding = SyncFixture(session)
        seeding.publishKeyMaterial(dataKey)
        seeding.coordinator.mirrorDataIfSignedIn()

        // A second device that shares the backend but has not unlocked yet.
        val lockedSession = InMemoryEncryptionSession().apply { markSetUp() }
        val restoring = SyncFixture(
            encryptionSession = lockedSession,
            remote = EncryptedRemoteFixture(store = seeding.remote.store, session = lockedSession),
            localAccountRecords = emptyList(),
            localTransactionRecords = emptyList()
        )

        restoring.coordinator.syncDataAfterSignIn()

        assertTrue(restoring.localAccounts.getAccounts().isEmpty(), "A locked device imported records")
    }
}

private class SyncFixture(
    encryptionSession: InMemoryEncryptionSession,
    val remote: EncryptedRemoteFixture = EncryptedRemoteFixture(session = encryptionSession),
    localAccountRecords: List<AccountResponse> = listOf(
        AccountResponse(
            id = "account-1",
            name = "Bank Mandiri",
            group = "Tabungan",
            balance = 12_450_000L,
            accountType = "REGULAR_BALANCE",
            createdAt = "2026-08-17T09:00:00Z"
        )
    ),
    localTransactionRecords: List<TransactionResponse> = listOf(
        TransactionResponse(
            id = "transaction-1",
            amount = 85_000L,
            category = "Groceries",
            date = "2026-08-17",
            type = "EXPENSE"
        )
    )
) {
    val localAccounts = FakeAccountLocalDataSource(localAccountRecords)
    val localTransactions = FakeTransactionLocalDataSource(localTransactionRecords)

    val coordinator = FirebaseSyncCoordinator(
        localAccounts,
        localTransactions,
        remote.accounts,
        remote.transactions,
        object : SessionPolicy {
            override fun isSignedIn(): Boolean = true
            override fun userIdOrNull(): String = "firebase-user"
        },
        encryptionSession
    )

    suspend fun publishKeyMaterial(dataKey: DataKey) {
        remote.keyMaterialGate.publishKeyMaterial(
            keyMaterialWith(dataKey.id, KeyWrapType.Device, KeyWrapType.RecoveryPhrase)
        )
    }
}

private class FakeAccountLocalDataSource(accounts: List<AccountResponse>) : AccountLocalDataSource {
    private val state = MutableStateFlow(accounts)

    override fun watchAccounts(): Flow<List<AccountResponse>> = state
    override suspend fun getAccounts(): List<AccountResponse> = state.value

    override suspend fun insertAccount(
        id: String,
        name: String,
        group: String,
        balance: Long,
        accountType: String
    ) = error("Not used by these tests")

    override suspend fun updateAccountBalance(accountId: String, delta: Long) =
        error("Not used by these tests")

    override suspend fun updateAccountType(accountId: String, accountType: String) =
        error("Not used by these tests")

    override suspend fun updateAccount(
        accountId: String,
        name: String,
        balance: Long,
        group: String,
        accountType: String
    ) = error("Not used by these tests")

    override suspend fun deleteAccount(accountId: String) = error("Not used by these tests")

    override suspend fun upsertAccounts(accounts: List<AccountResponse>) {
        val existing = state.value.associateBy { it.id }.toMutableMap()
        accounts.forEach { existing[it.id] = it }
        state.value = existing.values.toList()
    }

    override suspend fun clearAccounts() {
        state.value = emptyList()
    }
}

private class FakeTransactionLocalDataSource(
    transactions: List<TransactionResponse>
) : TransactionLocalDataSource {
    private val state = MutableStateFlow(transactions)

    override fun watchTransactions(
        startDate: String?,
        endDate: String?,
        isDraft: Boolean,
        transactionId: String?,
        expenseOnly: Boolean?
    ): Flow<List<TransactionResponse>> = state

    override suspend fun getTransactions(
        startDate: String?,
        endDate: String?,
        isDraft: Boolean,
        transactionId: String?
    ): List<TransactionResponse> = state.value

    override suspend fun getAllTransactions(): List<TransactionResponse> = state.value

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
        val existing = state.value.associateBy { it.id }.toMutableMap()
        transactions.forEach { existing[it.id] = it }
        state.value = existing.values.toList()
    }

    override suspend fun clearTransactions() {
        state.value = emptyList()
    }
}
