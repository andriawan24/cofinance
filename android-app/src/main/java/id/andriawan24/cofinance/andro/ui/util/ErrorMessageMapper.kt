package id.andriawan24.cofinance.andro.ui.util

import java.io.IOException
import retrofit2.HttpException

private const val NETWORK_ERROR_MESSAGE = "Network error. Please check your connection."
private const val SERVER_ERROR_MESSAGE = "Server error. Please try again later."
private const val UNKNOWN_ERROR_MESSAGE = "Unknown error occurred."
private const val GENERIC_ERROR_MESSAGE = "Something went wrong. Please try again."

fun mapErrorMessage(
    exception: Throwable?,
    fallbackMessage: String = GENERIC_ERROR_MESSAGE
): String {
    return when (exception) {
        is IOException -> NETWORK_ERROR_MESSAGE
        is HttpException -> SERVER_ERROR_MESSAGE
        null -> UNKNOWN_ERROR_MESSAGE
        else -> fallbackMessage
    }
}
