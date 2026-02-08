package id.andriawan.cofinance.auth

/**
 * WasmJS implementation of GoogleAuthManager
 * Currently not supported - returns error
 */
actual class GoogleAuthManager {

    /**
     * Signals that Google Sign-In is unsupported on the Web (WasmJS) target.
     *
     * @return A `GoogleAuthResult.Error` containing the message "Google Sign-In is not supported on Web".
     */
    actual suspend fun signIn(): GoogleAuthResult {
        // WasmJS doesn't support native Google Sign-In
        // Could implement OAuth browser flow in the future
        return GoogleAuthResult.Error("Google Sign-In is not supported on Web")
    }

    /**
     * Signs the current user out.
     *
     * This implementation is a no-op on WasmJS; Google Sign-In is not supported on the web target.
     */
    actual fun signOut() {
        // No-op for WasmJS
    }
}