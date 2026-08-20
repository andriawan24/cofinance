package id.andriawan.cofinance.data.remote

import id.andriawan.cofinance.data.crypto.DataKey
import id.andriawan.cofinance.data.crypto.KeyWrapType
import id.andriawan.cofinance.data.keyring.DataKeyUnavailableException
import id.andriawan.cofinance.data.model.response.AccountResponse
import id.andriawan.cofinance.data.model.response.TransactionResponse
import id.andriawan.cofinance.utils.enums.TransactionType
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

/**
 * What the cloud data sources store, and what they give back.
 *
 * The assertions about exposure are made against the serialized stored document, the way
 * `RecordCipherTest.sealedRecordCarriesNoReadableFinanceValue` does, because that text is what a
 * backend export would contain. Asserting against the object graph would pass even if the payload
 * were stored beside the ciphertext.
 */
class EncryptedFinanceDataSourceTest {

    @Test
    fun storedAccountDocumentCarriesNoReadableFinanceValue() = runTest {
        val fixture = setUpFixture()

        fixture.accounts.upsertAccounts(listOf(BANK_ACCOUNT))

        val stored = fixture.store.storedText(FinanceCollection.ACCOUNTS, "account-1")
        assertFalse(stored.contains("Bank Mandiri"), "Stored document exposed the account name")
        assertFalse(stored.contains("Tabungan"), "Stored document exposed the account group")
        assertFalse(stored.contains("12450000"), "Stored document exposed the balance")
        assertFalse(stored.contains("REGULAR_BALANCE"), "Stored document exposed the account type")
        assertFalse(stored.contains("2026-08-17"), "Stored document exposed the creation time")
    }

    @Test
    fun storedAccountDocumentCarriesTheEnvelopeMetadata() = runTest {
        val fixture = setUpFixture()

        fixture.accounts.upsertAccounts(listOf(BANK_ACCOUNT))

        val stored = fixture.store.storedText(FinanceCollection.ACCOUNTS, "account-1")
        assertContains(stored, "envelope_version")
        assertContains(stored, "key_id")
        assertContains(stored, "nonce")
        assertContains(stored, "ciphertext")
        // The identifier is what addresses the document, and stays outside the ciphertext.
        assertEquals(setOf("account-1"), fixture.store.documentIds(FinanceCollection.ACCOUNTS))
    }

    @Test
    fun accountReadReturnsTheOriginalValues() = runTest {
        val fixture = setUpFixture()

        fixture.accounts.upsertAccounts(listOf(BANK_ACCOUNT))

        assertEquals(listOf(BANK_ACCOUNT), fixture.accounts.getAccounts())
    }

    @Test
    fun storedTransactionDocumentCarriesNoReadableFinanceValue() = runTest {
        val fixture = setUpFixture()

        fixture.transactions.upsertTransactions(listOf(GROCERY_TRANSACTION))

        val stored = fixture.store.storedText(FinanceCollection.TRANSACTIONS, "transaction-1")
        assertFalse(stored.contains("85000"), "Stored document exposed the amount")
        assertFalse(stored.contains("Groceries"), "Stored document exposed the category")
        assertFalse(stored.contains("2026-08-17"), "Stored document exposed the date")
        assertFalse(stored.contains("2500"), "Stored document exposed the fee")
        assertFalse(stored.contains("Belanja mingguan"), "Stored document exposed the notes")
        assertFalse(stored.contains("account-1"), "Stored document exposed the sender account")
        assertFalse(stored.contains("EXPENSE"), "Stored document exposed the transaction type")
        assertFalse(stored.contains("09:05:00"), "Stored document exposed the update time")
    }

    @Test
    fun transactionReadReturnsTheOriginalValues() = runTest {
        val fixture = setUpFixture()

        fixture.transactions.upsertTransactions(listOf(GROCERY_TRANSACTION))

        assertEquals(listOf(GROCERY_TRANSACTION), fixture.transactions.getTransactions())
    }

    @Test
    fun draftTransactionIsEncryptedOnTheSameTerms() = runTest {
        val fixture = setUpFixture()
        val draft = GROCERY_TRANSACTION.copy(
            id = "draft-1",
            notes = "Struk belum diperiksa",
            type = TransactionType.DRAFT.name
        )

        fixture.transactions.upsertTransactions(listOf(draft))

        val stored = fixture.store.storedText(FinanceCollection.TRANSACTIONS, "draft-1")
        assertFalse(stored.contains(TransactionType.DRAFT.name), "Stored draft exposed its type")
        assertFalse(stored.contains("85000"), "Stored draft exposed the amount")
        assertFalse(stored.contains("Groceries"), "Stored draft exposed the category")
        assertFalse(stored.contains("Struk belum diperiksa"), "Stored draft exposed the notes")
        assertEquals(listOf(draft), fixture.transactions.getTransactions())
    }

    @Test
    fun cycleResetTransactionIsEncryptedOnTheSameTerms() = runTest {
        val fixture = setUpFixture()
        val marker = GROCERY_TRANSACTION.copy(
            id = "cycle-reset-1",
            category = "CYCLE_RESET",
            notes = "Awal siklus",
            type = TransactionType.CYCLE_RESET.name
        )

        fixture.transactions.upsertTransactions(listOf(marker))

        val stored = fixture.store.storedText(FinanceCollection.TRANSACTIONS, "cycle-reset-1")
        assertFalse(stored.contains(TransactionType.CYCLE_RESET.name), "Stored marker exposed its type")
        assertFalse(stored.contains("Awal siklus"), "Stored marker exposed the notes")
        assertEquals(listOf(marker), fixture.transactions.getTransactions())
    }

    @Test
    fun rewritingTheSameRecordDrawsAFreshNonce() = runTest {
        val fixture = setUpFixture()

        fixture.accounts.upsertAccounts(listOf(BANK_ACCOUNT))
        val first = fixture.store.storedDocuments(FinanceCollection.ACCOUNTS).single().document
        fixture.accounts.upsertAccounts(listOf(BANK_ACCOUNT))
        val second = fixture.store.storedDocuments(FinanceCollection.ACCOUNTS).single().document

        // The mirror re-uploads the whole snapshot every time, so this is the ordinary case rather
        // than an edge one, and a repeated nonce here would be a repeated nonce in production.
        assertTrue(first.nonce != second.nonce, "Re-uploading a record reused its nonce")
        assertTrue(first.ciphertext != second.ciphertext, "Re-uploading a record reused its ciphertext")
    }

    @Test
    fun readsPreserveTheBackendOrderAndApplyNoFinanceOrdering() = runTest {
        val dataKey = DataKey.generate()
        val fixture = setUpFixture(dataKey)
        val older = GROCERY_TRANSACTION.copy(id = "older", date = "2026-01-01")
        val newer = GROCERY_TRANSACTION.copy(id = "newer", date = "2026-12-31")

        fixture.transactions.upsertTransactions(listOf(older, newer))

        // Nothing in the read path sorts by a finance field, because none of them exist in readable
        // form; the returned order is whatever the backend handed back, and ordering is Room's job.
        assertEquals(
            fixture.store.storedDocuments(FinanceCollection.TRANSACTIONS).map { it.id },
            fixture.transactions.getTransactions().map { it.id }
        )
    }

    @Test
    fun aRecordSealedUnderAnotherKeyIsNotImported() = runTest {
        val fixture = setUpFixture()
        fixture.accounts.upsertAccounts(listOf(BANK_ACCOUNT))

        val otherDevice = EncryptedRemoteFixture(
            store = fixture.store,
            session = unlockedSession(DataKey.generate())
        )

        assertEquals(emptyList<AccountResponse>(), otherDevice.accounts.getAccounts())
    }

    @Test
    fun aLockedSessionCanNeitherReadNorWrite() = runTest {
        val fixture = EncryptedRemoteFixture(session = lockedSession())
        fixture.store.seedKeyMaterial(keyMaterialWith("key-1", KeyWrapType.RecoveryPhrase))

        assertFailsWith<DataKeyUnavailableException> {
            fixture.accounts.upsertAccounts(listOf(BANK_ACCOUNT))
        }
        assertFailsWith<DataKeyUnavailableException> { fixture.accounts.getAccounts() }
        assertTrue(fixture.store.writeLog.isEmpty(), "A locked session wrote to the backend")
    }

    private suspend fun setUpFixture(dataKey: DataKey? = null): EncryptedRemoteFixture {
        val key = dataKey ?: DataKey.generate()
        val fixture = EncryptedRemoteFixture(session = unlockedSession(key))
        fixture.keyMaterialGate.publishKeyMaterial(
            keyMaterialWith(key.id, KeyWrapType.Device, KeyWrapType.RecoveryPhrase)
        )
        return fixture
    }

    private companion object {
        val BANK_ACCOUNT = AccountResponse(
            id = "account-1",
            name = "Bank Mandiri",
            group = "Tabungan",
            balance = 12_450_000L,
            accountType = "REGULAR_BALANCE",
            createdAt = "2026-08-17T09:00:00Z"
        )

        val GROCERY_TRANSACTION = TransactionResponse(
            id = "transaction-1",
            amount = 85_000L,
            category = "Groceries",
            date = "2026-08-17",
            fee = 2_500L,
            notes = "Belanja mingguan",
            senderAccountId = "account-1",
            createdAt = "2026-08-17T09:05:00Z",
            updatedAt = "2026-08-17T09:05:00Z",
            type = "EXPENSE"
        )
    }
}
