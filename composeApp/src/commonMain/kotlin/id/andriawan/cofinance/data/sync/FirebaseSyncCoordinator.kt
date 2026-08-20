package id.andriawan.cofinance.data.sync

import id.andriawan.cofinance.data.keyring.EncryptionSession
import id.andriawan.cofinance.data.local.account.AccountLocalDataSource
import id.andriawan.cofinance.data.local.transaction.TransactionLocalDataSource
import id.andriawan.cofinance.data.remote.AccountRemoteDataSource
import id.andriawan.cofinance.data.remote.TransactionRemoteDataSource
import id.andriawan.cofinance.data.session.SessionPolicy

/**
 * Moves finance records between the local source of truth and the authenticated user's cloud
 * collections.
 *
 * Both directions need two things: an authenticated session, and an available data key. The session
 * says whose data this is; the key says whether the device can produce ciphertext at all. Without
 * the key there is nothing this coordinator may do, because the alternative — uploading readable
 * records — is the exact outcome the encryption work exists to prevent. It therefore returns rather
 * than throws: a mutation that has already committed locally stays committed, and the next
 * synchronization after an unlock re-uploads the whole snapshot, so nothing is lost by skipping one.
 */
class FirebaseSyncCoordinator(
    private val localAccountSource: AccountLocalDataSource,
    private val localTransactionSource: TransactionLocalDataSource,
    private val remoteAccountSource: AccountRemoteDataSource,
    private val remoteTransactionSource: TransactionRemoteDataSource,
    private val sessionPolicy: SessionPolicy,
    private val encryptionSession: EncryptionSession
) {
    suspend fun syncDataAfterSignIn() {
        sessionPolicy.requireUserId()
        // Setup incomplete or locked: nothing is read, because reading means decrypting, and nothing
        // is written, because writing would mean writing plaintext.
        if (encryptionSession.dataKeyOrNull() == null) return

        val localAccountRecords = localAccountSource.getAccounts()
        val localTransactionRecords = localTransactionSource.getAllTransactions()
        val localAccountIds = localAccountRecords.mapTo(mutableSetOf()) { it.id }
        val localTransactionIds = localTransactionRecords.mapTo(mutableSetOf()) { it.id }

        val localAccounts = remoteAccountSource.getAccounts().filterNot { it.id in localAccountIds }
        localAccountSource.upsertAccounts(localAccounts)

        val localTransactions = remoteTransactionSource.getTransactions().filterNot {
            it.id in localTransactionIds
        }
        localTransactionSource.upsertTransactions(localTransactions)

        mirrorDataIfSignedIn()
    }

    suspend fun mirrorDataIfSignedIn() {
        if (!sessionPolicy.isSignedIn()) return
        if (encryptionSession.dataKeyOrNull() == null) return

        remoteAccountSource.upsertAccounts(localAccountSource.getAccounts())
        remoteTransactionSource.upsertTransactions(localTransactionSource.getAllTransactions())
    }

    suspend fun clearLocalData() {
        localAccountSource.clearAccounts()
        localTransactionSource.clearTransactions()
    }
}
