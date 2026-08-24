package id.andriawan.cofinance.data.remote

import id.andriawan.cofinance.data.crypto.DataKey
import id.andriawan.cofinance.data.crypto.KeyMaterialDocument
import id.andriawan.cofinance.data.crypto.KeyWrapType
import id.andriawan.cofinance.data.model.AccountResponse
import id.andriawan.cofinance.data.model.TransactionResponse
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

/**
 * What reaches the backend as key material, and when.
 *
 * Both properties are correctness rather than hygiene. A record written before its key material
 * exists is unrecoverable on any other device, and a device or PIN wrap that reached the backend
 * would hand an attacker material the recovery-phrase-only rule was chosen to withhold.
 */
class KeyMaterialUploadTest {

    @Test
    fun keyMaterialIsWrittenBeforeTheFirstEncryptedRecord() = runTest {
        val dataKey = DataKey.generate()
        val fixture = EncryptedRemoteFixture(session = unlockedSession(dataKey))

        fixture.keyMaterialGate.publishKeyMaterial(
            keyMaterialWith(dataKey.id, KeyWrapType.Device, KeyWrapType.RecoveryPhrase)
        )
        fixture.accounts.upsertAccounts(listOf(ACCOUNT))
        fixture.transactions.upsertTransactions(listOf(TRANSACTION))

        assertEquals(
            listOf(
                FakeFinanceDocumentStore.KEY_MATERIAL_ENTRY,
                "accounts/account-1",
                "transactions/transaction-1"
            ),
            fixture.store.writeLog
        )
    }

    @Test
    fun accountsWrittenWithoutKeyMaterialAreRefusedAndStoreNothing() = runTest {
        val fixture = EncryptedRemoteFixture(session = unlockedSession(DataKey.generate()))

        assertFailsWith<MissingKeyMaterialException> {
            fixture.accounts.upsertAccounts(listOf(ACCOUNT))
        }

        assertTrue(fixture.store.writeLog.isEmpty(), "An account was stored before its key material")
    }

    @Test
    fun transactionsWrittenWithoutKeyMaterialAreRefusedAndStoreNothing() = runTest {
        val fixture = EncryptedRemoteFixture(session = unlockedSession(DataKey.generate()))

        assertFailsWith<MissingKeyMaterialException> {
            fixture.transactions.upsertTransactions(listOf(TRANSACTION))
        }

        assertTrue(fixture.store.writeLog.isEmpty(), "A transaction was stored before its key material")
    }

    @Test
    fun keyMaterialStoredByAnEarlierSessionSatisfiesTheGate() = runTest {
        val dataKey = DataKey.generate()
        val fixture = EncryptedRemoteFixture(session = unlockedSession(dataKey))
        fixture.store.seedKeyMaterial(keyMaterialWith(dataKey.id, KeyWrapType.RecoveryPhrase))

        fixture.accounts.upsertAccounts(listOf(ACCOUNT))

        assertEquals(listOf("accounts/account-1"), fixture.store.writeLog)
    }

    @Test
    fun keyMaterialConfirmedForOneUserDoesNotSatisfyTheNext() = runTest {
        val dataKey = DataKey.generate()
        val fixture = EncryptedRemoteFixture(session = unlockedSession(dataKey))
        fixture.store.seedKeyMaterial(keyMaterialWith(dataKey.id, KeyWrapType.RecoveryPhrase))
        fixture.accounts.upsertAccounts(listOf(ACCOUNT))

        // A different account signs in on the same device, against a scope where setup never ran.
        fixture.sessionPolicy.userId = "another-user"
        fixture.store.clearKeyMaterial()

        assertFailsWith<MissingKeyMaterialException> {
            fixture.accounts.upsertAccounts(listOf(ACCOUNT))
        }
        assertEquals(listOf("accounts/account-1"), fixture.store.writeLog)
    }

    @Test
    fun onlyTheRecoveryPhraseWrapIsUploaded() = runTest {
        val dataKey = DataKey.generate()
        val fixture = EncryptedRemoteFixture(session = unlockedSession(dataKey))

        fixture.keyMaterialGate.publishKeyMaterial(
            keyMaterialWith(
                dataKey.id,
                KeyWrapType.Device,
                KeyWrapType.RecoveryPhrase,
                KeyWrapType.Pin
            )
        )

        val uploaded = requireNotNull(fixture.store.readKeyMaterial())
        assertEquals(listOf(KeyWrapType.RecoveryPhrase.id), uploaded.wrappedKeys.map { it.wrapType })

        val text = fixture.store.storedKeyMaterialText()
        assertContains(text, KeyWrapType.RecoveryPhrase.id)
        assertFalse(text.contains(KeyWrapType.Device.id), "The device wrap reached the backend")
        assertFalse(text.contains(KeyWrapType.Pin.id), "The PIN wrap reached the backend")
    }

    @Test
    fun keyMaterialWithoutAPhraseWrapIsRefused() = runTest {
        val dataKey = DataKey.generate()
        val fixture = EncryptedRemoteFixture(session = unlockedSession(dataKey))

        // Uploading device-only material would open the gate while leaving the records reachable
        // from exactly one device, which is the failure the gate exists to prevent.
        assertFailsWith<MissingKeyMaterialException> {
            fixture.keyMaterialGate.publishKeyMaterial(
                keyMaterialWith(dataKey.id, KeyWrapType.Device, KeyWrapType.Pin)
            )
        }
        assertTrue(fixture.store.writeLog.isEmpty(), "Unusable key material reached the backend")
    }

    @Test
    fun emptyKeyMaterialIsRefused() = runTest {
        val fixture = EncryptedRemoteFixture(session = unlockedSession(DataKey.generate()))

        assertFailsWith<MissingKeyMaterialException> {
            fixture.keyMaterialGate.publishKeyMaterial(KeyMaterialDocument())
        }
    }

    private companion object {
        val ACCOUNT = AccountResponse(
            id = "account-1",
            name = "Bank Mandiri",
            group = "Tabungan",
            balance = 12_450_000L,
            accountType = "REGULAR_BALANCE",
            createdAt = "2026-08-17T09:00:00Z"
        )

        val TRANSACTION = TransactionResponse(
            id = "transaction-1",
            amount = 85_000L,
            category = "Groceries",
            date = "2026-08-17",
            type = "EXPENSE"
        )
    }
}
