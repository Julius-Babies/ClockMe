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
    fun allShortcutsAreAvailableMidWeekAndMidMonth() {
        assertEquals(RangeShortcut.entries, RangeShortcut.available(today))
    }

    @Test
    fun thisWeekIsLeftOutOnMondays() {
        assertEquals(
            listOf(RangeShortcut.TODAY, RangeShortcut.YESTERDAY, RangeShortcut.THIS_MONTH, RangeShortcut.ALL),
            RangeShortcut.available(LocalDate.parse("2026-11-09")),
        )
    }

    @Test
    fun thisMonthIsLeftOutOnTheFirst() {
        // A Sunday
        assertEquals(
            listOf(RangeShortcut.TODAY, RangeShortcut.YESTERDAY, RangeShortcut.THIS_WEEK, RangeShortcut.ALL),
            RangeShortcut.available(LocalDate.parse("2026-11-01")),
        )
    }
}
