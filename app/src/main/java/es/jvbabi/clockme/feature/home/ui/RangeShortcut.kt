package es.jvbabi.clockme.feature.home.ui

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** Predefined time ranges, each ending with [today] (capped at now when resolved). */
enum class RangeShortcut {
    TODAY,
    YESTERDAY,
    THIS_WEEK,
    THIS_MONTH,
    ALL,
    ;

    fun toFilter(today: LocalDate, timeZone: TimeZone): TimeRangeFilter = when (this) {
        TODAY -> TimeRangeFilter.ofDay(today, timeZone)
        YESTERDAY -> TimeRangeFilter.ofDay(today.minus(1, DateTimeUnit.DAY), timeZone)
        THIS_WEEK -> untilEndOf(today, today.minus(today.dayOfWeek.ordinal - DayOfWeek.MONDAY.ordinal, DateTimeUnit.DAY), timeZone)
        THIS_MONTH -> untilEndOf(today, LocalDate(today.year, today.month, 1), timeZone)
        ALL -> untilEndOf(today, BEGINNING_OF_TIME, timeZone)
    }

    companion object {
        private val BEGINNING_OF_TIME = LocalDate(1970, 1, 1)

        /** This week and this month are left out while they would be the same as today. */
        fun available(today: LocalDate): List<RangeShortcut> = entries.filter {
            when (it) {
                THIS_WEEK -> today.dayOfWeek != DayOfWeek.MONDAY
                THIS_MONTH -> today.day != 1
                else -> true
            }
        }

        private fun untilEndOf(today: LocalDate, from: LocalDate, timeZone: TimeZone) = TimeRangeFilter(
            from = from.atStartOfDayIn(timeZone),
            to = today.plus(1, DateTimeUnit.DAY).atStartOfDayIn(timeZone),
        )
    }
}
