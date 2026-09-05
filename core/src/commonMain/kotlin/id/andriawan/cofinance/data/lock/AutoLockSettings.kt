package id.andriawan.cofinance.data.lock

import id.andriawan.cofinance.data.lock.AutoLockTimeout.Companion.Default
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * How long the app may stay in the background before the data key is dropped.
 *
 * The four options and the default are fixed by Decision 11: one minute is the default because
 * leaving to read an amount out of a banking or messaging app and coming straight back is the app's
 * core entry flow, and locking on every such switch would make the lock the dominant interaction.
 * `Immediately` stays available for users who want exactly that.
 *
 * [storedId] is written to storage instead of the enum name or its ordinal, so that renaming a
 * constant or reordering the list cannot silently change a user's setting.
 */
enum class AutoLockTimeout(val storedId: String, val duration: Duration) {
    Immediately("immediately", Duration.ZERO),
    OneMinute("1m", 1.minutes),
    FiveMinutes("5m", 5.minutes),
    FifteenMinutes("15m", 15.minutes);

    companion object {

        /** The value in effect before the user chooses one, per Decision 11. */
        val Default: AutoLockTimeout = OneMinute

        /** The options the settings screen offers, in the order it offers them. */
        val options: List<AutoLockTimeout> = listOf(
            Immediately,
            OneMinute,
            FiveMinutes,
            FifteenMinutes
        )

        /** Resolves a stored identifier, falling back to [Default] for anything unrecognized. */
        fun ofStoredId(id: String?): AutoLockTimeout =
            options.firstOrNull { it.storedId == id } ?: Default
    }
}

/**
 * The user's auto-lock choice, observable so the settings screen and the controller share one value.
 *
 * This holds a preference, not a secret: knowing that a device locks after five minutes reveals
 * nothing about the data, so it lives in ordinary platform key-value storage rather than in the
 * sealed storage the failed-attempt counter needs.
 */
interface AutoLockSettings {
    val timeout: StateFlow<AutoLockTimeout>
    fun setTimeout(value: AutoLockTimeout)
}

/**
 * [AutoLockSettings] over any string key-value store, which is all either platform needs.
 *
 * The platform-specific part of this setting is two lines — read a string, write a string — so the
 * implementation is written once here and the `actual` factories supply the two lambdas. That also
 * lets `commonTest` drive the real implementation over a map instead of testing a fake of it.
 */
class KeyValueAutoLockSettings(
    readStoredId: () -> String?,
    private val writeStoredId: (String) -> Unit
) : AutoLockSettings {

    private val mutableTimeout = MutableStateFlow(AutoLockTimeout.ofStoredId(readStoredId()))

    override val timeout: StateFlow<AutoLockTimeout> = mutableTimeout.asStateFlow()

    override fun setTimeout(value: AutoLockTimeout) {
        writeStoredId(value.storedId)
        mutableTimeout.value = value
    }
}

/** Returns the auto-lock setting backed by this platform's key-value storage. */
expect fun createAutoLockSettings(): AutoLockSettings
