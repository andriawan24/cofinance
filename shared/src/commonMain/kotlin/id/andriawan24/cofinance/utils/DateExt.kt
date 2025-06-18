package id.andriawan24.cofinance.utils

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.toLocalDateTime

fun String.toLocalDateTime(): LocalDateTime {
    if (this.isBlank()) {
        return getCurrentLocalDateTime()
    }

    val dateTime = Instant.parse(this, format = DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET)
    return dateTime.toLocalDateTime(TimeZone.currentSystemDefault())
}