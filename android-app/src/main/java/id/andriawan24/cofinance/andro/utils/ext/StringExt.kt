package id.andriawan24.cofinance.andro.utils.ext

import java.util.Locale

fun String.titlecase(): String {
    if (this.isBlank()) return this
    return this.lowercase()
        .replaceFirstChar {
            it.titlecase(Locale.getDefault())
        }
}