package es.jvbabi.clockme.feature.home.ui

import es.jvbabi.clockme.data.calendar.Calendar
import es.jvbabi.clockme.data.calendar.CalendarAccount
import es.jvbabi.clockme.data.calendar.EventSegment
import es.jvbabi.clockme.data.group.CalendarGroup
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class HomeCalculationsTest {

    private val berlin = TimeZone.of("Europe/Berlin")
    private fun at(time: String, date: String = "2026-11-12"): Instant = LocalDateTime.parse("${date}T$time").toInstant(berlin)

    private val work = Calendar(id = 1, name = "Work", color = 1)
    private val gym = Calendar(id = 2, name = "Gym", color = 2)
    private val accounts = listOf(CalendarAccount(id = "LOCAL/Device", name = "Device", calendars = listOf(work, gym)))

    private fun segment(calendar: Calendar, eventId: Long, start: Instant, end: Instant) = EventSegment(
        calendarId = calendar.id,
        eventId = eventId,
        date = LocalDate.parse("2026-11-12"),
        start = start,
        end = end,
        title = "Event $eventId",
    )

    @Test
    fun defaultFilterIsTodayUntilNow() {
        val now = at("15:30")

        assertEquals(ResolvedTimeRange(from = at("00:00"), to = now), TimeRangeFilter().resolve(now, berlin))
    }

    @Test
    fun explicitBoundsAreKept() {
        val filter = TimeRangeFilter(from = at("08:00"), to = at("12:00"))

        assertEquals(ResolvedTimeRange(at("08:00"), at("12:00")), filter.resolve(at("15:30"), berlin))
    }

    @Test
    fun endIsCappedAtNow() {
        val filter = TimeRangeFilter.ofDay(LocalDate.parse("2026-11-12"), berlin)

        assertEquals(ResolvedTimeRange(at("00:00"), at("15:30")), filter.resolve(at("15:30"), berlin))
        assertEquals(
            ResolvedTimeRange(at("00:00"), at("00:00", date = "2026-11-13")),
            filter.resolve(at("09:00", date = "2026-11-14"), berlin),
        )
    }

    @Test
    fun timeIsCappedAtNowAndOverlapsCountOnce() {
        val range = ResolvedTimeRange(at("00:00"), at("15:00"))
        val segments = listOf(
            segment(work, 1, at("09:00"), at("11:00")),
            segment(work, 2, at("10:00"), at("11:30")),
            // ongoing, only counts until now
            segment(work, 3, at("14:30"), at("16:00")),
            // in the future
            segment(gym, 4, at("18:00"), at("19:00")),
        )

        assertEquals(
            listOf(
                CalendarTime(work.id, work.name, work.color, 180.minutes),
                CalendarTime(gym.id, gym.name, gym.color, Duration.ZERO),
            ),
            calendarTimes(accounts, segments, range),
        )
    }

    @Test
    fun calendarsOfAGroupAreNextToEachOtherBeforeUngroupedOnes() {
        val uni = Calendar(id = 3, name = "Uni", color = 3)
        val accounts = listOf(CalendarAccount(id = "LOCAL/Device", name = "Device", calendars = listOf(work, gym, uni)))
        val groups = listOf(
            CalendarGroup(id = 100, name = "Study", color = null, calendarIds = setOf(uni.id, work.id)),
            CalendarGroup(id = 200, name = "Empty", color = null, calendarIds = emptySet()),
        )
        val range = ResolvedTimeRange(at("00:00"), at("15:00"))

        val calendars = calendarTimes(accounts, emptyList(), range, groups)

        assertEquals(listOf(work.id, uni.id, gym.id), calendars.map { it.calendarId })
        assertEquals(listOf(100L, 100L, null), calendars.map { it.groupId })
        assertEquals(listOf(GroupInfo(100, "Study", null)), groupInfos(groups, calendars))
    }

    @Test
    fun eventsAreFilteredByRangeAndCalendar() {
        val range = ResolvedTimeRange(at("00:00"), at("15:00"))
        val segments = listOf(
            segment(work, 1, at("09:00"), at("10:00")),
            segment(gym, 2, at("12:00"), at("13:00")),
            segment(work, 3, at("14:30"), at("16:00")),
            segment(work, 4, at("18:00"), at("19:00")),
        )

        val all = eventItems(accounts, segments, range, calendarId = null)
        assertEquals(listOf(1L, 2L, 3L), all.map { it.eventId })
        assertEquals(listOf(false, false, true), all.map { it.isOngoing })
        assertEquals(listOf(60.minutes, 60.minutes, 30.minutes), all.map { it.duration })

        assertEquals(listOf(1L, 3L), eventItems(accounts, segments, range, calendarId = work.id).map { it.eventId })
    }

    @Test
    fun eventsOfInactiveCalendarsAreLeftOut() {
        val range = ResolvedTimeRange(at("00:00"), at("15:00"))
        val other = Calendar(id = 99, name = "Other", color = 0)

        assertEquals(emptyList<EventItem>(), eventItems(accounts, listOf(segment(other, 1, at("09:00"), at("10:00"))), range, null))
    }
}
