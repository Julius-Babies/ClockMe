package es.jvbabi.clockme.feature.home.ui

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * Predefined time ranges, each ending with [today] (capped at now when resolved), except for the
 * past ones (yesterday, last week, last month).
 */
enum class RangeShortcut {
    TODAY,
    YESTERDAY,
    THIS_WEEK,
    LAST_WEEK,
    THIS_MONTH,
    LAST_MONTH,
    ALL,
    ;

    fun toFilter(today: LocalDate, timeZone: TimeZone): TimeRangeFilter = when (this) {
        TODAY -> TimeRangeFilter.ofDay(today, timeZone)
        YESTERDAY -> TimeRangeFilter.ofDay(today.minus(1, DateTimeUnit.DAY), timeZone)
        THIS_WEEK -> untilEndOf(today, startOfWeek(today), timeZone)
        LAST_WEEK -> between(startOfWeek(today).minus(1, DateTimeUnit.WEEK), startOfWeek(today), timeZone)
        THIS_MONTH -> untilEndOf(today, startOfMonth(today), timeZone)
        LAST_MONTH -> between(startOfMonth(today).minus(1, DateTimeUnit.MONTH), startOfMonth(today), timeZone)
        ALL -> untilEndOf(today, BEGINNING_OF_TIME, timeZone)
    }

    companion object {
        private val BEGINNING_OF_TIME = LocalDate(1970, 1, 1)

        /**
         * While this week or this month would be the same as today (on Mondays or the 1st), the
         * previous week or month takes its place.
         */
        fun available(today: LocalDate): List<RangeShortcut> = entries.filter {
            when (it) {
                THIS_WEEK -> today.dayOfWeek != DayOfWeek.MONDAY
                LAST_WEEK -> today.dayOfWeek == DayOfWeek.MONDAY
                THIS_MONTH -> today.day != 1
                LAST_MONTH -> today.day == 1
                else -> true
            }
        }

        private fun startOfWeek(today: LocalDate) =
            today.minus(today.dayOfWeek.ordinal - DayOfWeek.MONDAY.ordinal, DateTimeUnit.DAY)

        private fun startOfMonth(today: LocalDate) = LocalDate(today.year, today.month, 1)

        /** From the start of [from] until the start of [until]. */
        private fun between(from: LocalDate, until: LocalDate, timeZone: TimeZone) = TimeRangeFilter(
            from = from.atStartOfDayIn(timeZone),
            to = until.atStartOfDayIn(timeZone),
        )

        private fun untilEndOf(today: LocalDate, from: LocalDate, timeZone: TimeZone) = TimeRangeFilter(
            from = from.atStartOfDayIn(timeZone),
            to = today.plus(1, DateTimeUnit.DAY).atStartOfDayIn(timeZone),
        )
    }
}
