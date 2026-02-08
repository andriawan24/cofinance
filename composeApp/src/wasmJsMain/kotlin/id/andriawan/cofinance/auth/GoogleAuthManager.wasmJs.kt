package id.andriawan.cofinance.auth

import coil3.PlatformContext

/**
 * WasmJS implementation of GoogleAuthManager
 * Currently not supported - returns error
 */
actual class GoogleAuthManager {

    actual suspend fun signIn(context: PlatformContext): GoogleAuthResult {
        return GoogleAuthResult.Error("Google Sign-In is not supported on Web")
    }

    actual fun signOut() {
        // No-op for WasmJS
    }
}
