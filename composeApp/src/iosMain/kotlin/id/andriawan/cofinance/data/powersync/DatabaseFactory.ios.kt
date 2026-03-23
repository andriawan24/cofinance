package id.andriawan.cofinance.data.powersync

import com.powersync.DatabaseDriverFactory
import com.powersync.PersistentConnectionFactory

actual fun createDatabaseDriverFactory(): PersistentConnectionFactory {
    return DatabaseDriverFactory()
}
