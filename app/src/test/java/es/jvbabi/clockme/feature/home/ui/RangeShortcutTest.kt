package es.jvbabi.clockme.feature.home.ui

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Instant

class RangeShortcutTest {

    private val berlin = TimeZone.of("Europe/Berlin")
    private fun startOf(date: String): Instant = LocalDateTime.parse("${date}T00:00").toInstant(berlin)

    // A Thursday
    private val today = LocalDate.parse("2026-11-12")

    @Test
    fun rangesEndWithToday() {
        val end = startOf("2026-11-13")

        assertEquals(TimeRangeFilter(startOf("2026-11-12"), end), RangeShortcut.TODAY.toFilter(today, berlin))
        assertEquals(TimeRangeFilter(startOf("2026-11-11"), startOf("2026-11-12")), RangeShortcut.YESTERDAY.toFilter(today, berlin))
        assertEquals(TimeRangeFilter(startOf("2026-11-09"), end), RangeShortcut.THIS_WEEK.toFilter(today, berlin))
        assertEquals(TimeRangeFilter(startOf("2026-11-01"), end), RangeShortcut.THIS_MONTH.toFilter(today, berlin))
        assertEquals(TimeRangeFilter(startOf("1970-01-01"), end), RangeShortcut.ALL.toFilter(today, berlin))
    }

    @Test
    fun pastWeekAndMonthAreTheWholePreviousOnes() {
        assertEquals(TimeRangeFilter(startOf("2026-11-02"), startOf("2026-11-09")), RangeShortcut.LAST_WEEK.toFilter(today, berlin))
        assertEquals(TimeRangeFilter(startOf("2026-10-01"), startOf("2026-11-01")), RangeShortcut.LAST_MONTH.toFilter(today, berlin))
        // Across the turn of the year
        assertEquals(
            TimeRangeFilter(startOf("2026-12-01"), startOf("2027-01-01")),
            RangeShortcut.LAST_MONTH.toFilter(LocalDate.parse("2027-01-01"), berlin),
        )
    }

    @Test
    fun thisWeekAndThisMonthMidWeekAndMidMonth() {
        assertEquals(
            listOf(RangeShortcut.TODAY, RangeShortcut.YESTERDAY, RangeShortcut.THIS_WEEK, RangeShortcut.THIS_MONTH, RangeShortcut.ALL),
            RangeShortcut.available(today),
        )
    }

    @Test
    fun lastWeekReplacesThisWeekOnMondays() {
        assertEquals(
            listOf(RangeShortcut.TODAY, RangeShortcut.YESTERDAY, RangeShortcut.LAST_WEEK, RangeShortcut.THIS_MONTH, RangeShortcut.ALL),
            RangeShortcut.available(LocalDate.parse("2026-11-09")),
        )
    }

    @Test
    fun lastMonthReplacesThisMonthOnTheFirst() {
        // A Sunday
        assertEquals(
            listOf(RangeShortcut.TODAY, RangeShortcut.YESTERDAY, RangeShortcut.THIS_WEEK, RangeShortcut.LAST_MONTH, RangeShortcut.ALL),
            RangeShortcut.available(LocalDate.parse("2026-11-01")),
        )
    }
}
