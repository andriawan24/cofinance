package id.andriawan.cofinance.auth

/**
 * JavaScript implementation of GoogleAuthManager
 * Currently not supported - returns error
 */
actual class GoogleAuthManager {

    /**
     * Indicate that Google Sign-In is unsupported on the JavaScript platform.
     *
     * @return `GoogleAuthResult.Error` with the message "Google Sign-In is not supported on Web".
     */
    actual suspend fun signIn(): GoogleAuthResult {
        // JS doesn't support native Google Sign-In
        // Could implement OAuth browser flow in the future
        return GoogleAuthResult.Error("Google Sign-In is not supported on Web")
    }

    /**
     * No-op sign-out implementation for the JavaScript platform.
     *
     * This implementation intentionally performs no action because Google Sign-In is not supported on Web.
     */
    actual fun signOut() {
        // No-op for JS
    }
}