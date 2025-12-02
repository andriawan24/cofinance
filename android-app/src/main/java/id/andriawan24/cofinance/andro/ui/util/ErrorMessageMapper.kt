package id.andriawan24.cofinance.andro.ui.util

import androidx.annotation.StringRes
import id.andriawan24.cofinance.andro.R
import java.io.IOException
import retrofit2.HttpException

@StringRes
fun mapErrorMessage(
    exception: Throwable?,
    @StringRes fallbackResId: Int = R.string.error_generic
): Int {
    return when (exception) {
        is IOException -> R.string.error_network
        is HttpException -> R.string.error_server
        null -> R.string.error_unknown
        else -> fallbackResId
    }
}
