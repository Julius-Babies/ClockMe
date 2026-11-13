package es.jvbabi.clockme.feature.home.ui

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** The unit home pages through, each page is one whole unit. */
enum class PageUnit {
    DAY,
    WEEK,
    MONTH,
    ;

    /** First day of the unit [offset] units away from the one containing [today] (0 is the current one). */
    fun firstDay(today: LocalDate, offset: Int): LocalDate = when (this) {
        DAY -> today.plus(offset, DateTimeUnit.DAY)
        WEEK -> startOfWeek(today).plus(offset, DateTimeUnit.WEEK)
        MONTH -> LocalDate(today.year, today.month, 1).plus(offset, DateTimeUnit.MONTH)
    }

    /** The day after the unit's last day. */
    fun endExclusive(firstDay: LocalDate): LocalDate = when (this) {
        DAY -> firstDay.plus(1, DateTimeUnit.DAY)
        WEEK -> firstDay.plus(1, DateTimeUnit.WEEK)
        MONTH -> firstDay.plus(1, DateTimeUnit.MONTH)
    }

    /** The whole unit [offset] units away from the current one, from 00:00 of its first day. */
    fun filter(today: LocalDate, offset: Int, timeZone: TimeZone): TimeRangeFilter {
        val first = firstDay(today, offset)
        return TimeRangeFilter(
            from = first.atStartOfDayIn(timeZone),
            to = endExclusive(first).atStartOfDayIn(timeZone),
        )
    }

    private companion object {
        fun startOfWeek(date: LocalDate) =
            date.minus(date.dayOfWeek.ordinal - DayOfWeek.MONDAY.ordinal, DateTimeUnit.DAY)
    }
}

/**
 * Predefined time ranges. All but [ALL] are a page of a [unit], [anchorOffset] units before the
 * current one; home pages from there through the history of that unit.
 */
enum class RangeShortcut(val unit: PageUnit?, val anchorOffset: Int = 0) {
    TODAY(PageUnit.DAY),
    YESTERDAY(PageUnit.DAY, anchorOffset = -1),
    THIS_WEEK(PageUnit.WEEK),
    LAST_WEEK(PageUnit.WEEK, anchorOffset = -1),
    THIS_MONTH(PageUnit.MONTH),
    LAST_MONTH(PageUnit.MONTH, anchorOffset = -1),
    ALL(unit = null),
    ;

    /** The shortcut's own range; the current unit is capped at now once resolved. */
    fun toFilter(today: LocalDate, timeZone: TimeZone): TimeRangeFilter =
        unit?.filter(today, anchorOffset, timeZone) ?: TimeRangeFilter(
            from = BEGINNING_OF_TIME.atStartOfDayIn(timeZone),
            to = today.plus(1, DateTimeUnit.DAY).atStartOfDayIn(timeZone),
        )

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
    }
}

/** The ISO 8601 week number (weeks start on Monday, week 1 contains the first Thursday). */
val LocalDate.isoWeekNumber: Int
    get() {
        // The Thursday of this week decides the week's year
        val thursday = plus(DayOfWeek.THURSDAY.isoDayNumber - dayOfWeek.isoDayNumber, DateTimeUnit.DAY)
        return (thursday.dayOfYear - 1) / 7 + 1
    }
