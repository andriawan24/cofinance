package id.andriawan.cofinance.auth

import cocoapods.FirebaseAuth.FIROAuthCredential
import cocoapods.FirebaseAuth.FIROAuthProvider
import coil3.PlatformContext
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
@OptIn(ExperimentalForeignApi::class)
actual class GoogleAuthManager {

    actual suspend fun signIn(context: PlatformContext): GoogleAuthResult =
        suspendCancellableCoroutine { continuation ->
            val provider = FIROAuthProvider.providerWithProviderID(GOOGLE_PROVIDER_ID)
            provider.setScopes(GOOGLE_SCOPES)

            provider.getCredentialWithUIDelegate(null) { credential, error ->
                if (!continuation.isActive) return@getCredentialWithUIDelegate

                when {
                    error != null -> {
                        if (error.code == ERROR_CODE_WEB_CONTEXT_CANCELLED) {
                            continuation.resume(GoogleAuthResult.Cancelled)
                        } else {
                            continuation.resume(GoogleAuthResult.Error(error.localizedDescription))
                        }
                    }

                    else -> {
                        val idToken = (credential as? FIROAuthCredential)?.IDToken()
                        if (idToken == null) {
                            continuation.resume(
                                GoogleAuthResult.Error("Failed to get ID token from Google")
                            )
                        } else {
                            continuation.resume(GoogleAuthResult.Success(idToken, null))
                        }
                    }
                }
            }
        }

    private companion object {
        const val GOOGLE_PROVIDER_ID = "google.com"
        val GOOGLE_SCOPES = listOf("email", "profile")

        /** FIRAuthErrorCodeWebContextCancelled - user dismissed the sign-in web flow. */
        const val ERROR_CODE_WEB_CONTEXT_CANCELLED = 17_058L
    }
}
