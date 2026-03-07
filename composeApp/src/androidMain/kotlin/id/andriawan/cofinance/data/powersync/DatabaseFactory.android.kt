package id.andriawan.cofinance.data.powersync

import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import com.powersync.DatabaseDriverFactory
import com.powersync.PersistentConnectionFactory

internal lateinit var appContext: android.content.Context
    private set

/**
 * Auto-initializer that captures the Application context.
 * Registered in AndroidManifest via androidResources.
 */
class PowerSyncInitProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        appContext = context?.applicationContext ?: return false
        return true
    }
    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}

actual fun createDatabaseDriverFactory(): PersistentConnectionFactory {
    return DatabaseDriverFactory(appContext)
}
