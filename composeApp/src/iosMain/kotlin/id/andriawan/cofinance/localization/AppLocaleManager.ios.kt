package id.andriawan.cofinance.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.languageCode

@Composable
actual fun rememberAppLocale(): AppLang {
    val nsLocale = NSLocale.currentLocale.languageCode

    return remember(nsLocale) {
        when (nsLocale) {
            "id" -> AppLang.Indonesian
            else -> AppLang.English
        }
    }
}
