package id.andriawan.cofinance.utils

import cofinance.composeapp.generated.resources.Res
import cofinance.composeapp.generated.resources.error_auth_failed
import cofinance.composeapp.generated.resources.error_network
import cofinance.composeapp.generated.resources.error_unknown
import kotlinx.io.IOException

fun mapAuthErrorMessage(
    exception: Throwable?,
    fallback: UiText = UiText.Res(Res.string.error_auth_failed)
): UiText {
    return when (exception) {
        is IOException -> UiText.Res(Res.string.error_network)
        null -> UiText.Res(Res.string.error_unknown)
        else -> exception.message?.takeIf { it.isNotBlank() }?.let { UiText.Raw(it) } ?: fallback
    }
}
