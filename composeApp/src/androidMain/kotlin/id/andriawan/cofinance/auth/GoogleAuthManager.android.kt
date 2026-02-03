package id.andriawan.cofinance.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.andriawan.cofinance.BuildKonfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Holder for Android Context - must be set during app initialization
 */
object AndroidContextHolder {
    var applicationContext: Context? = null
    var currentActivity: Activity? = null
}

actual class GoogleAuthManager {

    actual suspend fun signIn(): GoogleAuthResult = withContext(Dispatchers.Main) {
        val activity = AndroidContextHolder.currentActivity
        if (activity == null) {
            return@withContext GoogleAuthResult.Error("Activity not available. Please set AndroidContextHolder.currentActivity.")
        }

        val credentialManager = CredentialManager.create(activity)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(BuildKonfig.GOOGLE_AUTH_API_KEY)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        try {
            val result = credentialManager.getCredential(
                context = activity,
                request = request
            )
            handleSignInResult(result)
        } catch (e: GetCredentialCancellationException) {
            GoogleAuthResult.Cancelled
        } catch (e: GetCredentialException) {
            GoogleAuthResult.Error(e.message ?: "Failed to get credentials", e)
        } catch (e: Exception) {
            GoogleAuthResult.Error(e.message ?: "Unknown error occurred", e)
        }
    }

    private fun handleSignInResult(result: GetCredentialResponse): GoogleAuthResult {
        return when (val credential = result.credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        GoogleAuthResult.Success(
                            idToken = googleIdTokenCredential.idToken,
                            email = googleIdTokenCredential.id
                        )
                    } catch (e: GoogleIdTokenParsingException) {
                        GoogleAuthResult.Error("Failed to parse Google ID token", e)
                    }
                } else {
                    GoogleAuthResult.Error("Unexpected credential type: ${credential.type}")
                }
            }
            else -> GoogleAuthResult.Error("Unexpected credential type")
        }
    }

    actual fun signOut() {
        // Google Sign-In via Credential Manager doesn't have a sign-out method
        // Sign-out is handled by Supabase Auth
    }
}
