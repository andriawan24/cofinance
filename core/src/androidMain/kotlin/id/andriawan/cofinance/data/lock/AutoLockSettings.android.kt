package id.andriawan.cofinance.data.lock

import androidx.core.content.edit

/**
 * The auto-lock choice in ordinary app preferences.
 *
 * Deliberately not in the sealed storage the failed-attempt counter uses: this value is a
 * preference, not a secret, and an attacker who learns that a device locks after five minutes has
 * learned nothing about the data. Keeping it out of the Keystore-sealed file also means a lost or
 * cleared preference costs the user a setting rather than reading as tampering.
 */
actual fun createAutoLockSettings(): AutoLockSettings = KeyValueAutoLockSettings(
    readStoredId = { LockStorage.preferences().getString(TIMEOUT_KEY, null) },
    writeStoredId = { value ->
        LockStorage.preferences().edit { putString(TIMEOUT_KEY, value) }
    }
)

private const val TIMEOUT_KEY = "auto_lock_timeout"
