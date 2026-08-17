package id.andriawan.cofinance.data.migration

import id.andriawan.cofinance.data.crypto.DataKey
import id.andriawan.cofinance.data.crypto.EncryptedEnvelopeDocument
import id.andriawan.cofinance.data.crypto.KeyWrapType
import id.andriawan.cofinance.data.crypto.RecordCipher
import id.andriawan.cofinance.data.crypto.toDocument
import id.andriawan.cofinance.data.model.document.AccountDocument
import id.andriawan.cofinance.data.model.document.TransactionDocument
import id.andriawan.cofinance.data.model.response.AccountResponse
import id.andriawan.cofinance.data.model.response.TransactionResponse
import id.andriawan.cofinance.data.remote.EncryptedAccountDataSource
import id.andriawan.cofinance.data.remote.EncryptedTransactionDataSource
import id.andriawan.cofinance.data.remote.FinanceCollection
import id.andriawan.cofinance.data.remote.KeyMaterialGate
import id.andriawan.cofinance.data.remote.MissingKeyMaterialException
import id.andriawan.cofinance.data.remote.MutableSessionPolicy
import id.andriawan.cofinance.data.remote.keyMaterialWith
import id.andriawan.cofinance.data.remote.unlockedSession
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive

/**
 * Migration of records an earlier build synchronized in plaintext.
 *
 * The load-bearing assertion in this file is on the *order* of the stored operations, not on the
 * state migration ends in. Encrypt-then-delete and delete-then-encrypt reach the same end state, and
 * only one of them survives a process death between the two steps, so an end-state assertion cannot
 * distinguish the safe implementation from the one that destroys data. Every ordering test here
 * therefore reads `FakePlaintextFinanceDocumentStore.operations`.
 */
class PlaintextMigrationTest {

    // ---------------------------------------------------------------- 5.1 detection

    @Test
    fun plaintextAndEncryptedDocumentsAreDistinguishedInAMixedCollection() = runTest {
        val harness = Harness.create()
        harness.store.seedPlaintext(
            FinanceCollection.ACCOUNTS, "account-cash", AccountDocument.serializer(), CASH
        )
        harness.store.seedEncrypted(
            FinanceCollection.ACCOUNTS, "account-bank", harness.envelopeFor(BANK_RECORD)
        )
        harness.store.seedPlaintext(
            FinanceCollection.TRANSACTIONS, "txn-groceries", TransactionDocument.serializer(), GROCERIES
        )
        harness.store.seedEncrypted(
            FinanceCollection.TRANSACTIONS, "txn-salary", harness.envelopeFor(SALARY_RECORD)
        )

        val scanned = PlaintextRecordMigrator(harness.store).scan()

        assertEquals(
            setOf("accounts/account-cash", "transactions/txn-groceries"),
            scanned.filter { it.needsConversion }.map(ScannedRecord::toString).toSet()
        )
        assertEquals(
            setOf("accounts/account-bank", "transactions/txn-salary"),
            scanned.filter { it.isMigrated }.map(ScannedRecord::toString).toSet()
        )
    }

    @Test
    fun absenceOfTheEnvelopeVersionIsWhatMarksARecordUnmigrated() = runTest {
        val harness = Harness.create()
        harness.store.seedPlaintext(
            FinanceCollection.ACCOUNTS, "account-cash", AccountDocument.serializer(), CASH
        )

        val scanned = harness.store
            .readDocuments(FinanceCollection.ACCOUNTS, AccountDocument.serializer())
            .single()

        assertFalse(scanned.carriesEnvelope)
        assertTrue(scanned.needsConversion)
        assertFalse(
            EncryptedEnvelopeDocument.ENVELOPE_VERSION_FIELD in
                harness.store.fieldNames(FinanceCollection.ACCOUNTS, "account-cash")
        )
    }

    @Test
    fun aDocumentCarryingAnEnvelopeAndPlaintextAtOnceIsAnUnfinishedConversion() = runTest {
        val harness = Harness.create()
        harness.store.seedPlaintext(
            FinanceCollection.ACCOUNTS, "account-cash", AccountDocument.serializer(), CASH
        )
        harness.store.seedEncrypted(
            FinanceCollection.ACCOUNTS, "account-cash", harness.envelopeFor(CASH_RECORD)
        )

        val scanned = PlaintextRecordMigrator(harness.store).scan().single()

        assertFalse(scanned.isMigrated, "A record still carrying plaintext is not migrated")
        assertFalse(scanned.needsConversion, "Its ciphertext is already stored")
        assertTrue(scanned.needsPlaintextRemoval)
    }

    // ---------------------------------------------------------------- 5.2 per-record conversion

    @Test
    fun conversionWritesTheEnvelopeBeforeRemovingThePlaintext() = runTest {
        val harness = Harness.create().withKeyMaterial()
        harness.store.seedPlaintext(
            FinanceCollection.ACCOUNTS, "account-cash", AccountDocument.serializer(), CASH
        )

        harness.launch().run()

        assertEquals(
            listOf("write accounts/account-cash", "delete accounts/account-cash"),
            harness.store.operations
        )
    }

    @Test
    fun everyConvertedRecordWritesItsCiphertextBeforeItsPlaintextIsRemoved() = runTest {
        val harness = Harness.create().withKeyMaterial().seedMixedPlaintext()

        assertIs<MigrationState.Complete>(harness.launch().run())

        val operations = harness.store.operations
        val paths = operations.map { it.substringAfter(' ') }.toSet()
        assertEquals(5, paths.size, "Expected all five seeded records to be touched")
        paths.forEach { path ->
            val write = operations.indexOf("write $path")
            val delete = operations.indexOf("delete $path")
            assertTrue(write >= 0, "$path was never encrypted")
            assertTrue(delete >= 0, "$path kept its plaintext")
            assertTrue(
                write < delete,
                "$path removed plaintext at $delete before writing ciphertext at $write"
            )
        }
    }

    @Test
    fun aProcessDeathBetweenTheWriteAndTheDeleteLeavesTheRecordReadable() = runTest {
        val harness = Harness.create().withKeyMaterial()
        harness.store.seedPlaintext(
            FinanceCollection.ACCOUNTS, "account-cash", AccountDocument.serializer(), CASH
        )
        // One mutation is permitted: the envelope write. The plaintext removal never happens.
        harness.store.interruptAfter(1)

        assertIs<MigrationState.Failed>(harness.launch().run())

        assertEquals(listOf("write accounts/account-cash"), harness.store.operations)
        val fields = harness.store.fieldNames(FinanceCollection.ACCOUNTS, "account-cash")
        assertContains(fields, EncryptedEnvelopeDocument.ENVELOPE_VERSION_FIELD)
        assertContains(fields, "name")
        assertContains(
            charSequence = harness.store.storedText(FinanceCollection.ACCOUNTS, "account-cash"),
            other = CASH.name,
            message = "The interrupted record must still be readable, not destroyed"
        )
    }

    @Test
    fun resumingAnInterruptedRecordFinishesWithADeleteAndNeverResealsIt() = runTest {
        val harness = Harness.create().withKeyMaterial()
        harness.store.seedPlaintext(
            FinanceCollection.ACCOUNTS, "account-cash", AccountDocument.serializer(), CASH
        )
        harness.store.interruptAfter(1)
        harness.launch().run()
        val sealedByTheInterruptedRun =
            harness.store.readDocuments(FinanceCollection.ACCOUNTS).single().document

        harness.store.relaunch()
        val outcome = harness.launch().run()

        assertEquals(listOf("delete accounts/account-cash"), harness.store.operations)
        assertEquals(MigrationState.Complete(converted = 1, alreadyEncrypted = 0), outcome)
        assertEquals(
            sealedByTheInterruptedRun,
            harness.store.readDocuments(FinanceCollection.ACCOUNTS).single().document,
            "Resumption re-sealed a record that already carried ciphertext"
        )
    }

    @Test
    fun reRunningOverAConvertedRecordIsANoOp() = runTest {
        val harness = Harness.create().withKeyMaterial().seedMixedPlaintext()
        assertIs<MigrationState.Complete>(harness.launch().run())
        val afterFirstRun = harness.store.storedText(FinanceCollection.ACCOUNTS, "account-cash")

        harness.store.relaunch()
        val second = harness.launch().run()

        assertTrue(harness.store.operations.isEmpty(), "A second run mutated already-encrypted records")
        assertEquals(MigrationState.Complete(converted = 0, alreadyEncrypted = 5), second)
        assertEquals(
            afterFirstRun,
            harness.store.storedText(FinanceCollection.ACCOUNTS, "account-cash"),
            "A no-op run must not re-seal a record under a fresh nonce"
        )
    }

    // ---------------------------------------------------------------- 5.3 the flow

    @Test
    fun migrationResumesAfterInterruptionAndConvertsOnlyStillPlaintextRecords() = runTest {
        val harness = Harness.create().withKeyMaterial().seedMixedPlaintext()
        // Five mutations: two accounts fully converted, and the third left carrying both.
        harness.store.interruptAfter(5)

        assertIs<MigrationState.Failed>(harness.launch().run())
        assertEquals(
            listOf(
                "write accounts/account-cash",
                "delete accounts/account-cash",
                "write accounts/account-bank",
                "delete accounts/account-bank",
                "write accounts/account-ewallet"
            ),
            harness.store.operations
        )

        harness.store.relaunch()
        val resumed = harness.launch().run()

        assertEquals(
            listOf(
                // The half-converted account is finished with a delete, never a second seal.
                "delete accounts/account-ewallet",
                "write transactions/txn-groceries",
                "delete transactions/txn-groceries",
                "write transactions/txn-salary",
                "delete transactions/txn-salary"
            ),
            harness.store.operations
        )
        assertFalse(
            harness.store.operations.any { it == "write accounts/account-cash" || it == "write accounts/account-bank" },
            "A record converted before the interruption was converted again"
        )
        assertEquals(MigrationState.Complete(converted = 3, alreadyEncrypted = 2), resumed)
        assertTrue(assertIs<MigrationState.Complete>(resumed).isClean)
    }

    @Test
    fun keyMaterialIsPublishedBeforeTheFirstEncryptedRecordIsWritten() = runTest {
        val harness = Harness.create().seedMixedPlaintext()

        assertIs<MigrationState.Complete>(harness.launch().run())

        assertEquals(1, harness.setupRuns)
        assertEquals(
            FakePlaintextFinanceDocumentStore.KEY_MATERIAL_ENTRY,
            harness.store.operations.first(),
            "An encrypted record was written before its key material reached the backend"
        )
        assertEquals("write accounts/account-cash", harness.store.operations[1])
    }

    @Test
    fun migrationFailsWithoutWritingAnythingWhenKeyMaterialCannotBeSetUp() = runTest {
        val harness = Harness.create().seedMixedPlaintext()
        harness.setupPublishesKeyMaterial = false

        val outcome = harness.launch().run()

        assertIs<MissingKeyMaterialException>(assertIs<MigrationState.Failed>(outcome).cause)
        assertTrue(harness.store.operations.isEmpty(), "Something was written with no key material")
        assertContains(
            charSequence = harness.store.storedText(FinanceCollection.ACCOUNTS, "account-cash"),
            other = CASH.name,
            message = "A refused migration must leave the plaintext record intact"
        )
    }

    @Test
    fun aSignedOutUserIsNeitherMigratedNorSentThroughSetup() = runTest {
        val harness = Harness.create().seedMixedPlaintext()
        harness.sessionPolicy.userId = null

        val outcome = harness.launch().run()

        assertEquals(MigrationState.Complete(converted = 0, alreadyEncrypted = 0), outcome)
        assertEquals(0, harness.setupRuns)
        assertTrue(harness.store.operations.isEmpty())
    }

    @Test
    fun aSignedInUserWithNoPlaintextIsNotSentThroughSetup() = runTest {
        val harness = Harness.create().withKeyMaterial()
        harness.store.seedEncrypted(
            FinanceCollection.ACCOUNTS, "account-cash", harness.envelopeFor(CASH_RECORD)
        )

        val outcome = harness.launch().run()

        assertEquals(MigrationState.Complete(converted = 0, alreadyEncrypted = 1), outcome)
        assertEquals(0, harness.setupRuns, "Setup ran for a user with nothing to migrate")
        assertTrue(harness.store.operations.isEmpty())
    }

    @Test
    fun observedStatesProgressFromIdleThroughScanningAndConvertingToComplete() = runTest {
        val harness = Harness.create().withKeyMaterial().seedMixedPlaintext()
        val migration = harness.launch()
        val duringConversion = mutableListOf<MigrationState>()
        harness.store.onOperation = { duringConversion += migration.state.value }

        assertEquals(MigrationState.Idle, migration.state.value)
        val outcome = migration.run()

        assertEquals(MigrationState.Converting(finished = 0, total = 5), duringConversion.first())
        assertEquals(MigrationState.Converting(finished = 4, total = 5), duringConversion.last())
        assertTrue(
            duringConversion.map { assertIs<MigrationState.Converting>(it).finished }
                .zipWithNext()
                .all { (earlier, later) -> earlier <= later },
            "Progress went backwards"
        )
        assertEquals(outcome, migration.state.value)
        assertEquals(MigrationState.Complete(converted = 5, alreadyEncrypted = 0), outcome)
    }

    @Test
    fun anUnreadableRecordIsReportedRatherThanAbortingTheRun() = runTest {
        val harness = Harness.create().withKeyMaterial().seedMixedPlaintext()
        // A balance that is no longer a number: no retry will ever make this record convert.
        harness.store.seedFields(
            FinanceCollection.ACCOUNTS,
            "account-corrupt",
            mapOf("name" to JsonPrimitive("Broken"), "balance" to JsonPrimitive("not-a-number"))
        )

        val outcome = assertIs<MigrationState.Complete>(harness.launch().run())

        assertEquals(5, outcome.converted, "One bad record stopped the other five")
        assertEquals(
            listOf(UnconvertedRecord(FinanceCollection.ACCOUNTS, "account-corrupt", UNREADABLE)),
            outcome.unconverted
        )
        assertFalse(outcome.isClean, "A run that left plaintext behind reported a clean finish")
        assertFalse(
            harness.store.operations.any { it.endsWith("accounts/account-corrupt") },
            "The unreadable record was written to despite not converting"
        )
    }

    // ---------------------------------------------------------------- 5.4 completion

    @Test
    fun migrationLeavesNoPlaintextFinanceFieldInAnyCollection() = runTest {
        val harness = Harness.create().withKeyMaterial().seedMixedPlaintext()

        assertTrue(assertIs<MigrationState.Complete>(harness.launch().run()).isClean)

        val envelopeFields = setOf(
            EncryptedEnvelopeDocument.ENVELOPE_VERSION_FIELD,
            EncryptedEnvelopeDocument.KEY_ID_FIELD,
            "nonce",
            "ciphertext"
        )
        FinanceCollection.entries.forEach { collection ->
            harness.store.ids(collection).forEach { id ->
                assertEquals(
                    envelopeFields,
                    harness.store.fieldNames(collection, id),
                    "${collection.path}/$id kept fields beyond the envelope"
                )
                val stored = harness.store.storedText(collection, id)
                READABLE_VALUES.forEach { value ->
                    assertFalse(
                        stored.contains(value),
                        "${collection.path}/$id still exposes \"$value\" in $stored"
                    )
                }
            }
        }
    }

    @Test
    fun everyMigratedDocumentCarriesTheCurrentEnvelopeAndThisUsersKeyIdentifier() = runTest {
        val harness = Harness.create().withKeyMaterial().seedMixedPlaintext()

        assertTrue(assertIs<MigrationState.Complete>(harness.launch().run()).isClean)

        FinanceCollection.entries.forEach { collection ->
            harness.store.readDocuments(collection).forEach { stored ->
                assertEquals(1, stored.document.envelopeVersion, "${collection.path}/${stored.id}")
                assertEquals(harness.dataKey.id, stored.document.keyId)
                assertTrue(stored.document.ciphertext.isNotEmpty())
                assertTrue(stored.document.nonce.isNotEmpty())
            }
        }
    }

    @Test
    fun migratedRecordsReadBackThroughTheProductionEncryptedSources() = runTest {
        val harness = Harness.create().withKeyMaterial().seedMixedPlaintext()
        assertTrue(assertIs<MigrationState.Complete>(harness.launch().run()).isClean)

        val cipher = RecordCipher()
        val accounts = EncryptedAccountDataSource(
            harness.store, harness.gate(), harness.session, cipher
        ).getAccounts()
        val transactions = EncryptedTransactionDataSource(
            harness.store, harness.gate(), harness.session, cipher
        ).getTransactions()

        assertEquals(listOf(CASH_RECORD, BANK_RECORD, EWALLET_RECORD), accounts)
        assertEquals(listOf(GROCERIES_RECORD, SALARY_RECORD), transactions)
    }

    /** One installation over one cloud scope; [launch] stands in for a fresh app process. */
    private class Harness private constructor(val dataKey: DataKey) {

        val store = FakePlaintextFinanceDocumentStore()
        val sessionPolicy = MutableSessionPolicy()
        val session = unlockedSession(dataKey)

        var setupRuns = 0
        var setupPublishesKeyMaterial = true

        private val setup = EncryptionSetup {
            setupRuns++
            if (setupPublishesKeyMaterial && store.readKeyMaterial() == null) {
                gate().publishKeyMaterial(
                    keyMaterialWith(dataKey.id, KeyWrapType.Device, KeyWrapType.RecoveryPhrase)
                )
            }
        }

        fun gate(): KeyMaterialGate = KeyMaterialGate(store, sessionPolicy)

        fun launch(): PlaintextMigration = PlaintextMigration(
            migrator = PlaintextRecordMigrator(store),
            keyMaterialGate = gate(),
            encryptionSession = session,
            encryptionSetup = setup,
            sessionPolicy = sessionPolicy
        )

        /** Key material an earlier setup already published, so migration has only records to do. */
        fun withKeyMaterial() = apply {
            store.seedKeyMaterial(keyMaterialWith(dataKey.id, KeyWrapType.RecoveryPhrase))
        }

        fun seedMixedPlaintext() = apply {
            store.seedPlaintext(FinanceCollection.ACCOUNTS, "account-cash", AccountDocument.serializer(), CASH)
            store.seedPlaintext(FinanceCollection.ACCOUNTS, "account-bank", AccountDocument.serializer(), BANK)
            store.seedPlaintext(FinanceCollection.ACCOUNTS, "account-ewallet", AccountDocument.serializer(), EWALLET)
            store.seedPlaintext(FinanceCollection.TRANSACTIONS, "txn-groceries", TransactionDocument.serializer(), GROCERIES)
            store.seedPlaintext(FinanceCollection.TRANSACTIONS, "txn-salary", TransactionDocument.serializer(), SALARY)
        }

        suspend fun envelopeFor(record: AccountResponse): EncryptedEnvelopeDocument =
            RecordCipher().seal(record, AccountResponse.serializer(), dataKey).toDocument()

        suspend fun envelopeFor(record: TransactionResponse): EncryptedEnvelopeDocument =
            RecordCipher().seal(record, TransactionResponse.serializer(), dataKey).toDocument()

        companion object {
            suspend fun create(): Harness = Harness(DataKey.generate())
        }
    }

    private companion object {

        const val UNREADABLE = "Stored plaintext could not be read as a finance record"

        val CASH = AccountDocument(
            name = "Dompet Tunai",
            group = "Cash",
            balance = 4_250_000,
            accountType = "CASH",
            createdAt = "2026-01-05T09:00:00Z"
        )

        val BANK = AccountDocument(
            name = "BCA Payroll",
            group = "Bank",
            balance = 18_900_000,
            accountType = "BANK",
            createdAt = "2026-01-06T09:00:00Z"
        )

        val EWALLET = AccountDocument(
            name = "GoPay Harian",
            group = "E-Wallet",
            balance = 316_000,
            accountType = "EWALLET",
            createdAt = "2026-01-07T09:00:00Z"
        )

        val GROCERIES = TransactionDocument(
            amount = 137_500,
            category = "Groceries",
            date = "2026-02-01",
            fee = 2_500,
            notes = "Belanja mingguan Indomaret",
            senderAccountId = "account-cash",
            type = "EXPENSE",
            createdAt = "2026-02-01T10:15:00Z",
            updatedAt = "2026-02-01T10:15:00Z"
        )

        val SALARY = TransactionDocument(
            amount = 12_000_000,
            category = "Salary",
            date = "2026-02-25",
            notes = "Gaji bulanan",
            senderAccountId = "account-bank",
            receiverAccountId = "account-ewallet",
            type = "INCOME",
            createdAt = "2026-02-25T02:00:00Z",
            updatedAt = "2026-02-25T02:05:00Z"
        )

        val CASH_RECORD = CASH.asResponse("account-cash")
        val BANK_RECORD = BANK.asResponse("account-bank")
        val EWALLET_RECORD = EWALLET.asResponse("account-ewallet")
        val GROCERIES_RECORD = GROCERIES.asResponse("txn-groceries")
        val SALARY_RECORD = SALARY.asResponse("txn-salary")

        /**
         * Distinctive values that must not survive anywhere in a stored document.
         *
         * All are at least six characters or carry a character Base64 never produces, so a match
         * means the value is genuinely readable rather than a coincidence inside the ciphertext.
         */
        val READABLE_VALUES = listOf(
            CASH.name, CASH.balance.toString(), CASH.createdAt,
            BANK.name, BANK.balance.toString(),
            EWALLET.name, EWALLET.group, EWALLET.accountType, EWALLET.balance.toString(),
            GROCERIES.category, GROCERIES.notes, GROCERIES.date, GROCERIES.amount.toString(),
            SALARY.category, SALARY.notes, SALARY.date, SALARY.amount.toString(),
            "account-cash", "account-bank", "account-ewallet"
        )

        fun AccountDocument.asResponse(id: String) = AccountResponse(
            id = id,
            name = name,
            group = group,
            balance = balance,
            accountType = accountType,
            createdAt = createdAt
        )

        fun TransactionDocument.asResponse(id: String) = TransactionResponse(
            id = id,
            amount = amount,
            category = category,
            date = date,
            fee = fee,
            notes = notes,
            senderAccountId = senderAccountId,
            receiverAccountId = receiverAccountId,
            type = type,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}
