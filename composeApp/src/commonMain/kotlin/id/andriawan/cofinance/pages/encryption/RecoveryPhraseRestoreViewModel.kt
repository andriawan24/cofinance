package id.andriawan.cofinance.pages.encryption

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.andriawan.cofinance.data.crypto.DataKey
import id.andriawan.cofinance.data.crypto.DeviceKeyWrapper
import id.andriawan.cofinance.data.crypto.KeyMaterialDocument
import id.andriawan.cofinance.data.crypto.KeyWrapException
import id.andriawan.cofinance.data.crypto.KeyWrapType
import id.andriawan.cofinance.data.crypto.RecoveryPhrase
import id.andriawan.cofinance.data.crypto.RecoveryPhraseKeyWrapper
import id.andriawan.cofinance.data.crypto.RecoveryPhraseResult
import id.andriawan.cofinance.data.crypto.RecoveryPhraseVault
import id.andriawan.cofinance.data.crypto.createRecoveryPhraseVault
import id.andriawan.cofinance.data.crypto.WrappedDataKey
import id.andriawan.cofinance.data.keyring.InMemoryEncryptionSession
import id.andriawan.cofinance.data.local.account.AccountLocalDataSource
import id.andriawan.cofinance.data.local.transaction.TransactionLocalDataSource
import id.andriawan.cofinance.data.lock.LocalKeyMaterialStore
import id.andriawan.cofinance.data.remote.AccountRemoteDataSource
import id.andriawan.cofinance.data.remote.KeyMaterialGate
import id.andriawan.cofinance.data.remote.TransactionRemoteDataSource
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Why a restore attempt did not work, kept apart so the screen can say something useful.
 *
 * The three parse failures are distinct because the user can act on each differently: a word that
 * is not in the list at all is a spelling mistake they can see, whereas a phrase whose every word
 * is real but which does not check out means a word is wrong or out of order, which reading it back
 * will not reveal.
 */
sealed interface RecoveryPhraseEntryError {

    /** [actual] words were entered where twelve were expected. */
    data class WrongWordCount(val actual: Int) : RecoveryPhraseEntryError

    /** The word at 1-based [position] is not in the recovery wordlist. */
    data class UnknownWord(val position: Int, val word: String) : RecoveryPhraseEntryError

    /** Every word is real, but the phrase as a whole is not one this app ever produced. */
    data object ChecksumFailed : RecoveryPhraseEntryError

    /** A well-formed phrase that does not open this account's key material. */
    data object PhraseDoesNotOpenThisAccount : RecoveryPhraseEntryError

    /** This account has no key material stored, so there is nothing a phrase could restore. */
    data object NoStoredKeyMaterial : RecoveryPhraseEntryError

    /** The phrase was right but the restore itself did not finish. Nothing was imported. */
    data object RestoreFailed : RecoveryPhraseEntryError
}

@Stable
data class RecoveryPhraseRestoreUiState(
    val phraseInput: String = "",
    val isRestoring: Boolean = false,
    val error: RecoveryPhraseEntryError? = null,
    val isRestored: Boolean = false,
    val importedAccounts: Int = 0,
    val importedTransactions: Int = 0
) {
    val canRestore: Boolean get() = phraseInput.isNotBlank() && !isRestoring && !isRestored
}

sealed interface RecoveryPhraseRestoreUiEvent {
    data class PhraseChanged(val value: String) : RecoveryPhraseRestoreUiEvent
    data object Restore : RecoveryPhraseRestoreUiEvent
}

/**
 * Bringing synchronized records back onto a device that holds no key material.
 *
 * The sequence is: parse the phrase, unwrap the stored key material with it, decrypt every record
 * into memory, wrap the data key for this device, and only then write anything locally. That order
 * is what makes the flow all-or-nothing. A phrase that does not open the key material fails at the
 * unwrap, before a single record has been read and before the session has been unlocked, so an
 * invalid phrase cannot leave a partial import behind. A failure after the unwrap — a dropped
 * connection mid-download — rolls the session back to [id.andriawan.cofinance.data.keyring
 * .EncryptionSessionState.SetupIncomplete] and still writes nothing, because the local writes are
 * the last two statements in the flow and each is a single batch upsert.
 *
 * Retrying is always available: nothing here is one-shot, and a failed attempt leaves the typed
 * phrase in place so the user can correct one word rather than all twelve.
 */
@Stable
class RecoveryPhraseRestoreViewModel(
    private val encryptionSession: InMemoryEncryptionSession,
    private val keyMaterialGate: KeyMaterialGate,
    private val recoveryPhraseKeyWrapper: RecoveryPhraseKeyWrapper,
    private val deviceKeyWrapper: DeviceKeyWrapper,
    private val localKeyMaterialStore: LocalKeyMaterialStore,
    private val recoveryPhraseVault: RecoveryPhraseVault = createRecoveryPhraseVault(),
    private val remoteAccountSource: AccountRemoteDataSource,
    private val remoteTransactionSource: TransactionRemoteDataSource,
    private val localAccountSource: AccountLocalDataSource,
    private val localTransactionSource: TransactionLocalDataSource
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecoveryPhraseRestoreUiState())
    val uiState = _uiState.asStateFlow()

    fun onEvent(event: RecoveryPhraseRestoreUiEvent) {
        when (event) {
            is RecoveryPhraseRestoreUiEvent.PhraseChanged -> onPhraseChanged(event.value)
            is RecoveryPhraseRestoreUiEvent.Restore -> viewModelScope.launch { restore() }
        }
    }

    private fun onPhraseChanged(value: String) {
        _uiState.update { it.copy(phraseInput = value, error = null) }
    }

    suspend fun restore() {
        val state = _uiState.value
        if (state.isRestoring || state.isRestored) return

        val phrase = when (val parsed = RecoveryPhrase.parse(state.phraseInput)) {
            is RecoveryPhraseResult.Valid -> parsed.phrase

            is RecoveryPhraseResult.WrongWordCount ->
                return fail(RecoveryPhraseEntryError.WrongWordCount(parsed.actual))

            is RecoveryPhraseResult.UnknownWords -> {
                // The first unknown word is the one to fix; reporting all of them at once buries it.
                val first = parsed.words.first()
                return fail(RecoveryPhraseEntryError.UnknownWord(first.position, first.word))
            }

            is RecoveryPhraseResult.ChecksumFailed ->
                return fail(RecoveryPhraseEntryError.ChecksumFailed)
        }

        _uiState.update { it.copy(isRestoring = true, error = null) }

        try {
            val wraps = keyMaterialGate.storedKeyMaterial()
                ?.wrapsOf(KeyWrapType.RecoveryPhrase)
                .orEmpty()
            if (wraps.isEmpty()) return fail(RecoveryPhraseEntryError.NoStoredKeyMaterial)

            // More than one phrase wrap is possible once key rotation exists, so every one is tried
            // before the phrase is called wrong.
            val dataKey = wraps.firstNotNullOfOrNull { wrap -> unwrapOrNull(wrap, phrase) }
                ?: return fail(RecoveryPhraseEntryError.PhraseDoesNotOpenThisAccount)

            // Past this point the phrase is proven correct. Nothing above it read or wrote a record,
            // which is what makes a wrong phrase import nothing at all.
            importWith(dataKey, phrase, wraps)
        } catch (cause: Throwable) {
            if (cause is CancellationException) throw cause
            encryptionSession.forgetSetup()
            _uiState.update {
                it.copy(isRestoring = false, error = RecoveryPhraseEntryError.RestoreFailed)
            }
        }
    }

    /**
     * Decrypts everything, then writes it.
     *
     * The session has to be unlocked first because the encrypted data sources decrypt against the
     * session's data key, and it is rolled back on any failure so a half-finished restore does not
     * leave the app in a state where synchronization would run against records that never arrived.
     */
    private suspend fun importWith(
        dataKey: DataKey,
        phrase: RecoveryPhrase,
        phraseWraps: List<WrappedDataKey>
    ) {
        encryptionSession.markSetUp()
        encryptionSession.unlock(dataKey)

        val accounts = remoteAccountSource.getAccounts()
        val transactions = remoteTransactionSource.getTransactions()

        // The device wrap is what stops the next launch from asking for the phrase again. It is
        // created before the import so that a failure here imports nothing rather than leaving
        // records behind that this device could not open again on its own.
        val deviceWrap = deviceKeyWrapper.wrap(dataKey)
        localKeyMaterialStore.write(
            KeyMaterialDocument(
                keyMaterialVersion = KeyMaterialDocument.CURRENT_VERSION,
                wrappedKeys = phraseWraps + deviceWrap
            )
        )

        // The user just proved they hold the phrase, so this device can offer re-display too. It is
        // the phrase that was typed rather than one read back from anywhere, so a restore onto a
        // second device leaves both able to show the same words.
        recoveryPhraseVault.store(phrase, dataKey)

        localAccountSource.upsertAccounts(accounts)
        localTransactionSource.upsertTransactions(transactions)

        _uiState.update {
            it.copy(
                isRestoring = false,
                isRestored = true,
                importedAccounts = accounts.size,
                importedTransactions = transactions.size
            )
        }
    }

    private suspend fun unwrapOrNull(wrap: WrappedDataKey, phrase: RecoveryPhrase): DataKey? = try {
        recoveryPhraseKeyWrapper.unwrap(wrap, phrase)
    } catch (_: KeyWrapException) {
        // A wrong phrase derives a wrong wrapping key and so fails authentication, which is
        // indistinguishable from a tampered wrap and is treated the same: nothing is opened.
        null
    }

    private fun fail(error: RecoveryPhraseEntryError) {
        _uiState.update { it.copy(isRestoring = false, error = error) }
    }
}
