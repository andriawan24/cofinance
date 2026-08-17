package id.andriawan.cofinance.pages.lock

import androidx.compose.runtime.Composable
import cofinance.composeapp.generated.resources.Res
import cofinance.composeapp.generated.resources.label_auto_lock_15_minutes
import cofinance.composeapp.generated.resources.label_auto_lock_1_minute
import cofinance.composeapp.generated.resources.label_auto_lock_5_minutes
import cofinance.composeapp.generated.resources.label_auto_lock_immediately
import cofinance.composeapp.generated.resources.label_duration_minutes
import cofinance.composeapp.generated.resources.label_duration_seconds
import id.andriawan.cofinance.data.lock.AutoLockTimeout
import kotlin.math.ceil
import kotlin.time.Duration
import org.jetbrains.compose.resources.stringResource

/**
 * A waiting time, in the coarsest unit that does not mislead.
 *
 * Delays run from 30 seconds to 5 minutes, so seconds below a minute and whole minutes above it
 * cover every value the schedule produces. Rounding *up* is deliberate: telling someone to wait one
 * minute when 90 seconds remain produces a second failure and a doubled delay.
 */
@Composable
fun lockDelayText(duration: Duration): String {
    val seconds = ceil(duration.inWholeMilliseconds / 1000.0).toInt().coerceAtLeast(1)
    return if (seconds < 60) {
        stringResource(Res.string.label_duration_seconds, seconds)
    } else {
        stringResource(Res.string.label_duration_minutes, ceil(seconds / 60.0).toInt())
    }
}

/** The label for an auto-lock option, kept next to the enum so a new option cannot go unnamed. */
@Composable
fun autoLockTimeoutLabel(timeout: AutoLockTimeout): String = when (timeout) {
    AutoLockTimeout.Immediately -> stringResource(Res.string.label_auto_lock_immediately)
    AutoLockTimeout.OneMinute -> stringResource(Res.string.label_auto_lock_1_minute)
    AutoLockTimeout.FiveMinutes -> stringResource(Res.string.label_auto_lock_5_minutes)
    AutoLockTimeout.FifteenMinutes -> stringResource(Res.string.label_auto_lock_15_minutes)
}
