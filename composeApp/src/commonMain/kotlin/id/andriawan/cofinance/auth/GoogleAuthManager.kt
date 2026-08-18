package id.andriawan.cofinance.auth

import coil3.PlatformContext

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
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect class GoogleAuthManager() {
    suspend fun signIn(context: PlatformContext): GoogleAuthResult
}
