package id.andriawan24.cofinance.andro.utils.ext

import io.github.aakira.napier.Napier
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Date.formatToString(format: String = "dd MMM yyyy, HH:mm z"): String {
    return try {
        val formatter = SimpleDateFormat(format, Locale("id", "ID"))
        formatter.format(this)
    } catch (e: Exception) {
        Napier.e("Failed to fetch date", e)
        "Failed to fetch date"
    }
}