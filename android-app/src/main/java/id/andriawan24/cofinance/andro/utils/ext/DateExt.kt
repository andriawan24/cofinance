package id.andriawan24.cofinance.andro.utils.ext

import io.github.aakira.napier.Napier
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.Month
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.time.toJavaInstant

const val FORMAT_DAY_MONTH_YEAR = "EEE, dd MMMM yyyy"

@OptIn(ExperimentalTime::class)
fun String.toDate(): Date {
    if (this.isBlank()) {
        return Date()
    }

    val instant = Instant.parse(this)

    return Date.from(instant.toJavaInstant())
}

fun Date.formatToString(
    format: String = "dd MMM yyyy, HH:mm z",
    locale: Locale = Locale.getDefault()
): String {
    return try {
        val formatter = SimpleDateFormat(format, locale)
        formatter.format(this)
    } catch (e: Exception) {
        Napier.e("Failed to fetch date", e)
        "Failed to fetch date"
    }
}

fun getCurrentYear(): Int = LocalDate.now().year
fun getCurrentMonth(): Int = LocalDate.now().monthValue
fun getMonthLabel(month: Int, locale: Locale = Locale.getDefault()): String {
    return Month.of(month).getDisplayName(TextStyle.FULL, locale)
}
