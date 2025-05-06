package id.andriawan24.cofinance.android.utils

import android.content.Context
import android.util.Log
import androidx.compose.runtime.remember
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import id.andriawan24.cofinance.android.R
import id.andriawan24.cofinance.android.ui.presentation.MainActivity

object AuthHelper {
    suspend fun googleSignIn(
        context: Context,
        credentialManager: CredentialManager,
        onSignedIn: () -> Unit
    ) {
        val auth = Firebase.auth

        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(context.getString(R.string.default_web_client_id))
            .setFilterByAuthorizedAccounts(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        try {
            val result = credentialManager.getCredential(
                request = request,
                context = context
            )

            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)

            val credential = GoogleAuthProvider.getCredential(
                googleIdTokenCredential.idToken,
                null
            )

            auth.signInWithCredential(credential)
                .addOnCompleteListener {
                    Log.d(
                        MainActivity::class.simpleName,
                        "MainNavigation - Success Login: ${it.result.user?.displayName}"
                    )
                    onSignedIn()
                }
                .addOnFailureListener {
                    Log.e(
                        MainActivity::class.simpleName,
                        "MainNavigation - Failed Login ${it.message}"
                    )
                }
        } catch (e: Exception) {
            Log.e(MainActivity::class.simpleName, "MainNavigation: ${e.message}")
        }
    }
}