package id.andriawan.cofinance.utils

import kotlinx.io.IOException

private const val NETWORK_ERROR_MESSAGE = "Network error. Please check your connection."
private const val UNKNOWN_ERROR_MESSAGE = "Unknown error occurred."
private const val GENERIC_ERROR_MESSAGE = "Authentication failed. Please try again."

/**
 * Maps low-level exceptions coming from authentication layers into user-friendly messages
 * that can safely be shown in the UI.
 */
fun mapAuthErrorMessage(
    exception: Throwable?,
    fallbackMessage: String = GENERIC_ERROR_MESSAGE
): String {
    return when (exception) {
        is IOException -> NETWORK_ERROR_MESSAGE
        null -> UNKNOWN_ERROR_MESSAGE
        else -> exception.message?.takeIf { it.isNotBlank() } ?: fallbackMessage
    }
}
