package id.andriawan.cofinance.pages.encryption

import id.andriawan.cofinance.data.crypto.DeviceKeyWrapper
import id.andriawan.cofinance.data.crypto.EncryptedEnvelopeDocument
import id.andriawan.cofinance.data.crypto.FakeDeviceKeyVault
import id.andriawan.cofinance.data.crypto.InMemoryRecoveryPhraseVault
import id.andriawan.cofinance.data.crypto.KeyMaterialDocument
import id.andriawan.cofinance.data.crypto.RecordCipher
import id.andriawan.cofinance.data.crypto.RecoveryPhraseKeyWrapper
import id.andriawan.cofinance.data.keyring.InMemoryEncryptionSession
import id.andriawan.cofinance.data.local.account.AccountLocalDataSource
import id.andriawan.cofinance.data.local.transaction.TransactionLocalDataSource
import id.andriawan.cofinance.data.lock.FakeLocalKeyMaterialStore
import id.andriawan.cofinance.data.model.response.AccountResponse
import id.andriawan.cofinance.data.model.response.TransactionResponse
import id.andriawan.cofinance.data.remote.EncryptedAccountDataSource
import id.andriawan.cofinance.data.remote.EncryptedTransactionDataSource
import id.andriawan.cofinance.data.remote.FakeFinanceDocumentStore
import id.andriawan.cofinance.data.remote.FinanceCollection
import id.andriawan.cofinance.data.remote.FinanceDocumentStore
import id.andriawan.cofinance.data.remote.KeyMaterialGate
import id.andriawan.cofinance.data.remote.MutableSessionPolicy
import id.andriawan.cofinance.data.remote.StoredFinanceDocument
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * The collaborators the two encryption screens share, wired as they are in the graph.
 *
 * The wrappers and the cipher are the real ones rather than doubles, because what these tests are
 * about is whether a phrase actually opens the key material a previous device published. Only the
 * document store, the device key storage, and the local database are faked, which are exactly the
 * three things a host test cannot have.
 */
class EncryptionFixture(
    val store: FakeFinanceDocumentStore = FakeFinanceDocumentStore(),
    val session: InMemoryEncryptionSession = InMemoryEncryptionSession(),
    val sessionPolicy: MutableSessionPolicy = MutableSessionPolicy(),
    documentStore: FinanceDocumentStore = store
) {
    val cipher: RecordCipher = RecordCipher()
    val keyMaterialGate: KeyMaterialGate = KeyMaterialGate(documentStore, sessionPolicy)
    val deviceKeyWrapper: DeviceKeyWrapper = DeviceKeyWrapper(FakeDeviceKeyVault())
    val recoveryPhraseKeyWrapper: RecoveryPhraseKeyWrapper = RecoveryPhraseKeyWrapper()

    /** The real sealer over a field, so what setup and restore keep is actually openable. */
    val recoveryPhraseVault: InMemoryRecoveryPhraseVault = InMemoryRecoveryPhraseVault()
    val localKeyMaterialStore: FakeLocalKeyMaterialStore = FakeLocalKeyMaterialStore()

    val remoteAccounts: EncryptedAccountDataSource =
        EncryptedAccountDataSource(documentStore, keyMaterialGate, session, cipher)

    val remoteTransactions: EncryptedTransactionDataSource =
        EncryptedTransactionDataSource(documentStore, keyMaterialGate, session, cipher)

    val localAccounts: FakeAccountLocalDataSource = FakeAccountLocalDataSource()
    val localTransactions: FakeTransactionLocalDataSource = FakeTransactionLocalDataSource()

    fun setupViewModel(random: kotlin.random.Random = kotlin.random.Random(7)) =
        EncryptionSetupViewModel(
            encryptionSession = session,
            keyMaterialGate = keyMaterialGate,
            deviceKeyWrapper = deviceKeyWrapper,
            recoveryPhraseKeyWrapper = recoveryPhraseKeyWrapper,
            localKeyMaterialStore = localKeyMaterialStore,
            recoveryPhraseVault = recoveryPhraseVault,
            random = random
        )

    fun restoreViewModel() = RecoveryPhraseRestoreViewModel(
        encryptionSession = session,
        keyMaterialGate = keyMaterialGate,
        recoveryPhraseKeyWrapper = recoveryPhraseKeyWrapper,
        deviceKeyWrapper = deviceKeyWrapper,
        localKeyMaterialStore = localKeyMaterialStore,
        recoveryPhraseVault = recoveryPhraseVault,
        remoteAccountSource = remoteAccounts,
        remoteTransactionSource = remoteTransactions,
        localAccountSource = localAccounts,
        localTransactionSource = localTransactions
    )
}

/**
 * A store that reads normally until [failReadsFrom] is set, standing in for a connection that drops
 * part-way through a restore.
 *
 * This is the only way to reach the case the restore flow is built to survive: a phrase that is
 * genuinely correct, and a download that does not finish.
 */
class FlakyFinanceDocumentStore(
    private val delegate: FakeFinanceDocumentStore
) : FinanceDocumentStore {

    var failReadsFrom: FinanceCollection? = null

    override suspend fun readKeyMaterial(): KeyMaterialDocument? = delegate.readKeyMaterial()

    override suspend fun writeKeyMaterial(material: KeyMaterialDocument) =
        delegate.writeKeyMaterial(material)

    override suspend fun readDocuments(collection: FinanceCollection): List<StoredFinanceDocument> {
        if (collection == failReadsFrom) error("The connection dropped reading ${collection.path}")
        return delegate.readDocuments(collection)
    }

    override suspend fun writeDocument(
        collection: FinanceCollection,
        id: String,
        document: EncryptedEnvelopeDocument
    ) = delegate.writeDocument(collection, id, document)
}

class FakeAccountLocalDataSource(accounts: List<AccountResponse> = emptyList()) :
    AccountLocalDataSource {

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

class FakeTransactionLocalDataSource(transactions: List<TransactionResponse> = emptyList()) :
    TransactionLocalDataSource {

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

/** The account a "previous device" had, used to prove a restore brought the real values back. */
val SEEDED_ACCOUNT = AccountResponse(
    id = "account-1",
    name = "Bank Mandiri",
    group = "Tabungan",
    balance = 12_450_000L,
    accountType = "REGULAR_BALANCE",
    createdAt = "2026-08-17T09:00:00Z"
)

val SEEDED_TRANSACTION = TransactionResponse(
    id = "transaction-1",
    amount = 85_000L,
    category = "Groceries",
    date = "2026-08-17",
    type = "EXPENSE"
)
