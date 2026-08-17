package id.andriawan.cofinance.data.lock

import android.content.Context
import android.content.SharedPreferences
import id.andriawan.cofinance.data.crypto.DeviceKeyVaultStorage
import java.io.File

/**
 * Where the Android lock keeps its two files, and how it reaches an application `Context`.
 *
 * The context comes from `DeviceKeyVaultStorage`, which a `ContentProvider` populates before
 * `Application.onCreate`. Reusing it rather than registering a second provider keeps one hook in
 * the manifest and guarantees the lock and the device key vault see the same application instance —
 * they are two halves of the same mechanism, and a device whose vault is unavailable has no PIN
 * wrap for the lock to protect anyway.
 */
internal object LockStorage {

    /** Everything the lock writes lives here, inside app-private storage. */
    fun directory(): File = File(context().filesDir, DIRECTORY_NAME).apply { mkdirs() }

    /** Non-secret lock preferences: the auto-lock timeout, and nothing else. */
    fun preferences(): SharedPreferences =
        context().getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private fun context(): Context = DeviceKeyVaultStorage.applicationContext
        ?: throw IllegalStateException(
            "App lock storage is unavailable because the application context was never captured"
        )

    private const val DIRECTORY_NAME = "app-lock"
    private const val PREFERENCES_NAME = "id.andriawan.cofinance.app-lock"
}
