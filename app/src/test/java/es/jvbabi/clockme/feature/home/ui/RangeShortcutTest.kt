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
    fun shortcutsAreWholeUnits() {
        // The current ones are capped at now once resolved
        assertEquals(TimeRangeFilter(startOf("2026-11-12"), startOf("2026-11-13")), RangeShortcut.TODAY.toFilter(today, berlin))
        assertEquals(TimeRangeFilter(startOf("2026-11-11"), startOf("2026-11-12")), RangeShortcut.YESTERDAY.toFilter(today, berlin))
        assertEquals(TimeRangeFilter(startOf("2026-11-09"), startOf("2026-11-16")), RangeShortcut.THIS_WEEK.toFilter(today, berlin))
        assertEquals(TimeRangeFilter(startOf("2026-11-01"), startOf("2026-12-01")), RangeShortcut.THIS_MONTH.toFilter(today, berlin))
        assertEquals(TimeRangeFilter(startOf("1970-01-01"), startOf("2026-11-13")), RangeShortcut.ALL.toFilter(today, berlin))
    }

    @Test
    fun pagesGoBackUnitByUnit() {
        assertEquals(LocalDate.parse("2026-11-09"), PageUnit.DAY.firstDay(today, -3))
        assertEquals(LocalDate.parse("2026-10-26"), PageUnit.WEEK.firstDay(today, -2))
        assertEquals(LocalDate.parse("2025-12-01"), PageUnit.MONTH.firstDay(today, -11))
        assertEquals(
            TimeRangeFilter(startOf("2026-10-26"), startOf("2026-11-02")),
            PageUnit.WEEK.filter(today, -2, berlin),
        )
    }

    @Test
    fun isoWeekNumbers() {
        assertEquals(46, today.isoWeekNumber)
        // Week 1 contains the first Thursday of the year
        assertEquals(53, LocalDate.parse("2027-01-01").isoWeekNumber)
        assertEquals(1, LocalDate.parse("2027-01-04").isoWeekNumber)
        assertEquals(1, LocalDate.parse("2025-12-29").isoWeekNumber)
        assertEquals(52, LocalDate.parse("2025-12-28").isoWeekNumber)
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
