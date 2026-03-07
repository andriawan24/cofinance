package id.andriawan.cofinance.data.powersync

import com.powersync.DatabaseDriverFactory
import com.powersync.PowerSyncDatabase
import com.powersync.PersistentConnectionFactory

expect fun createDatabaseDriverFactory(): PersistentConnectionFactory

fun createPowerSyncDatabase(): PowerSyncDatabase {
    return PowerSyncDatabase(
        factory = createDatabaseDriverFactory(),
        schema = CofinanceSchema,
        dbFilename = "cofinance.db"
    )
}
