package id.andriawan.cofinance.pages.splash

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.andriawan.cofinance.data.crypto.DeviceKeyWrapper
import id.andriawan.cofinance.data.crypto.KeyWrapType
import id.andriawan.cofinance.data.keyring.EncryptionSessionState
import id.andriawan.cofinance.data.keyring.InMemoryEncryptionSession
import id.andriawan.cofinance.data.lock.AppLock
import id.andriawan.cofinance.data.lock.LocalKeyMaterialStore
import id.andriawan.cofinance.data.migration.MigrationState
import id.andriawan.cofinance.data.migration.PlaintextMigration
import id.andriawan.cofinance.data.session.SessionPolicy
import id.andriawan.cofinance.pages.lock.LaunchLockDecision
import id.andriawan.cofinance.pages.lock.launchLockDecision
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Where the launch sequence ends up, once it stops being a launch sequence. */
sealed interface LaunchRoute {

    /** The main experience. Reached only when nothing is left to unlock, set up, or convert. */
    data object Main : LaunchRoute

    /**
     * Mandatory encryption setup, which decides for itself whether this device generates a phrase
     * or has to restore with one.
     */
    data object EncryptionSetup : LaunchRoute

    /**
     * The unlock screen. Reached only for a session holding key material it cannot open, which is
     * exactly what [id.andriawan.cofinance.pages.lock.launchLockDecision] answers `Unlock` for.
     */
    data object Unlock : LaunchRoute
}

/** What the launch screen is doing while it holds the user. */
sealed interface LaunchPhase {

    /** Reading key material, signing state, and the account. Nothing is asked of the user. */
    data object Preparing : LaunchPhase

    /** Converting this account's plaintext cloud records, which per Decision 6 blocks the launch. */
    data class Migrating(val finished: Int, val total: Int) : LaunchPhase

    /** Migration stopped part-way. Records already converted stay converted; a retry resumes. */
    data object MigrationFailed : LaunchPhase
}

@Stable
data class SplashUiState(
    val phase: LaunchPhase = LaunchPhase.Preparing,
    /** Non-null once the launch sequence has decided; the screen navigates and stops rendering. */
    val route: LaunchRoute? = null
)

/**
 * Mirroring the account's cloud data and loading the account itself, as the launch sequence needs
 * them.
 *
 * A seam rather than the two collaborators directly, because both reach Firebase through concrete
 * classes that a host test cannot stand in for, and because the launch sequence has no interest in
 * either of them beyond "do the signed-in work". What it does care about — that this runs *after*
 * migration, never before — is visible in [SplashViewModel.launch] and would be buried if the calls
 * were inlined there.
 */
fun interface LaunchSynchronizer {

    /** Mirrors cloud data and loads the account. Failures are the caller's to decide about. */
    suspend fun synchronize()
}

sealed interface SplashUiEvent {
    data object RetryMigration : SplashUiEvent
}

/**
 * The launch sequence: who is signed in, whether this device can open its data, and whether the
 * account still has plaintext in the cloud.
 *
 * Four rules are decided here rather than by any screen, because each of them is a security or
 * data-integrity property that a navigation callback could otherwise get wrong:
 *
 * - **A local-only user is untouched.** Someone who has never signed in has no copy of their data
 *   anywhere but this device, so there is nothing a recovery phrase protects and nothing to unlock.
 *   They go straight to the main experience, and neither setup nor an unlock prompt is reachable
 *   from here for them. This is the first branch in [launch] for exactly that reason.
 * - **Stored key material is adopted before anything is decided.** The device wrap survives the
 *   process now, so a relaunching device that already completed setup must be recognised as set up
 *   rather than sent through setup again — which would publish a second phrase and strand the
 *   records written under the first.
 * - **A PIN, if one is set, is the gate.** The device wrap could open the session without asking
 *   anything, so [AppLock.isPinSet] is consulted *before* it is used. Nothing here ever sees a PIN:
 *   a device that has one is routed to the unlock screen, which derives the key from it.
 * - **Migration runs before the main experience, and only when there is something to convert.**
 *   It is invoked after the session is unlocked because sealing a record needs the data key, and
 *   its own scan is what decides whether a user is blocked at all — an account with no plaintext
 *   records completes it without ever reaching the conversion phase.
 */
@Stable
class SplashViewModel(
    private val sessionPolicy: SessionPolicy,
    private val synchronizer: LaunchSynchronizer,
    private val encryptionSession: InMemoryEncryptionSession,
    private val localKeyMaterialStore: LocalKeyMaterialStore,
    private val deviceKeyWrapper: DeviceKeyWrapper,
    private val appLock: AppLock,
    private val migration: PlaintextMigration
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState = _uiState.asStateFlow()

    /** Starts the sequence from the screen. */
    fun start() {
        viewModelScope.launch { launch() }
    }

    fun onEvent(event: SplashUiEvent) {
        when (event) {
            SplashUiEvent.RetryMigration -> viewModelScope.launch { launch() }
        }
    }

    /** Runs the whole sequence, and is safe to run again from any point it stopped at. */
    suspend fun launch() {
        _uiState.update { it.copy(phase = LaunchPhase.Preparing, route = null) }

        if (!sessionPolicy.isSignedIn()) {
            // A local-only user. No encrypted copy exists to protect, so setup and unlock are both
            // skipped rather than deferred, and migration would have nothing in the cloud to find.
            return settle(LaunchRoute.Main)
        }

        adoptStoredKeyMaterial()

        val state = encryptionSession.state.value
        if (state == EncryptionSessionState.SetupIncomplete) {
            return settle(LaunchRoute.EncryptionSetup)
        }
        // The lock's own rule, rather than a second copy of it here. It answers `Unlock` exactly
        // when key material exists and the key is not in memory.
        if (launchLockDecision(state) == LaunchLockDecision.Unlock) {
            return settle(LaunchRoute.Unlock)
        }

        if (!runMigration()) return

        synchronizeAndFetchUser()
        settle(LaunchRoute.Main)
    }

    /**
     * Brings the process-local session up to date with what this device already stores.
     *
     * The session starts every process at `SetupIncomplete` because the data key lives in memory
     * only. The durable key material is the record of what actually happened on this device, so it
     * is read first and the session is told setup completed. Whether that becomes an unlocked
     * session or a locked one is the next decision, and it turns on the PIN.
     */
    private suspend fun adoptStoredKeyMaterial() {
        if (encryptionSession.state.value != EncryptionSessionState.SetupIncomplete) return
        val document = localKeyMaterialStore.read() ?: return

        encryptionSession.markSetUp()

        // A PIN wrap means the user asked to be asked. The device wrap below would open the session
        // without a prompt, so this check has to come before it rather than after.
        if (appLock.isPinSet()) return

        val deviceWrap = document.wrapsOf(KeyWrapType.Device).firstOrNull()
        val dataKey = deviceWrap?.let {
            try {
                deviceKeyWrapper.unwrap(it)
            } catch (cause: Throwable) {
                if (cause is CancellationException) throw cause
                null
            }
        }

        if (dataKey != null) {
            encryptionSession.unlock(dataKey)
            return
        }

        // A device wrap that no longer opens — the platform key material behind it is gone — and no
        // PIN to fall back on leaves nothing here that could ever unlock. Forgetting setup routes
        // the launch to the setup screen, which finds the account's recovery-phrase key material
        // already published and hands over to restore. Left as `Locked` this would be a dead end.
        encryptionSession.forgetSetup()
    }

    /** Returns true when the launch may proceed; false leaves a retryable failure on screen. */
    private suspend fun runMigration(): Boolean = coroutineScope {
        _uiState.update { it.copy(phase = LaunchPhase.Migrating(finished = 0, total = 0)) }

        // The progress job is a child of the caller rather than of `viewModelScope`, so the whole
        // sequence is drivable from a test without a main dispatcher, and so a cancelled launch
        // cannot leave a collector behind.
        val progress = launch {
            migration.state.collect { state ->
                if (state is MigrationState.Converting) {
                    _uiState.update {
                        it.copy(phase = LaunchPhase.Migrating(state.finished, state.total))
                    }
                }
            }
        }

        val outcome = try {
            migration.run()
        } finally {
            progress.cancel()
        }

        if (outcome is MigrationState.Failed) {
            _uiState.update { it.copy(phase = LaunchPhase.MigrationFailed) }
            return@coroutineScope false
        }
        true
    }

    /**
     * The pre-existing launch work: mirror what is in the cloud, then load the account.
     *
     * Both failures are swallowed into a successful launch, as they were before this change: the
     * app is offline-first and a user with no connection still reaches their local data. What is
     * *not* swallowed is anything above this line — an unmigrated account or a locked session stops
     * the launch instead.
     */
    private suspend fun synchronizeAndFetchUser() {
        try {
            synchronizer.synchronize()
        } catch (cause: Throwable) {
            if (cause is CancellationException) throw cause
        }
    }

    private fun settle(route: LaunchRoute) {
        _uiState.update { it.copy(route = route) }
    }
}
