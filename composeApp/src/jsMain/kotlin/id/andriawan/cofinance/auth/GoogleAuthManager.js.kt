package id.andriawan.cofinance.auth

import coil3.PlatformContext

/**
 * JavaScript implementation of GoogleAuthManager
 * Currently not supported - returns error
 */
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class GoogleAuthManager {

    actual suspend fun signIn(context: PlatformContext): GoogleAuthResult {
        return GoogleAuthResult.Error("Google Sign-In is not supported on Web")
    }

    actual fun signOut() {
        // No-op for JS
    }
}
