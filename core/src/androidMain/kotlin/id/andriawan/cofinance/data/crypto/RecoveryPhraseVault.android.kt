package id.andriawan.cofinance.data.crypto

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * The sealed recovery phrase in ordinary app-private preferences.
 *
 * It is deliberately not in the Keystore-sealed storage the failed-attempt counter uses. The blob is
 * already sealed under the data key, so a second layer bound to this device would add nothing an
 * attacker has to defeat while making the phrase — the thing that exists to survive this device —
 * depend on Keystore entries that a reinstallation destroys.
 */
actual fun createRecoveryPhraseVault(): RecoveryPhraseVault = SealedRecoveryPhraseVault(
    readSealed = { preferences().getString(PHRASE_KEY, null) },
    writeSealed = { value -> preferences().edit { putString(PHRASE_KEY, value) } },
    clearSealed = { preferences().edit { remove(PHRASE_KEY) } }
)

private fun preferences(): SharedPreferences {
    val context = DeviceKeyVaultStorage.applicationContext ?: throw DeviceKeyVaultException(
        "The recovery phrase vault is unavailable because the application context was never captured"
    )
    return context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
}

private const val PREFERENCES_NAME = "id.andriawan.cofinance.recovery-phrase"
private const val PHRASE_KEY = "sealed_recovery_phrase"
