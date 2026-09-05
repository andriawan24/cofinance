package id.andriawan.cofinance.data.crypto

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import java.io.File

/**
 * Where the Android vault keeps the sealed agreement key on API levels below 31.
 *
 * `createDeviceKeyVault()` takes no arguments, because on iOS the Keychain needs no handle and the
 * common interface should not carry an Android concept. Android does need an application `Context`
 * to address app-private storage, so [DeviceKeyVaultInitializer] captures it at process start, the
 * same mechanism `androidx.startup` and Firebase use.
 *
 * On API 31 and above nothing is ever written here: the agreement key is Keystore-resident and the
 * device secret is derived rather than stored, so a missing context is not an error until a
 * pre-31 device actually needs the file.
 */
internal object DeviceKeyVaultStorage {

    @Volatile
    internal var applicationContext: Context? = null

    fun directory(): File {
        val context = applicationContext ?: throw DeviceKeyVaultException(
            "Device key storage is unavailable because the application context was never captured"
        )

        return File(context.filesDir, DIRECTORY_NAME)
    }

    private const val DIRECTORY_NAME = "device-key-vault"
}

/**
 * Captures the application context for [DeviceKeyVaultStorage].
 *
 * A `ContentProvider` is instantiated by the system before `Application.onCreate`, which is the
 * earliest hook a library can claim without asking the host application to call an initializer. It
 * is declared `android:exported="false"` and answers nothing: it exists only for [onCreate].
 */
class DeviceKeyVaultInitializer : ContentProvider() {

    override fun onCreate(): Boolean {
        DeviceKeyVaultStorage.applicationContext = context?.applicationContext
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0
}
