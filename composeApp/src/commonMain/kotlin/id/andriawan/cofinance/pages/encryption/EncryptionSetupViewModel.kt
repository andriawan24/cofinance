package id.andriawan.cofinance.pages.encryption

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.andriawan.cofinance.data.crypto.DataKey
import id.andriawan.cofinance.data.crypto.DeviceKeyWrapper
import id.andriawan.cofinance.data.crypto.KeyMaterialDocument
import id.andriawan.cofinance.data.crypto.KeyWrapType
import id.andriawan.cofinance.data.crypto.PhraseExportStatus
import id.andriawan.cofinance.data.crypto.RECOVERY_PHRASE_FILE_NAME
import id.andriawan.cofinance.data.crypto.RecoveryPhrase
import id.andriawan.cofinance.data.crypto.RecoveryPhraseExporter
import id.andriawan.cofinance.data.crypto.RecoveryPhraseKeyWrapper
import id.andriawan.cofinance.data.crypto.RecoveryPhraseVault
import id.andriawan.cofinance.data.crypto.createRecoveryPhraseVault
import id.andriawan.cofinance.data.crypto.toExportText
import id.andriawan.cofinance.data.keyring.InMemoryEncryptionSession
import id.andriawan.cofinance.data.lock.LocalKeyMaterialStore
import id.andriawan.cofinance.data.remote.KeyMaterialGate
import kotlin.coroutines.cancellation.CancellationException
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

    /** The six groups are on screen, to be copied, saved to a file, or written down. */
    PhraseDisplay,

    /** Key material is published and the session holds the data key. */
    Completed
}

/** What went wrong during setup, resolved to user-facing text by the screen. */
sealed interface EncryptionSetupError {

    /** Key material could not be published. Nothing was set up and nothing was synchronized. */
    data object SetupFailed : EncryptionSetupError
}

@Stable
data class EncryptionSetupUiState(
    val step: EncryptionSetupStep = EncryptionSetupStep.Preparing,
    val groups: List<String> = emptyList(),
    val isBusy: Boolean = false,
    val error: EncryptionSetupError? = null,
    val exportStatus: PhraseExportStatus? = null
)

sealed interface EncryptionSetupUiEvent {
    data object CopyPhrase : EncryptionSetupUiEvent
    data object DownloadPhrase : EncryptionSetupUiEvent
    data object PhraseSaved : EncryptionSetupUiEvent
}

/**
 * Encryption setup, which every signed-in user passes through before anything synchronizes.
 *
 * Setup runs at sign-in rather than at first launch, so a user who never signs in never meets it:
 * the phrase protects the copy that leaves the device, and a local-only user has no such copy. That
 * gating lives in navigation; what lives here is the sequence that publishes key material and then
 * unlocks the session, and the rule that neither happens until the user has moved past the phrase.
 *
 * Setup no longer asks for part of the phrase back. Re-typing three groups proved little — the
 * phrase was on the screen above, so the test was satisfied by copying from it — while costing every
 * user a typing exercise. Keeping the phrase is offered instead of examined: it can go on the clipboard or into a
 * file in one tap, and the user says when they are done with it.
 *
 * The data key and the phrase are held in fields rather than in [EncryptionSetupUiState], because
 * state is what the screen renders and neither of those belongs in a recomposition. The groups
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
    private val recoveryPhraseExporter: RecoveryPhraseExporter,
    // Defaulted so that the graph does not have to name it. The vault is stateless over platform
    // storage, so a second instance is the same vault, and a test supplies its own.
    private val recoveryPhraseVault: RecoveryPhraseVault = createRecoveryPhraseVault()
) : ViewModel() {

    private val _uiState = MutableStateFlow(EncryptionSetupUiState())
    val uiState = _uiState.asStateFlow()

    private var dataKey: DataKey? = null
    private var recoveryPhrase: RecoveryPhrase? = null

    fun onEvent(event: EncryptionSetupUiEvent) {
        when (event) {
            is EncryptionSetupUiEvent.CopyPhrase -> viewModelScope.launch { copyPhrase() }
            is EncryptionSetupUiEvent.DownloadPhrase -> viewModelScope.launch { downloadPhrase() }
            is EncryptionSetupUiEvent.PhraseSaved -> viewModelScope.launch { finishSetup() }
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
                groups = phrase.groups,
                isBusy = false
            )
        }
    }

    /**
     * Publishes key material, keeps the phrase, and unlocks the session.
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
    suspend fun finishSetup() {
        val phrase = recoveryPhrase ?: return
        val key = dataKey ?: return
        val state = _uiState.value
        if (state.isBusy || state.step != EncryptionSetupStep.PhraseDisplay) return

        _uiState.update { it.copy(isBusy = true, error = null) }

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
            // Kept so security settings can show the phrase again, per Decision 10. It is sealed
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

    /** Puts the phrase on the clipboard. Reports what happened; never blocks setup. */
    suspend fun copyPhrase() {
        val phrase = recoveryPhrase ?: return
        val copied = try {
            recoveryPhraseExporter.copyToClipboard(phrase.toExportText())
        } catch (cause: Throwable) {
            if (cause is CancellationException) throw cause
            false
        }
        _uiState.update {
            it.copy(
                exportStatus =
                    if (copied) PhraseExportStatus.Copied else PhraseExportStatus.CopyFailed
            )
        }
    }

    /**
     * Writes the phrase to a file the user chooses or can find.
     *
     * A user who backs out of the platform's save flow is indistinguishable here from a write that
     * failed, and both mean the same thing to them: there is no file. So both report
     * [PhraseExportStatus.SaveFailed] rather than the screen claiming a file that does not exist.
     */
    suspend fun downloadPhrase() {
        val phrase = recoveryPhrase ?: return
        val location = try {
            recoveryPhraseExporter.saveToFile(RECOVERY_PHRASE_FILE_NAME, phrase.toExportText())
        } catch (cause: Throwable) {
            if (cause is CancellationException) throw cause
            null
        }
        _uiState.update {
            it.copy(
                exportStatus = location?.let(PhraseExportStatus::Saved)
                    ?: PhraseExportStatus.SaveFailed
            )
        }
    }
}
