package id.andriawan.cofinance.pages.encryption

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.andriawan.cofinance.data.crypto.DataKey
import id.andriawan.cofinance.data.crypto.DeviceKeyWrapper
import id.andriawan.cofinance.data.crypto.KeyMaterialDocument
import id.andriawan.cofinance.data.crypto.KeyWrapType
import id.andriawan.cofinance.data.crypto.RecoveryPhrase
import id.andriawan.cofinance.data.crypto.RecoveryPhraseKeyWrapper
import id.andriawan.cofinance.data.crypto.RecoveryPhraseVault
import id.andriawan.cofinance.data.crypto.createRecoveryPhraseVault
import id.andriawan.cofinance.data.keyring.InMemoryEncryptionSession
import id.andriawan.cofinance.data.lock.LocalKeyMaterialStore
import id.andriawan.cofinance.data.remote.KeyMaterialGate
import kotlin.coroutines.cancellation.CancellationException
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Where the user is in setup. Only [Completed] permits synchronization. */
enum class EncryptionSetupStep {

    /** Deciding whether this account needs a new phrase or already has one in the cloud. */
    Preparing,

    /** Key material already exists for this account, so the restore flow owns this device. */
    RestoreRequired,

    /** The twelve words are on screen to be copied onto paper. */
    PhraseDisplay,

    /** A few of the words have to be typed back before setup may finish. */
    Confirmation,

    /** Key material is published and the session holds the data key. */
    Completed
}

/**
 * One word the user is asked to type back, at its 1-based [position] in the phrase.
 *
 * Only a few positions are asked for, never the whole phrase. Re-typing all twelve would be
 * satisfied by copying from the screen above, which proves nothing about what is on paper.
 */
data class RequestedWord(
    val position: Int,
    val entry: String = "",
    val isWrong: Boolean = false
)

/** What went wrong during setup, resolved to user-facing text by the screen. */
sealed interface EncryptionSetupError {

    /** At least one typed word is not the word at that position. Setup has not completed. */
    data object RequestedWordsDoNotMatch : EncryptionSetupError

    /** Key material could not be published. Nothing was set up and nothing was synchronized. */
    data object SetupFailed : EncryptionSetupError
}

@Stable
data class EncryptionSetupUiState(
    val step: EncryptionSetupStep = EncryptionSetupStep.Preparing,
    val words: List<String> = emptyList(),
    val requestedWords: List<RequestedWord> = emptyList(),
    val isBusy: Boolean = false,
    val error: EncryptionSetupError? = null
) {
    /** Every requested word has something in it. Whether it is the right thing is [confirm]'s call. */
    val canConfirm: Boolean
        get() = requestedWords.isNotEmpty() && requestedWords.all { it.entry.isNotBlank() }
}

sealed interface EncryptionSetupUiEvent {
    data object PhraseWrittenDown : EncryptionSetupUiEvent
    data object BackToPhrase : EncryptionSetupUiEvent
    data class RequestedWordChanged(val position: Int, val value: String) : EncryptionSetupUiEvent
    data object Confirm : EncryptionSetupUiEvent
}

/**
 * Encryption setup, which every signed-in user passes through before anything synchronizes.
 *
 * Setup runs at sign-in rather than at first launch, so a user who never signs in never meets it:
 * the phrase protects the copy that leaves the device, and a local-only user has no such copy. That
 * gating lives in navigation; what lives here is the rule that setup is not finished until the
 * requested words come back correctly. Until then no key material is published and the session is
 * never unlocked, and since every synchronization path asks the session for a data key first, an
 * unconfirmed phrase means nothing can be uploaded.
 *
 * The data key and the phrase are held in fields rather than in [EncryptionSetupUiState], because
 * state is what the screen renders and neither of those belongs in a recomposition. The words
 * themselves do reach the state — they have to be read off the screen — but the entropy behind them
 * does not.
 *
 * The operations are `suspend` and the events launch them, so a test can drive setup without a main
 * dispatcher.
 */
@Stable
class EncryptionSetupViewModel(
    private val encryptionSession: InMemoryEncryptionSession,
    private val keyMaterialGate: KeyMaterialGate,
    private val deviceKeyWrapper: DeviceKeyWrapper,
    private val recoveryPhraseKeyWrapper: RecoveryPhraseKeyWrapper,
    private val localKeyMaterialStore: LocalKeyMaterialStore,
    // Defaulted so that the graph does not have to name it. The vault is stateless over platform
    // storage, so a second instance is the same vault, and a test supplies its own.
    private val recoveryPhraseVault: RecoveryPhraseVault = createRecoveryPhraseVault(),
    private val random: Random = Random.Default
) : ViewModel() {

    private val _uiState = MutableStateFlow(EncryptionSetupUiState())
    val uiState = _uiState.asStateFlow()

    private var dataKey: DataKey? = null
    private var recoveryPhrase: RecoveryPhrase? = null

    fun onEvent(event: EncryptionSetupUiEvent) {
        when (event) {
            is EncryptionSetupUiEvent.PhraseWrittenDown -> askForWords()
            is EncryptionSetupUiEvent.BackToPhrase -> returnToPhrase()
            is EncryptionSetupUiEvent.RequestedWordChanged ->
                onRequestedWordChanged(event.position, event.value)

            is EncryptionSetupUiEvent.Confirm -> viewModelScope.launch { confirm() }
        }
    }

    /** Starts setup from the screen. Safe to call again; a prepared screen ignores it. */
    fun start() {
        viewModelScope.launch { prepare() }
    }

    /**
     * Decides what this device needs and, when that is a new phrase, generates one.
     *
     * A device that finds recovery-phrase key material already in the cloud must not generate a
     * second phrase: doing so would publish key material that does not open the records already
     * stored under the first one, and the user's written phrase would silently stop working. That
     * account belongs to restore, so this reports [EncryptionSetupStep.RestoreRequired] instead.
     */
    suspend fun prepare() {
        if (_uiState.value.step != EncryptionSetupStep.Preparing || _uiState.value.isBusy) return
        _uiState.update { it.copy(isBusy = true, error = null) }

        val existing = try {
            keyMaterialGate.storedKeyMaterial()
        } catch (cause: Throwable) {
            if (cause is CancellationException) throw cause
            _uiState.update { it.copy(isBusy = false, error = EncryptionSetupError.SetupFailed) }
            return
        }

        if (existing != null && existing.wrapsOf(KeyWrapType.RecoveryPhrase).isNotEmpty()) {
            _uiState.update {
                it.copy(step = EncryptionSetupStep.RestoreRequired, isBusy = false)
            }
            return
        }

        val key = DataKey.generate()
        val phrase = RecoveryPhrase.generate()
        dataKey = key
        recoveryPhrase = phrase
        _uiState.update {
            it.copy(
                step = EncryptionSetupStep.PhraseDisplay,
                words = phrase.words,
                isBusy = false
            )
        }
    }

    /**
     * Checks the typed words and, only when every one of them is right, completes setup.
     *
     * The order of the three writes is the point. Key material reaches the backend first, because a
     * record encrypted before its recovery-phrase wrap exists is unrecoverable on any other device.
     * The device wrap is then kept locally, because it never leaves this device. The session is
     * unlocked last, since that is the moment synchronization becomes possible at all, and it must
     * not become possible before the material that makes the records readable again is stored.
     *
     * A failure anywhere in that sequence leaves the session untouched, so nothing synchronizes and
     * the user can try again.
     */
    suspend fun confirm() {
        val phrase = recoveryPhrase ?: return
        val key = dataKey ?: return
        val state = _uiState.value
        if (state.isBusy || state.step != EncryptionSetupStep.Confirmation) return

        val checked = state.requestedWords.map { requested ->
            requested.copy(isWrong = !matches(requested, phrase))
        }
        if (checked.any { it.isWrong }) {
            _uiState.update {
                it.copy(
                    requestedWords = checked,
                    error = EncryptionSetupError.RequestedWordsDoNotMatch
                )
            }
            return
        }

        _uiState.update { it.copy(requestedWords = checked, isBusy = true, error = null) }

        try {
            val material = KeyMaterialDocument(
                keyMaterialVersion = KeyMaterialDocument.CURRENT_VERSION,
                wrappedKeys = listOf(
                    deviceKeyWrapper.wrap(key),
                    recoveryPhraseKeyWrapper.wrap(key, phrase)
                )
            )
            // Uploads the recovery-phrase wrap only; the gate decides what may leave the device.
            keyMaterialGate.publishKeyMaterial(material)
            localKeyMaterialStore.write(material)
            // Kept so security settings can show the words again, per Decision 10. It is sealed
            // under the data key, so it is stored after the material that makes that key openable
            // and before the session is unlocked: an interruption here leaves a device that can
            // still be unlocked and restored, only without re-display.
            recoveryPhraseVault.store(phrase, key)

            encryptionSession.markSetUp()
            encryptionSession.unlock(key)
            _uiState.update { it.copy(step = EncryptionSetupStep.Completed, isBusy = false) }
        } catch (cause: Throwable) {
            if (cause is CancellationException) throw cause
            _uiState.update { it.copy(isBusy = false, error = EncryptionSetupError.SetupFailed) }
        }
    }

    private fun askForWords() {
        if (_uiState.value.step != EncryptionSetupStep.PhraseDisplay) return
        _uiState.update {
            it.copy(
                step = EncryptionSetupStep.Confirmation,
                requestedWords = requestedPositions().map(::RequestedWord),
                error = null
            )
        }
    }

    private fun returnToPhrase() {
        if (_uiState.value.step != EncryptionSetupStep.Confirmation) return
        _uiState.update {
            it.copy(
                step = EncryptionSetupStep.PhraseDisplay,
                requestedWords = emptyList(),
                error = null
            )
        }
    }

    private fun onRequestedWordChanged(position: Int, value: String) {
        _uiState.update { state ->
            state.copy(
                requestedWords = state.requestedWords.map { requested ->
                    if (requested.position == position) {
                        requested.copy(entry = value, isWrong = false)
                    } else {
                        requested
                    }
                },
                error = null
            )
        }
    }

    /**
     * Compares a typed word to the phrase, normalizing the way entry does.
     *
     * Trimming and lowercasing are safe for the same reason they are safe in
     * [RecoveryPhrase.parse]: the wordlist is lowercase ASCII, so normalization can only turn an
     * entry a human would call correct into one that matches.
     */
    private fun matches(requested: RequestedWord, phrase: RecoveryPhrase): Boolean =
        requested.entry.trim().lowercase() == phrase.words[requested.position - 1]

    private fun requestedPositions(): List<Int> =
        (1..RecoveryPhrase.WORD_COUNT).shuffled(random).take(REQUESTED_WORD_COUNT).sorted()

    companion object {
        /**
         * How many of the twelve words are asked for.
         *
         * Three positions out of twelve is 1 in 1320 to pass by guessing, which is enough to make
         * confirmation mean something, while staying short enough that a user who did write the
         * phrase down is not retyping it.
         */
        const val REQUESTED_WORD_COUNT: Int = 3
    }
}
