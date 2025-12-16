package id.andriawan24.cofinance.andro.data.security

import android.content.Context
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecureTokenStorage(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val preferences = EncryptedSharedPreferences.create(
        context,
        PREF_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveToken(token: String) {
        preferences.edit {
            putString(KEY_ID_TOKEN, token)
        }
    }

    fun getToken(): String? = preferences.getString(KEY_ID_TOKEN, null)

    fun clear() {
        preferences.edit {
            remove(KEY_ID_TOKEN)
        }
    }

    companion object {
        private const val PREF_NAME = "secure_auth_storage"
        private const val KEY_ID_TOKEN = "id_token"
    }
}
