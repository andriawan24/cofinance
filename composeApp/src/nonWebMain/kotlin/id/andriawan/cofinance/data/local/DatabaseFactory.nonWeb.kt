package id.andriawan.cofinance.data.local

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import id.andriawan.cofinance.data.powersync.PowerSyncCofinanceDatabase
import id.andriawan.cofinance.data.powersync.createPowerSyncDatabase

@Composable
actual fun rememberCofinanceDatabase(): CofinanceDatabase {
    return remember {
        PowerSyncCofinanceDatabase(createPowerSyncDatabase())
    }
}
