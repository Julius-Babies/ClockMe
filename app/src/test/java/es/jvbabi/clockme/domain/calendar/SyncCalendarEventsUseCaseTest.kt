package es.jvbabi.clockme.domain.calendar

import es.jvbabi.clockme.data.calendar.Calendar
import es.jvbabi.clockme.data.calendar.CalendarAccount
import es.jvbabi.clockme.data.calendar.DailyCalendarMinutes
import es.jvbabi.clockme.data.calendar.EventInstance
import es.jvbabi.clockme.data.keyvalue.KeyValueKey
import es.jvbabi.clockme.data.keyvalue.getInstant
import es.jvbabi.clockme.testing.FakeCalendarRepository
import es.jvbabi.clockme.testing.FakeKeyValueRepository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Clock
import kotlin.time.Instant

class SyncCalendarEventsUseCaseTest {

    private val berlin = TimeZone.of("Europe/Berlin")
    private val now = at("2026-11-12", "12:00")
    private val clock = object : Clock {
        override fun now(): Instant = now
    }

    private val calendarRepository = FakeCalendarRepository()
    private val keyValueRepository = FakeKeyValueRepository()
    private val syncCalendarEvents = SyncCalendarEventsUseCase(
        calendarRepository,
        keyValueRepository,
        clock = clock,
        timeZone = { berlin },
    )

    private fun at(date: String, time: String): Instant =
        LocalDateTime.parse("${date}T$time").toInstant(berlin)

    private suspend fun activate(vararg calendarIds: Long) {
        val account = CalendarAccount(id = "LOCAL/Device", name = "Device", calendars = emptyList())
        calendarIds.forEach {
            calendarRepository.setCalendarActive(account, Calendar(id = it, name = "Calendar $it", color = 0), active = true)
        }
    }

    @Test
    fun storesSegmentsAndMinutesPerActiveCalendar() = runTest {
        activate(10, 20)
        calendarRepository.eventInstances = mapOf(
            10L to listOf(
                EventInstance(eventId = 1, calendarId = 10, start = at("2026-11-12", "23:00"), end = at("2026-11-13", "01:00")),
                EventInstance(eventId = 2, calendarId = 10, start = at("2026-11-12", "23:30"), end = at("2026-11-13", "00:30")),
            ),
            20L to listOf(
                EventInstance(eventId = 3, calendarId = 20, start = at("2026-11-12", "09:00"), end = at("2026-11-12", "09:45")),
            ),
        )

        assertTrue(syncCalendarEvents())

        // both events cross midnight
        assertEquals(4, calendarRepository.storedSegments.value[10]!!.size)
        assertEquals(
            listOf(
                DailyCalendarMinutes(calendarId = 10, date = LocalDate.parse("2026-11-12"), minutes = 60),
                DailyCalendarMinutes(calendarId = 10, date = LocalDate.parse("2026-11-13"), minutes = 60),
            ),
            calendarRepository.storedDailyMinutes.value[10],
        )
        assertEquals(
            listOf(DailyCalendarMinutes(calendarId = 20, date = LocalDate.parse("2026-11-12"), minutes = 45)),
            calendarRepository.storedDailyMinutes.value[20],
        )
    }

    @Test
    fun syncsTheWindowAroundToday() = runTest {
        activate(10)

        syncCalendarEvents()

        val from = LocalDate.parse("2025-11-12")
        val to = LocalDate.parse("2027-02-10")
        assertEquals(listOf(Triple(10L, from, to)), calendarRepository.replacedRanges)
        assertEquals(
            listOf(at("2025-11-12", "00:00") to at("2027-02-11", "00:00")),
            calendarRepository.instanceRequests,
        )
    }

    @Test
    fun dropsSegmentsOutsideTheWindow() = runTest {
        activate(10)
        calendarRepository.eventInstances = mapOf(
            // starts the day before the window
            10L to listOf(EventInstance(eventId = 1, calendarId = 10, start = at("2025-11-11", "23:00"), end = at("2025-11-12", "01:00"))),
        )

        syncCalendarEvents()

        assertEquals(listOf(LocalDate.parse("2025-11-12")), calendarRepository.storedSegments.value[10]!!.map { it.date })
    }

    @Test
    fun storesTimeOfLastSync() = runTest {
        syncCalendarEvents()

        assertEquals(now, keyValueRepository.getInstant(KeyValueKey.LAST_EVENT_SYNC))
    }

    @Test
    fun changesNothingIfCalendarsCantBeRead() = runTest {
        activate(10)
        calendarRepository.eventInstances = null

        assertFalse(syncCalendarEvents())

        assertEquals(emptyList<Any>(), calendarRepository.replacedRanges)
        assertNull(keyValueRepository.getInstant(KeyValueKey.LAST_EVENT_SYNC))
    }
}
