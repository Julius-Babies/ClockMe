package es.jvbabi.clockme.domain.calendar

import es.jvbabi.clockme.data.calendar.DailyCalendarMinutes
import es.jvbabi.clockme.data.calendar.EventInstance
import es.jvbabi.clockme.data.calendar.EventSegment
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Instant

class EventDaysTest {

    private val berlin = TimeZone.of("Europe/Berlin")

    private fun at(date: String, time: String): Instant =
        LocalDateTime.parse("${date}T$time").toInstant(berlin)

    private fun event(start: Instant, end: Instant, eventId: Long = 1, calendarId: Long = 10) =
        EventInstance(eventId = eventId, calendarId = calendarId, start = start, end = end)

    private fun segment(date: String, start: Instant, end: Instant, eventId: Long = 1, calendarId: Long = 10) =
        EventSegment(calendarId = calendarId, eventId = eventId, date = LocalDate.parse(date), start = start, end = end)

    @Test
    fun eventWithinOneDayIsOneSegment() {
        val start = at("2026-11-12", "09:00")
        val end = at("2026-11-12", "10:30")

        assertEquals(listOf(segment("2026-11-12", start, end)), event(start, end).splitIntoDays(berlin))
    }

    @Test
    fun eventAcrossMidnightIsSplitPerDay() {
        val start = at("2026-11-12", "22:00")
        val end = at("2026-11-13", "02:00")

        assertEquals(
            listOf(
                segment("2026-11-12", start, at("2026-11-13", "00:00")),
                segment("2026-11-13", at("2026-11-13", "00:00"), end),
            ),
            event(start, end).splitIntoDays(berlin),
        )
    }

    @Test
    fun multiDayEventHasFullDaysInBetween() {
        val start = at("2026-11-12", "20:00")
        val end = at("2026-11-14", "08:00")

        assertEquals(
            listOf(
                segment("2026-11-12", start, at("2026-11-13", "00:00")),
                segment("2026-11-13", at("2026-11-13", "00:00"), at("2026-11-14", "00:00")),
                segment("2026-11-14", at("2026-11-14", "00:00"), end),
            ),
            event(start, end).splitIntoDays(berlin),
        )
    }

    @Test
    fun eventEndingAtMidnightStaysOnItsDay() {
        val start = at("2026-11-12", "22:00")
        val end = at("2026-11-13", "00:00")

        assertEquals(listOf(segment("2026-11-12", start, end)), event(start, end).splitIntoDays(berlin))
    }

    @Test
    fun dayWithDaylightSavingChangeHasItsRealLength() {
        // 2026-03-29 has only 23 hours in Berlin
        val segments = event(at("2026-03-28", "12:00"), at("2026-03-30", "12:00")).splitIntoDays(berlin)

        assertEquals(listOf(720, 1380, 720), segments.map { (it.end - it.start).inWholeMinutes.toInt() })
    }

    @Test
    fun eventWithoutDurationHasNoSegments() {
        val start = at("2026-11-12", "09:00")

        assertEquals(emptyList<EventSegment>(), event(start, start).splitIntoDays(berlin))
    }

    @Test
    fun overlappingEventsCountOnce() {
        val segments = listOf(
            segment("2026-11-12", at("2026-11-12", "09:00"), at("2026-11-12", "11:00"), eventId = 1),
            segment("2026-11-12", at("2026-11-12", "10:00"), at("2026-11-12", "12:00"), eventId = 2),
            // nested in the first one
            segment("2026-11-12", at("2026-11-12", "09:30"), at("2026-11-12", "10:00"), eventId = 3),
        )

        assertEquals(
            listOf(DailyCalendarMinutes(calendarId = 10, date = LocalDate.parse("2026-11-12"), minutes = 180)),
            segments.minutesPerCalendarAndDay(),
        )
    }

    @Test
    fun separateAndAdjacentEventsAreSummedUp() {
        val segments = listOf(
            segment("2026-11-12", at("2026-11-12", "09:00"), at("2026-11-12", "10:00"), eventId = 1),
            segment("2026-11-12", at("2026-11-12", "10:00"), at("2026-11-12", "10:30"), eventId = 2),
            segment("2026-11-12", at("2026-11-12", "14:00"), at("2026-11-12", "14:45"), eventId = 3),
        )

        assertEquals(135, segments.minutesPerCalendarAndDay().single().minutes)
    }

    @Test
    fun calendarsAndDaysAreAggregatedSeparately() {
        val segments = listOf(
            segment("2026-11-12", at("2026-11-12", "09:00"), at("2026-11-12", "10:00"), calendarId = 10),
            // overlaps, but in another calendar
            segment("2026-11-12", at("2026-11-12", "09:00"), at("2026-11-12", "09:30"), calendarId = 20, eventId = 2),
            segment("2026-11-13", at("2026-11-13", "09:00"), at("2026-11-13", "09:15"), calendarId = 10, eventId = 3),
        )

        assertEquals(
            listOf(
                DailyCalendarMinutes(calendarId = 10, date = LocalDate.parse("2026-11-12"), minutes = 60),
                DailyCalendarMinutes(calendarId = 20, date = LocalDate.parse("2026-11-12"), minutes = 30),
                DailyCalendarMinutes(calendarId = 10, date = LocalDate.parse("2026-11-13"), minutes = 15),
            ),
            segments.minutesPerCalendarAndDay(),
        )
    }

    @Test
    fun partialMinutesAreCutOff() {
        val start = at("2026-11-12", "09:00")
        val segments = listOf(segment("2026-11-12", start, Instant.fromEpochMilliseconds(start.toEpochMilliseconds() + 119_000)))

        assertEquals(1, segments.minutesPerCalendarAndDay().single().minutes)
    }
}
