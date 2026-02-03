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
     * Initiates the Google Sign-In flow
     * @return GoogleAuthResult indicating success, cancellation, or error
     */
    suspend fun signIn(): GoogleAuthResult

    /**
     * Signs out from Google
     */
    fun signOut()
}
