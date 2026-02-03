package id.andriawan.cofinance.auth

/**
 * Desktop implementation of GoogleAuthManager
 * Currently not supported - returns error
 */
actual class GoogleAuthManager {

    actual suspend fun signIn(): GoogleAuthResult {
        // Desktop doesn't support Google Sign-In natively
        // Could implement OAuth browser flow in the future
        return GoogleAuthResult.Error("Google Sign-In is not supported on Desktop")
    }

    actual fun signOut() {
        // No-op for desktop
    }
}
