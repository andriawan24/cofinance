package id.andriawan.cofinance.auth

import coil3.PlatformContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class GoogleAuthManager {

    actual suspend fun signIn(context: PlatformContext): GoogleAuthResult =
        suspendCancellableCoroutine { continuation ->
            val bridge = GoogleSignInBridgeRegistry.bridge

            if (bridge == null) {
                continuation.resume(GoogleAuthResult.Error(ERROR_BRIDGE_MISSING))
                return@suspendCancellableCoroutine
            }

            bridge.signIn { result ->
                if (!continuation.isActive) return@signIn

                when {
                    result.cancelled -> continuation.resume(GoogleAuthResult.Cancelled)

                    result.idToken != null -> continuation.resume(
                        GoogleAuthResult.Success(
                            idToken = result.idToken,
                            accessToken = result.accessToken,
                            email = result.email
                        )
                    )

                    else -> continuation.resume(
                        GoogleAuthResult.Error(result.errorMessage ?: ERROR_UNKNOWN)
                    )
                }
            }
        }

    private companion object {
        const val ERROR_BRIDGE_MISSING =
            "Google Sign-In is not available: native bridge was not registered"
        const val ERROR_UNKNOWN = "Unknown error occurred during Google Sign-In"
    }
}
