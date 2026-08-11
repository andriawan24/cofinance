package id.andriawan.cofinance.data.ocr.parser

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.number
import kotlinx.datetime.toInstant
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Extracts a transaction date from recognized lines.
 *
 * Formats are tried in priority order and month names are matched against an
 * Indonesian lexicon alongside English forms, because Indonesian receipts mix both.
 * Ambiguous numeric dates resolve day-first. A receipt that carries no timezone is
 * interpreted at the Asia/Jakarta offset, which is an explicit assumption of the
 * Indonesia-only user base rather than a device-locale reading.
 */
@OptIn(ExperimentalTime::class)
internal object ReceiptDateParser {

    val JAKARTA_OFFSET: UtcOffset = UtcOffset(hours = 7)

    private const val BASE_CONFIDENCE = 0.85f
    private const val TIME_BONUS = 0.1f
    private const val CONFLICT_CONFIDENCE = 0.6f

    private val DAY_FIRST_SLASH = Regex("(\\d{1,2})/(\\d{1,2})/(\\d{4})")
    private val DAY_FIRST_DASH = Regex("(\\d{1,2})-(\\d{1,2})-(\\d{4})")
    private val YEAR_FIRST_DASH = Regex("(\\d{4})-(\\d{1,2})-(\\d{1,2})")
    private val MONTH_NAME_DATE = Regex("(\\d{1,2})\\s+([A-Za-z]{3,12})\\.?\\s+(\\d{4})")
    private val TIME_OF_DAY = Regex("(\\d{1,2}):(\\d{2})(?::(\\d{2}))?")

    private val MONTHS: Map<String, Int> = buildMap {
        listOf(
            listOf("JANUARI", "JANUARY", "JAN"),
            listOf("FEBRUARI", "FEBRUARY", "FEB", "PEB"),
            listOf("MARET", "MARCH", "MAR"),
            listOf("APRIL", "APR"),
            listOf("MEI", "MAY"),
            listOf("JUNI", "JUNE", "JUN"),
            listOf("JULI", "JULY", "JUL"),
            listOf("AGUSTUS", "AUGUST", "AGU", "AGS", "AUG"),
            listOf("SEPTEMBER", "SEP", "SEPT"),
            listOf("OKTOBER", "OCTOBER", "OKT", "OCT"),
            listOf("NOVEMBER", "NOV", "NOP"),
            listOf("DESEMBER", "DECEMBER", "DES", "DEC")
        ).forEachIndexed { index, names -> names.forEach { put(it, index + 1) } }
    }

    data class DateResult(val isoDate: String, val confidence: Float)

    fun parse(lines: List<String>, now: Instant): DateResult {
        val accepted = lines.mapNotNull { line -> parseLine(line, now) }
        val chosen = accepted.firstOrNull() ?: return DateResult("", 0f)

        val distinct = accepted.distinctBy { it.dateTime }.size
        val confidence = if (distinct > 1) {
            minOf(chosen.confidence, CONFLICT_CONFIDENCE)
        } else {
            chosen.confidence
        }

        return DateResult(chosen.dateTime.toJakartaIso(), confidence)
    }

    private data class Candidate(val dateTime: LocalDateTime, val confidence: Float)

    private fun parseLine(line: String, now: Instant): Candidate? {
        val date = matchDayFirst(line, DAY_FIRST_SLASH)
            ?: matchDayFirst(line, DAY_FIRST_DASH)
            ?: matchYearFirst(line)
            ?: matchMonthName(line)
            ?: return null

        val time = TIME_OF_DAY.findAll(line)
            .firstOrNull { hasDigitBoundaries(line, it.range.first, it.range.last) }
        val dateTime = try {
            LocalDateTime(
                date = date,
                time = LocalTime(
                    hour = time?.groupValues?.get(1)?.toInt() ?: 0,
                    minute = time?.groupValues?.get(2)?.toInt() ?: 0,
                    second = time?.groupValues?.get(3)?.toIntOrNull() ?: 0
                )
            )
        } catch (_: IllegalArgumentException) {
            return null
        }

        if (dateTime.toInstant(JAKARTA_OFFSET) > now) return null

        return Candidate(dateTime, BASE_CONFIDENCE + (if (time != null) TIME_BONUS else 0f))
    }

    private fun matchDayFirst(line: String, pattern: Regex): LocalDate? {
        val match = pattern.findAll(line)
            .firstOrNull { hasDigitBoundaries(line, it.range.first, it.range.last) }
            ?: return null

        var day = match.groupValues[1].toInt()
        var month = match.groupValues[2].toInt()
        if (month > 12 && day <= 12) {
            val swapped = day
            day = month
            month = swapped
        }
        return localDateOrNull(match.groupValues[3].toInt(), month, day)
    }

    private fun matchYearFirst(line: String): LocalDate? {
        val match = YEAR_FIRST_DASH.findAll(line)
            .firstOrNull { hasDigitBoundaries(line, it.range.first, it.range.last) }
            ?: return null

        return localDateOrNull(
            year = match.groupValues[1].toInt(),
            month = match.groupValues[2].toInt(),
            day = match.groupValues[3].toInt()
        )
    }

    private fun matchMonthName(line: String): LocalDate? {
        val match = MONTH_NAME_DATE.find(line) ?: return null
        val month = MONTHS[match.groupValues[2].uppercase()] ?: return null
        return localDateOrNull(
            year = match.groupValues[3].toInt(),
            month = month,
            day = match.groupValues[1].toInt()
        )
    }

    private fun localDateOrNull(year: Int, month: Int, day: Int): LocalDate? = try {
        LocalDate(year = year, month = month, day = day)
    } catch (_: IllegalArgumentException) {
        null
    }

    private fun hasDigitBoundaries(line: String, start: Int, end: Int): Boolean =
        line.getOrNull(start - 1)?.isDigit() != true && line.getOrNull(end + 1)?.isDigit() != true

    private fun LocalDateTime.toJakartaIso(): String = buildString {
        append(year.toString().padStart(4, '0'))
        append('-')
        append(month.number.pad())
        append('-')
        append(day.pad())
        append('T')
        append(hour.pad())
        append(':')
        append(minute.pad())
        append(':')
        append(second.pad())
        append("+07:00")
    }

    private fun Int.pad(): String = toString().padStart(2, '0')
}
