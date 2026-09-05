package id.andriawan.cofinance.data.remote

import id.andriawan.cofinance.data.crypto.EncryptedRecordException
import id.andriawan.cofinance.data.crypto.RecordCipher
import id.andriawan.cofinance.data.crypto.toDocument
import id.andriawan.cofinance.data.crypto.toEnvelope
import id.andriawan.cofinance.data.keyring.EncryptionSession
import id.andriawan.cofinance.data.model.TransactionResponse

/**
 * The cloud transaction source: sealed before the write, opened after the read.
 *
 * Amount, category, date, fee, notes, both account identifiers, the transaction type, and both
 * timestamps are inside the ciphertext. `updatedAt` is encrypted with the rest: keeping it readable
 * would allow incremental synchronization, which this coordinator does not perform, at the cost of
 * publishing when the user records their spending.
 *
 * Every row the local source hands over is sealed on the same terms, including DRAFT rows from an
 * unfinished receipt scan and the CYCLE_RESET markers. They reach the mirror today, so excluding
 * them would change what synchronization means rather than how it is protected.
 */
class EncryptedTransactionDataSource(
    private val store: FinanceDocumentStore,
    private val keyMaterialGate: KeyMaterialGate,
    private val encryptionSession: EncryptionSession,
    private val cipher: RecordCipher
) : TransactionRemoteDataSource {

    override suspend fun getTransactions(): List<TransactionResponse> {
        val dataKey = encryptionSession.requireDataKey()
        return store.readDocuments(FinanceCollection.TRANSACTIONS).mapNotNull { stored ->
            try {
                cipher.open(stored.document.toEnvelope(), TransactionResponse.serializer(), dataKey)
                    .copy(id = stored.id)
            } catch (_: EncryptedRecordException) {
                // As in the account source: a record that will not open is not imported, and one
                // unreadable row does not fail the rest of the import.
                null
            }
        }
    }

    override suspend fun upsertTransactions(transactions: List<TransactionResponse>) {
        val writable = transactions.filter { it.id != null }
        if (writable.isEmpty()) return

        val dataKey = encryptionSession.requireDataKey()
        keyMaterialGate.requireKeyMaterial()

        writable.forEach { transaction ->
            val id = requireNotNull(transaction.id)
            // Sender and receiver are hydrated locally from the account table, so sealing them would
            // store a second encrypted copy of account data under every transaction.
            val record = transaction.copy(id = id, sender = null, receiver = null)
            val envelope = cipher.seal(record, TransactionResponse.serializer(), dataKey)
            store.writeDocument(FinanceCollection.TRANSACTIONS, id, envelope.toDocument())
        }
    }

    /**
     * Removal needs neither the data key nor the key material gate: nothing is read back and nothing
     * new is written, and the identifier being addressed was already plaintext.
     */
    override suspend fun deleteTransaction(id: String) {
        store.deleteDocument(FinanceCollection.TRANSACTIONS, id)
    }
}
