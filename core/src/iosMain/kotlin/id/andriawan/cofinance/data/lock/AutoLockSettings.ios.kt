package id.andriawan.cofinance.data.lock

import platform.Foundation.NSUserDefaults

/**
 * The auto-lock choice in `NSUserDefaults`.
 *
 * A preference rather than a secret, so it stays out of the Keychain the failed-attempt counter
 * uses: nothing about the data is revealed by the timeout, and keeping it here means a user who
 * reinstalls gets the default back rather than an inherited setting they cannot see.
 */
actual fun createAutoLockSettings(): AutoLockSettings = KeyValueAutoLockSettings(
    readStoredId = { NSUserDefaults.standardUserDefaults.stringForKey(TIMEOUT_KEY) },
    writeStoredId = { value ->
        NSUserDefaults.standardUserDefaults.setObject(value, forKey = TIMEOUT_KEY)
    }
)

private const val TIMEOUT_KEY = "id.andriawan.cofinance.lock.auto_lock_timeout"
