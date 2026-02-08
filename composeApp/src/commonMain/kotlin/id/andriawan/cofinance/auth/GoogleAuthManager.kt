package id.andriawan.cofinance.auth

/**
 * Result of Google Sign-In operation
 */
sealed class GoogleAuthResult {
    data class Success(val idToken: String, val email: String?) : GoogleAuthResult()
    data class Error(val message: String, val exception: Exception? = null) : GoogleAuthResult()
    data object Cancelled : GoogleAuthResult()
}

/**
 * Platform-specific Google Sign-In manager
 * Implementations handle native Google Sign-In flows for each platform
 */
expect class GoogleAuthManager() {
    /**
 * Starts the Google Sign-In flow.
 *
 * @return A [GoogleAuthResult] representing either a successful sign-in with an ID token and optional email, a cancellation by the user, or an error with a message and optional exception.
 */
    suspend fun signIn(): GoogleAuthResult

    /**
 * Signs the current user out of Google for this manager.
 */
    fun signOut()
}