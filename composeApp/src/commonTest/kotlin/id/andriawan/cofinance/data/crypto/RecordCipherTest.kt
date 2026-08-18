package id.andriawan.cofinance.data.crypto

import id.andriawan.cofinance.data.model.response.AccountResponse
import id.andriawan.cofinance.data.model.response.TransactionResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

class RecordCipherTest {

    private val cipher = RecordCipher()

    @Test
    fun accountRoundTripsToTheSameRecord() = runTest {
        val key = DataKey.generate()
        val account = AccountResponse(
            id = "account-1",
            name = "BCA",
            group = "Bank",
            balance = 12_450_000L,
            accountType = "SAVINGS",
            createdAt = "2026-08-17T09:00:00Z"
        )

        val envelope = cipher.seal(account, AccountResponse.serializer(), key)
        val decrypted = cipher.open(envelope, AccountResponse.serializer(), key)

        assertEquals(account, decrypted)
    }

    @Test
    fun transactionRoundTripsToTheSameRecord() = runTest {
        val key = DataKey.generate()
        val transaction = TransactionResponse(
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

        val envelope = cipher.seal(transaction, TransactionResponse.serializer(), key)
        val decrypted = cipher.open(envelope, TransactionResponse.serializer(), key)

        assertEquals(transaction, decrypted)
    }

    @Test
    fun sealedRecordCarriesNoReadableFinanceValue() = runTest {
        val key = DataKey.generate()
        val account = AccountResponse(id = "account-1", name = "BCA", balance = 12_450_000L)

        val stored = Json.encodeToString(
            EncryptedEnvelopeDocument.serializer(),
            cipher.seal(account, AccountResponse.serializer(), key).toDocument()
        )

        assertFalse(stored.contains("12450000"), "Stored document exposed the balance")
        assertFalse(stored.contains("BCA"), "Stored document exposed the account name")
    }

    @Test
    fun modifiedCiphertextFailsAndReturnsNothing() = runTest {
        val key = DataKey.generate()
        val envelope = cipher.seal(
            AccountResponse(id = "account-1", balance = 1_000L),
            AccountResponse.serializer(),
            key
        )

        val tampered = EncryptedEnvelope(
            version = envelope.version,
            keyId = envelope.keyId,
            nonce = envelope.nonce,
            ciphertext = envelope.ciphertext.copyOf().also { it[0] = (it[0] + 1).toByte() }
        )

        assertFailsWith<EncryptedRecordException> {
            cipher.open(tampered, AccountResponse.serializer(), key)
        }
    }

    @Test
    fun modifiedNonceFailsAndReturnsNothing() = runTest {
        val key = DataKey.generate()
        val envelope = cipher.seal(
            AccountResponse(id = "account-1", balance = 1_000L),
            AccountResponse.serializer(),
            key
        )

        val tampered = EncryptedEnvelope(
            version = envelope.version,
            keyId = envelope.keyId,
            nonce = envelope.nonce.copyOf().also { it[0] = (it[0] + 1).toByte() },
            ciphertext = envelope.ciphertext
        )

        assertFailsWith<EncryptedRecordException> {
            cipher.open(tampered, AccountResponse.serializer(), key)
        }
    }

    @Test
    fun anotherKeyCannotOpenTheRecord() = runTest {
        val key = DataKey.generate()
        val other = DataKey.generate()
        val envelope = cipher.seal(
            AccountResponse(id = "account-1", balance = 1_000L),
            AccountResponse.serializer(),
            key
        )

        assertFailsWith<EncryptedRecordException> {
            cipher.open(envelope, AccountResponse.serializer(), other)
        }
    }

    @Test
    fun sameKeyBytesReopenTheRecord() = runTest {
        val key = DataKey.generate()
        val account = AccountResponse(id = "account-1", balance = 1_000L)
        val envelope = cipher.seal(account, AccountResponse.serializer(), key)

        val restored = DataKey.fromRawBytes(key.id, key.exportRawBytes())

        assertEquals(account, cipher.open(envelope, AccountResponse.serializer(), restored))
    }

    @Test
    fun unknownEnvelopeVersionIsRejected() = runTest {
        val key = DataKey.generate()
        val envelope = cipher.seal(
            AccountResponse(id = "account-1", balance = 1_000L),
            AccountResponse.serializer(),
            key
        )

        val future = EncryptedEnvelope(
            version = envelope.version + 1,
            keyId = envelope.keyId,
            nonce = envelope.nonce,
            ciphertext = envelope.ciphertext
        )

        assertFailsWith<EncryptedRecordException> {
            cipher.open(future, AccountResponse.serializer(), key)
        }
    }

    @Test
    fun recordSealedBeforeAFieldWasAddedStillOpens() = runTest {
        val key = DataKey.generate()
        val account = AccountResponse(id = "account-1", name = "BCA", balance = 1_000L)
        val envelope = cipher.seal(account, AccountResponse.serializer(), key)

        // A build whose model has since lost a field must still read the older record rather than
        // failing the whole import.
        val opened = cipher.open(envelope, ReducedAccount.serializer(), key)

        assertEquals("account-1", opened.id)
        assertTrue(envelope.keyId == key.id)
    }

    @kotlinx.serialization.Serializable
    private data class ReducedAccount(val id: String? = null)
}
