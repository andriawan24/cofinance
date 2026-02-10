package id.andriawan.cofinance.auth

import coil3.PlatformContext

/**
 * Desktop implementation of GoogleAuthManager
 * Currently not supported - returns error
 */
actual class GoogleAuthManager {

    actual suspend fun signIn(context: PlatformContext): GoogleAuthResult {
        return GoogleAuthResult.Error("Google Sign-In is not supported on Desktop")
    }

    actual fun signOut() {
        // No-op for desktop
    }
}
