package id.andriawan24.cofinance.andro.utils

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.biometricSettingsDataStore by preferencesDataStore(name = "biometric_settings")

object BiometricPreferenceManager {

    private val BIOMETRIC_ENABLED_KEY: Preferences.Key<Boolean> =
        booleanPreferencesKey(name = "biometric_enabled")

    fun biometricEnabledFlow(context: Context): Flow<Boolean> {
        return context.biometricSettingsDataStore.data.map { preferences ->
            preferences[BIOMETRIC_ENABLED_KEY] ?: false
        }
    }

    suspend fun setBiometricEnabled(context: Context, enabled: Boolean) {
        context.biometricSettingsDataStore.edit { preferences ->
            preferences[BIOMETRIC_ENABLED_KEY] = enabled
        }
    }

    suspend fun isBiometricEnabled(context: Context): Boolean {
        val preferences = context.biometricSettingsDataStore.data.first()
        return preferences[BIOMETRIC_ENABLED_KEY] ?: false
    }
}
