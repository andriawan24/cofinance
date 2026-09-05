package id.andriawan.cofinance.auth

import coil3.PlatformContext

sealed class GoogleAuthResult {
    data class Success(
        val idToken: String,
        val accessToken: String?,
        val email: String?
    ) : GoogleAuthResult()

    data class Error(val message: String, val exception: Exception? = null) : GoogleAuthResult()
    data object Cancelled : GoogleAuthResult()
}

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect class GoogleAuthManager() {
    suspend fun signIn(context: PlatformContext): GoogleAuthResult
}
