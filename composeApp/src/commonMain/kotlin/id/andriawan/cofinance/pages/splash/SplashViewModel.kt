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
import id.andriawan.cofinance.data.session.SessionPolicy
import id.andriawan.cofinance.pages.lock.LaunchLockDecision
import id.andriawan.cofinance.pages.lock.launchLockDecision
import kotlin.coroutines.cancellation.CancellationException
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
 * either of them beyond "do the signed-in work". Keeping the call in [SplashViewModel.launch]
 * rather than inlining it there keeps the order of the launch sequence readable in one place.
 */
fun interface LaunchSynchronizer {

    /** Mirrors cloud data and loads the account. Failures are the caller's to decide about. */
    suspend fun synchronize()
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
 */
@Stable
class SplashViewModel(
    private val sessionPolicy: SessionPolicy,
    private val synchronizer: LaunchSynchronizer,
    private val encryptionSession: InMemoryEncryptionSession,
    private val localKeyMaterialStore: LocalKeyMaterialStore,
    private val deviceKeyWrapper: DeviceKeyWrapper,
    private val appLock: AppLock
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState = _uiState.asStateFlow()

    /** Starts the sequence from the screen. */
    fun start() {
        viewModelScope.launch { launch() }
    }

    /** Runs the whole sequence, and is safe to run again from any point it stopped at. */
    suspend fun launch() {
        _uiState.update { it.copy(phase = LaunchPhase.Preparing, route = null) }

        if (!sessionPolicy.isSignedIn()) {
            return settle(LaunchRoute.Main)
        }

        adoptStoredKeyMaterial()

        val state = encryptionSession.state.value
        if (state == EncryptionSessionState.SetupIncomplete) {
            return settle(LaunchRoute.EncryptionSetup)
        }

        if (launchLockDecision(state) == LaunchLockDecision.Unlock) {
            return settle(LaunchRoute.Unlock)
        }

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

        encryptionSession.forgetSetup()
    }

    /**
     * The pre-existing launch work: mirror what is in the cloud, then load the account.
     *
     * Both failures are swallowed into a successful launch, as they were before this change: the
     * app is offline-first and a user with no connection still reaches their local data. What is
     * *not* swallowed is anything above this line — a locked session stops the launch instead.
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
