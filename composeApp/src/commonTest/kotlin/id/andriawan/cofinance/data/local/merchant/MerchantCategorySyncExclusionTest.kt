package id.andriawan.cofinance.data.local.merchant

import id.andriawan.cofinance.data.crypto.DataKey
import id.andriawan.cofinance.data.keyring.EncryptionSession
import id.andriawan.cofinance.data.keyring.InMemoryEncryptionSession
import id.andriawan.cofinance.data.local.account.AccountLocalDataSource
import id.andriawan.cofinance.data.local.transaction.TransactionLocalDataSource
import id.andriawan.cofinance.data.model.AccountResponse
import id.andriawan.cofinance.data.model.TransactionResponse
import id.andriawan.cofinance.data.ocr.OcrBlock
import id.andriawan.cofinance.data.ocr.OcrLine
import id.andriawan.cofinance.data.ocr.OcrRect
import id.andriawan.cofinance.data.ocr.OcrResult
import id.andriawan.cofinance.data.ocr.parser.MerchantCategoryLearning
import id.andriawan.cofinance.data.remote.AccountRemoteDataSource
import id.andriawan.cofinance.data.remote.TransactionRemoteDataSource
import id.andriawan.cofinance.data.session.SessionPolicy
import id.andriawan.cofinance.data.sync.FirebaseSyncCoordinator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Learned merchant categories are device-local. The sync coordinator has no dependency on
 * the association store, and these tests hold that boundary in place.
 */
class MerchantCategorySyncExclusionTest {

    @Test
    fun syncPathNeverReadsTheAssociationStore() = runTest {
        val associations = CountingMerchantCategoryStore()
        associations.recordAssociation("TOKO SERBA JAYA", "HOUSING")
        val fixture = SyncFixture(encryptionSession = unlockedTestSession())

        fixture.coordinator.syncDataAfterSignIn()
        fixture.coordinator.mirrorDataIfSignedIn()
        fixture.coordinator.clearLocalData()

        assertEquals(0, associations.readCount)
        assertEquals(1, associations.associationCount)
    }

    @Test
    fun noAssociationDataIsUploaded() = runTest {
        val associations = CountingMerchantCategoryStore()
        val learning = MerchantCategoryLearning(associations)
        val fixture = SyncFixture(
            encryptionSession = unlockedTestSession(),
            localTransactions = listOf(
                TransactionResponse(id = "t1", amount = 55_000, category = "FOOD", date = "2026-01-02")
            )
        )

        learning.parse(receiptHeaderedBy("TOKO SERBA JAYA"))
        learning.recordCategoryCorrection("HOUSING")
        val readsBeforeSync = associations.readCount

        fixture.coordinator.syncDataAfterSignIn()

        assertEquals(1, associations.associationCount)
        assertEquals(readsBeforeSync, associations.readCount)
        assertTrue(
            fixture.remoteAccounts.uploaded.none { it.toString().contains("TOKO SERBA JAYA") } &&
                fixture.remoteTransactions.uploaded.none { it.toString().contains("TOKO SERBA JAYA") },
            "no uploaded record may carry a learned merchant association"
        )
    }
}

private fun receiptHeaderedBy(merchant: String) = OcrResult(
    listOf(
        OcrBlock(
            text = merchant,
            lines = listOf(
                OcrLine(merchant, OcrRect(left = 0.05f, top = 0.04f, right = 0.35f, bottom = 0.06f))
            )
        )
    )
)

/** The sync path runs unlocked here; refusal while locked is covered by `EncryptedSyncGatingTest`. */
private suspend fun unlockedTestSession(): EncryptionSession =
    InMemoryEncryptionSession().apply { unlock(DataKey.generate()) }

private class SyncFixture(
    private val encryptionSession: EncryptionSession,
    localAccountRecords: List<AccountResponse> = emptyList(),
    localTransactions: List<TransactionResponse> = emptyList()
) {
    val remoteAccounts = RecordingAccountRemoteDataSource()
    val remoteTransactions = RecordingTransactionRemoteDataSource()
    val coordinator = FirebaseSyncCoordinator(
        FakeAccountLocalDataSource(localAccountRecords),
        FakeTransactionLocalDataSource(localTransactions),
        remoteAccounts,
        remoteTransactions,
        object : SessionPolicy {
            override fun isSignedIn(): Boolean = true
            override fun userIdOrNull(): String = "firebase-user"
        },
        encryptionSession
    )
}

private class CountingMerchantCategoryStore : MerchantCategoryLocalDataSource {
    private val associations = mutableMapOf<String, String>()
    var readCount = 0
        private set

    val associationCount: Int get() = associations.size

    override suspend fun getAssociations(): Map<String, String> {
        readCount++
        return associations.toMap()
    }

    override suspend fun getAssociation(merchantKey: String): String? {
        readCount++
        return associations[merchantKey]
    }

    override suspend fun recordAssociation(merchantKey: String, category: String) {
        associations[merchantKey] = category
    }

    override suspend fun clearAssociations() {
        associations.clear()
    }
}

private class RecordingAccountRemoteDataSource : AccountRemoteDataSource {
    var uploaded: List<AccountResponse> = emptyList()
        private set

    override suspend fun getAccounts(): List<AccountResponse> = emptyList()

    override suspend fun upsertAccounts(accounts: List<AccountResponse>) {
        uploaded = uploaded + accounts
    }
}

private class RecordingTransactionRemoteDataSource : TransactionRemoteDataSource {
    var uploaded: List<TransactionResponse> = emptyList()
        private set

    override suspend fun getTransactions(): List<TransactionResponse> = emptyList()

    override suspend fun upsertTransactions(transactions: List<TransactionResponse>) {
        uploaded = uploaded + transactions
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
