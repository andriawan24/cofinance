package id.andriawan.cofinance.auth

/**
 * Desktop implementation of GoogleAuthManager
 * Currently not supported - returns error
 */
actual class GoogleAuthManager {

    /**
     * Attempt to sign in with Google on desktop.
     *
     * This desktop implementation does not support Google Sign-In.
     *
     * @return `GoogleAuthResult.Error` with message "Google Sign-In is not supported on Desktop".
     */
    actual suspend fun signIn(): GoogleAuthResult {
        // Desktop doesn't support Google Sign-In natively
        // Could implement OAuth browser flow in the future
        return GoogleAuthResult.Error("Google Sign-In is not supported on Desktop")
    }

    /**
     * Performs no operation — sign-out is a no-op on Desktop.
     *
     * Calling this function has no effect because Google authentication is not supported in the desktop implementation.
     */
    actual fun signOut() {
        // No-op for desktop
    }
}