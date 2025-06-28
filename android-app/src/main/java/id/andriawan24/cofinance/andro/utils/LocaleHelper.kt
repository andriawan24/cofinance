package id.andriawan24.cofinance.andro.utils

import android.content.res.Resources
import androidx.core.os.ConfigurationCompat
import java.util.Locale


object LocaleHelper {
    val indonesian: Locale = Locale.forLanguageTag("id-ID")

    fun getCurrentLocale(): Locale {
        return ConfigurationCompat.getLocales(Resources.getSystem().configuration).get(0)
            ?: Locale.getDefault()
    }
}