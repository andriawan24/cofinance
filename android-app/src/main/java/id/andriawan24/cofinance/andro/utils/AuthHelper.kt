package id.andriawan24.cofinance.andro.utils

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import id.andriawan24.cofinance.andro.BuildConfig
import io.github.aakira.napier.Napier

object AuthHelper {
    suspend fun signInGoogle(
        context: Context,
        credentialManager: CredentialManager,
        onSignedIn: (String) -> Unit,
        onSignedInFailed: (String) -> Unit
    ) {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(BuildConfig.GOOGLE_CLIENT_ID)
            .setFilterByAuthorizedAccounts(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        try {
            val result = credentialManager.getCredential(request = request, context = context)
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
            onSignedIn(googleIdTokenCredential.idToken)
        } catch (e: GetCredentialCancellationException) {
            // do nothing
        } catch (e: NoCredentialException) {
            onSignedInFailed(e.message.orEmpty())
        } catch (e: Exception) {
            Napier.e("Failed to signed in", e)
            onSignedInFailed(e.message.orEmpty())
        }
    }
}