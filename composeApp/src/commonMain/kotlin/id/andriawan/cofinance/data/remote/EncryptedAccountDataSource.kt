package id.andriawan.cofinance.data.remote

import id.andriawan.cofinance.data.crypto.EncryptedRecordException
import id.andriawan.cofinance.data.crypto.RecordCipher
import id.andriawan.cofinance.data.crypto.toDocument
import id.andriawan.cofinance.data.crypto.toEnvelope
import id.andriawan.cofinance.data.keyring.EncryptionSession
import id.andriawan.cofinance.data.model.AccountResponse

/**
 * The cloud account source: sealed before the write, opened after the read.
 *
 * The stored document holds the plaintext document identifier and an encrypted envelope, and nothing
 * else. Every finance field — name, group, balance, account type, and the creation timestamp — is
 * inside the ciphertext, so a full backend export shows the number of accounts and nothing about
 * them.
 *
 * Each upload seals afresh rather than reusing an envelope, because the mirror re-uploads the whole
 * local snapshot on every synchronization and a reused envelope would mean a reused nonce.
 * [RecordCipher] draws a new one per seal, so nothing here caches.
 */
class EncryptedAccountDataSource(
    private val store: FinanceDocumentStore,
    private val keyMaterialGate: KeyMaterialGate,
    private val encryptionSession: EncryptionSession,
    private val cipher: RecordCipher
) : AccountRemoteDataSource {

    override suspend fun getAccounts(): List<AccountResponse> {
        val dataKey = encryptionSession.requireDataKey()
        return store.readDocuments(FinanceCollection.ACCOUNTS).mapNotNull { stored ->
            try {
                cipher.open(stored.document.toEnvelope(), AccountResponse.serializer(), dataKey)
                    // The document identifier is what addresses the record, so it wins over the
                    // sealed copy of itself.
                    .copy(id = stored.id)
            } catch (_: EncryptedRecordException) {
                // A record that fails authentication, was sealed under another key, or is still
                // plaintext from an earlier build is skipped rather than imported, and rather than
                // failing the whole import. Migration is what converts the plaintext ones.
                null
            }
        }
    }

    override suspend fun upsertAccounts(accounts: List<AccountResponse>) {
        val writable = accounts.filter { it.id != null }
        if (writable.isEmpty()) return

        val dataKey = encryptionSession.requireDataKey()
        keyMaterialGate.requireKeyMaterial()

        writable.forEach { account ->
            val id = requireNotNull(account.id)
            val envelope = cipher.seal(account.copy(id = id), AccountResponse.serializer(), dataKey)
            store.writeDocument(FinanceCollection.ACCOUNTS, id, envelope.toDocument())
        }
    }

    /**
     * Removal needs neither the data key nor the key material gate: nothing is read back and nothing
     * new is written, and the identifier being addressed was already plaintext.
     */
    override suspend fun deleteAccount(id: String) {
        store.deleteDocument(FinanceCollection.ACCOUNTS, id)
    }
}
