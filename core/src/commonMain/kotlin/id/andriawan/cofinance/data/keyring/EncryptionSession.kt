package id.andriawan.cofinance.data.keyring

import id.andriawan.cofinance.data.crypto.DataKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Whether this installation has completed encryption setup, and whether the data key can be used
 * right now.
 *
 * Everything that would send finance data out of the device asks this one question first, so that
 * "no plaintext leaves the device" is a property of a single object rather than of every call site
 * remembering to check. The two negative answers are deliberately distinct: [EncryptionSessionState
 * .SetupIncomplete] means there is no key at all and setup has to run, whereas
 * [EncryptionSessionState.Locked] means the key exists but is not in memory and an unlock will
 * produce it. Both refuse synchronization; only one of them is a reason to show setup.
 *
 * The unwrapped [DataKey] lives in process memory and nowhere else. Nothing here writes it, and
 * [lock] is the only way it leaves memory, which is the seam auto-lock builds on: a component
 * watching app lifecycle calls [lock] once the configured timeout elapses, and needs nothing from
 * this type beyond [state] and that call.
 */
interface EncryptionSession {

    /** The current state, observable so the lock screen and auto-lock can both react to it. */
    val state: StateFlow<EncryptionSessionState>

    /** True once key material exists for this installation, whether or not it is unlocked. */
    val isSetUp: Boolean get() = state.value != EncryptionSessionState.SetupIncomplete

    /** The data key while unlocked, and null while setup is incomplete or the session is locked. */
    fun dataKeyOrNull(): DataKey?

    /**
     * Returns the data key, for callers that cannot proceed without one.
     *
     * @throws DataKeyUnavailableException when setup has not completed or the session is locked.
     * Callers that should degrade rather than fail — a local mutation whose mirror has to wait for
     * the next unlock — check [dataKeyOrNull] instead.
     */
    fun requireDataKey(): DataKey = dataKeyOrNull() ?: throw DataKeyUnavailableException(state.value)

    /** Drops the data key from memory. Setup state is unaffected: a locked session can be unlocked. */
    fun lock()
}

/** The three states of [EncryptionSession], of which only one permits synchronization. */
enum class EncryptionSessionState {

    /** No key material for this installation. Encryption setup has to run before anything syncs. */
    SetupIncomplete,

    /** Key material exists but the data key is not in memory. An unlock produces it. */
    Locked,

    /** The data key is in memory and finance data may be encrypted, decrypted, and synchronized. */
    Unlocked
}

/** Raised when the data key is needed and the session cannot supply one. */
class DataKeyUnavailableException(val sessionState: EncryptionSessionState) : IllegalStateException(
    "The data key is unavailable because the encryption session is $sessionState"
)

/**
 * The [EncryptionSession] the app runs on: state in a flow, the data key in a field, nothing on disk.
 *
 * The mutating calls are deliberately absent from [EncryptionSession] so that the synchronization
 * paths, which only ever ask, cannot unlock. Setup, restore, and unlock hold this type; everything
 * else holds the interface.
 */
class InMemoryEncryptionSession : EncryptionSession {

    private var dataKey: DataKey? = null
    private val mutableState = MutableStateFlow(EncryptionSessionState.SetupIncomplete)

    override val state: StateFlow<EncryptionSessionState> = mutableState.asStateFlow()

    override fun dataKeyOrNull(): DataKey? = dataKey

    override fun lock() {
        dataKey = null
        if (mutableState.value == EncryptionSessionState.Unlocked) {
            mutableState.value = EncryptionSessionState.Locked
        }
    }

    /**
     * Records that key material exists for this installation without holding a key.
     *
     * This is the launch-time answer for a device that completed setup earlier: the wrapped copies
     * are on disk, so setup must not run again, but nothing is unlocked until the user unlocks.
     */
    fun markSetUp() {
        if (mutableState.value == EncryptionSessionState.SetupIncomplete) {
            mutableState.value = EncryptionSessionState.Locked
        }
    }

    /** Holds [dataKey] in memory, which also records that setup has completed. */
    fun unlock(dataKey: DataKey) {
        this.dataKey = dataKey
        mutableState.value = EncryptionSessionState.Unlocked
    }

    /**
     * Drops the key and forgets that setup ever completed.
     *
     * Sign-out and destruction of local key material after repeated failed PIN attempts both land
     * here: the next launch has to reach setup or restore rather than an unlock screen.
     */
    fun forgetSetup() {
        dataKey = null
        mutableState.value = EncryptionSessionState.SetupIncomplete
    }
}
