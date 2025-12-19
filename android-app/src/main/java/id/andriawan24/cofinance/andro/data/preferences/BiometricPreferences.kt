package id.andriawan24.cofinance.andro.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val BIOMETRIC_PREFS = "biometric_preferences"

private val Context.biometricDataStore by preferencesDataStore(name = BIOMETRIC_PREFS)

class BiometricPreferences(context: Context) {

    private val dataStore = context.biometricDataStore

    val biometricEnabled: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[IS_ENABLED_KEY] ?: false }

    suspend fun setBiometricEnabled(isEnabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[IS_ENABLED_KEY] = isEnabled
        }
    }

    suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.remove(IS_ENABLED_KEY)
        }
    }

    private companion object {
        val IS_ENABLED_KEY: Preferences.Key<Boolean> = booleanPreferencesKey("biometric_enabled")
    }
}
