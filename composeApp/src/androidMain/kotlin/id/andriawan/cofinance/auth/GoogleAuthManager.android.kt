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

    /**
     * Initiates a Google sign-in using the Android Credential Manager and maps the outcome to a `GoogleAuthResult`.
     *
     * Attempts to obtain the current activity from `AndroidContextHolder` and request Google ID credentials. On a successful credential response the result will be parsed and returned as `GoogleAuthResult.Success`. If the user cancels the flow the result will be `GoogleAuthResult.Cancelled`. Any failures produce `GoogleAuthResult.Error` with a descriptive message and, when available, the underlying exception.
     *
     * @return `GoogleAuthResult.Success` with `idToken` and `email` on success, `GoogleAuthResult.Cancelled` if the credential flow was cancelled, or `GoogleAuthResult.Error` with a message and optional exception for other failures.
     */
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

    /**
     * Convert a Credential Manager response into a GoogleAuthResult by extracting and validating a Google ID token credential.
     *
     * @param result The credential response returned by the Credential Manager.
     * @return `GoogleAuthResult.Success` containing `idToken` and `email` when a valid Google ID token credential is present; `GoogleAuthResult.Error` when the credential type is unexpected or the ID token cannot be parsed.
     */
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

    /**
     * No-op placeholder for signing out of Google authentication on Android.
     *
     * Credential Manager does not provide a sign-out operation; actual sign-out is performed by Supabase Auth.
     */
    actual fun signOut() {
        // Google Sign-In via Credential Manager doesn't have a sign-out method
        // Sign-out is handled by Supabase Auth
    }
}