package id.andriawan.cofinance.utils.extensions

import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

data class CycleDateRange(
    val startDate: String,
    val endDate: String,
    val label: String
)

fun computeCycleDateRange(month: Int, year: Int, cycleStartDay: Int): CycleDateRange {
    if (cycleStartDay == 1) {
        val startDate = LocalDate(year, month, 1)
        val endMonth = if (month == 12) 1 else month + 1
        val endYear = if (month == 12) year + 1 else year
        val endDate = LocalDate(endYear, endMonth, 1)

        val label = "${getMonthLabel(month)} $year"
        return CycleDateRange(
            startDate = startDate.toString(),
            endDate = endDate.toString(),
            label = label
        )
    }

    // Cycle starts on cycleStartDay of previous month, ends on cycleStartDay of current month
    // For month=3, year=2026, cycleStartDay=25: start = Feb 25, end = Mar 25
    val prevMonth = if (month == 1) 12 else month - 1
    val prevYear = if (month == 1) year - 1 else year

    val startDay = cycleStartDay.coerceAtMost(daysInMonth(prevYear, prevMonth))
    val startDate = LocalDate(prevYear, prevMonth, startDay)

    val endDay = cycleStartDay.coerceAtMost(daysInMonth(year, month))
    val endDate = LocalDate(year, month, endDay)

    // Label shows the inclusive range: startDate to endDate minus 1 day visually
    val lastInclusiveDay = endDay - 1
    val lastMonth: Int
    val lastYear: Int
    val lastDay: Int
    if (lastInclusiveDay < 1) {
        // Edge case: end is the 1st, so last inclusive day is last day of previous month
        lastMonth = prevMonth
        lastYear = prevYear
        lastDay = daysInMonth(prevYear, prevMonth)
    } else {
        lastMonth = month
        lastYear = year
        lastDay = lastInclusiveDay
    }

    val label = "${startDay} ${shortMonthName(prevMonth)} - $lastDay ${shortMonthName(lastMonth)} $lastYear"

    return CycleDateRange(
        startDate = startDate.toString(),
        endDate = endDate.toString(),
        label = label
    )
}

@OptIn(ExperimentalTime::class)
fun getCurrentCycleMonth(cycleStartDay: Int): Pair<Int, Int> {
    val today = Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault()).date
    val monthNum = today.month.number
    return if (cycleStartDay == 1) {
        Pair(monthNum, today.year)
    } else if (today.day >= cycleStartDay) {
        val nextMonth = if (monthNum == 12) 1 else monthNum + 1
        val nextYear = if (monthNum == 12) today.year + 1 else today.year
        Pair(nextMonth, nextYear)
    } else {
        Pair(monthNum, today.year)
    }
}

private fun daysInMonth(year: Int, month: Int): Int {
    return when (month) {
        1 -> 31
        2 -> if (isLeapYear(year)) 29 else 28
        3 -> 31
        4 -> 30
        5 -> 31
        6 -> 30
        7 -> 31
        8 -> 31
        9 -> 30
        10 -> 31
        11 -> 30
        12 -> 31
        else -> 30
    }
}

private fun isLeapYear(year: Int): Boolean {
    return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
}

private fun shortMonthName(month: Int): String {
    return Month(month).name.take(3).lowercase().replaceFirstChar { it.uppercaseChar() }
}

/**
 * Checks if the current cycle boundary has passed since the last reset.
 * Returns true if we are in a new cycle that hasn't been processed yet.
 */
@OptIn(ExperimentalTime::class)
fun isCycleBoundaryPassed(lastResetDate: String?, cycleStartDay: Int): Boolean {
    val today = Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault()).date

    val (currentCycleMonth, currentCycleYear) = getCurrentCycleMonth(cycleStartDay)
    val currentCycleRange = computeCycleDateRange(currentCycleMonth, currentCycleYear, cycleStartDay)
    val cycleStartDate = LocalDate.parse(currentCycleRange.startDate)

    if (lastResetDate == null) {
        // No reset recorded yet — check if we're past the first cycle start
        return today >= cycleStartDate
    }

    val lastReset = LocalDate.parse(lastResetDate)
    // If last reset was before the current cycle started, we need a new reset
    return lastReset < cycleStartDate
}

/**
 * Returns the end date string of the previous cycle (the cycle that just ended).
 */
@OptIn(ExperimentalTime::class)
fun getPreviousCycleEndDate(cycleStartDay: Int): String {
    val (currentCycleMonth, currentCycleYear) = getCurrentCycleMonth(cycleStartDay)
    val currentCycleRange = computeCycleDateRange(currentCycleMonth, currentCycleYear, cycleStartDay)
    return currentCycleRange.startDate // The end of prev cycle is the start of current cycle
}
