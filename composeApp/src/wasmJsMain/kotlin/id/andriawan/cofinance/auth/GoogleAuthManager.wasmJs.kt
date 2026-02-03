package id.andriawan.cofinance.auth

/**
 * WasmJS implementation of GoogleAuthManager
 * Currently not supported - returns error
 */
actual class GoogleAuthManager {

    actual suspend fun signIn(): GoogleAuthResult {
        // WasmJS doesn't support native Google Sign-In
        // Could implement OAuth browser flow in the future
        return GoogleAuthResult.Error("Google Sign-In is not supported on Web")
    }

    actual fun signOut() {
        // No-op for WasmJS
    }
}
