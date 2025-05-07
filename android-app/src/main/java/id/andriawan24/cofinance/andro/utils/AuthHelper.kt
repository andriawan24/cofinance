package id.andriawan24.cofinance.andro.utils

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import id.andriawan24.cofinance.andro.MainActivity

object AuthHelper {
    suspend fun signInGoogle(
        context: Context,
        credentialManager: CredentialManager,
        onSignedIn: (String) -> Unit
    ) {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId("441243986243-ks2t66rum6ee4nq1k62mnn68gauth5q1.apps.googleusercontent.com")
            .setFilterByAuthorizedAccounts(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        try {
            val result = credentialManager.getCredential(request = request, context = context)
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
            onSignedIn(googleIdTokenCredential.idToken)
        } catch (e: Exception) {
            Log.e(
                MainActivity::class.simpleName,
                "MainNavigation: $e"
            )
        }
    }
}