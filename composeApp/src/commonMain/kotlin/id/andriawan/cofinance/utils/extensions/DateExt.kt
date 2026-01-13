package id.andriawan.cofinance.utils.extensions

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import kotlinx.datetime.number
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

const val FORMAT_DAY_MONTH_YEAR = "EEE, dd MMMM yyyy"
const val FORMAT_HOUR_MINUTE = "HH:mm"
const val FORMAT_ISO_8601 = "yyyy-MM-dd HH:mm:ssXXX"

val formatFull = LocalDateTime.Format {
    day()
    char(' ')
    monthName(MonthNames.ENGLISH_FULL)
    char(' ')
    year()
    chars(", ")
    hour()
    char(':')
    minute()
}

val formatMonth = LocalDateTime.Format {
    monthName(MonthNames.ENGLISH_FULL)
}

@OptIn(ExperimentalTime::class)
fun String.toDate(): Instant {
    if (this.isBlank()) {
        return Clock.System.now()
    }
    return Instant.parse(this)
}

fun Pair<Int, Int>.formatToString(
    format: DateTimeFormat<LocalDateTime> = formatFull,
    timeZone: TimeZone = TimeZone.currentSystemDefault()
): String {
    return try {
        val dateTime = LocalDate(
            year = first,
            month = second,
            day = 1
        ).atStartOfDayIn(timeZone)

        format.format(dateTime.toLocalDateTime(timeZone))
    } catch (_: Exception) {
        "Invalid date format"
    }
}

fun Instant.formatToString(
    format: DateTimeFormat<LocalDateTime> = LocalDateTime.Format {
        day()
        char(' ')
        monthName(MonthNames.ENGLISH_FULL)
        char(' ')
        year()
        chars(", ")
        hour()
        char(':')
        minute()
    },
    timeZone: TimeZone = TimeZone.currentSystemDefault()
): String {
    return try {
        format.format(toLocalDateTime(timeZone))
    } catch (_: Exception) {
        "Invalid date format"
    }
}

fun Instant.setTime(
    hour: Int,
    minute: Int,
    timeZone: TimeZone = TimeZone.currentSystemDefault()
): Instant {
    val currentDateTime = toLocalDateTime(timeZone)

    return LocalDateTime(
        year = currentDateTime.year,
        month = currentDateTime.month,
        day = currentDateTime.day,
        hour = hour,
        minute = minute,
        second = currentDateTime.second,
        nanosecond = currentDateTime.nanosecond
    ).toInstant(timeZone)
}

fun Instant.setDate(
    dateMillis: Long,
    timeZone: TimeZone = TimeZone.currentSystemDefault()
): Instant {
    val newDate = Instant.fromEpochMilliseconds(dateMillis)
        .toLocalDateTime(timeZone)

    val currentTime = toLocalDateTime(timeZone)

    return LocalDateTime(
        year = newDate.year,
        month = newDate.month,
        day = newDate.day,
        hour = currentTime.hour,
        minute = currentTime.minute,
        second = currentTime.second,
        nanosecond = currentTime.nanosecond
    ).toInstant(timeZone)
}

fun getCurrentYear(
    timeZone: TimeZone = TimeZone.currentSystemDefault()
): Int =
    Clock.System.now().toLocalDateTime(timeZone).year


fun getCurrentMonth(
    timeZone: TimeZone = TimeZone.currentSystemDefault()
): Int =
    Clock.System.now().toLocalDateTime(timeZone).month.number

fun getMonthLabel(
    month: Int
): String {
    return Month(month).name.lowercase().replaceFirstChar { it.uppercaseChar() }
}
