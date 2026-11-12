package es.jvbabi.clockme.sync

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import es.jvbabi.clockme.MainActivity
import es.jvbabi.clockme.data.calendar.Calendar
import es.jvbabi.clockme.data.calendar.CalendarAccount
import es.jvbabi.clockme.data.calendar.CalendarRepository
import es.jvbabi.clockme.data.calendar.DailyCalendarMinutes
import es.jvbabi.clockme.data.keyvalue.KeyValueKey
import es.jvbabi.clockme.data.keyvalue.KeyValueRepository
import es.jvbabi.clockme.data.keyvalue.getInstant
import es.jvbabi.clockme.domain.calendar.SyncCalendarEventsUseCase
import es.jvbabi.clockme.testing.CalendarProviderFixture
import es.jvbabi.clockme.testing.CalendarProviderFixture.Companion.ACCOUNT_A
import es.jvbabi.clockme.testing.CalendarProviderFixture.Companion.ACCOUNT_TYPE
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.todayIn
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.workmanager.factory.KoinWorkerFactory
import org.koin.core.context.GlobalContext
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Tests reading events from the real calendar provider and storing them, triggered directly,
 * by the in-app observer and by the background worker.
 */
@RunWith(AndroidJUnit4::class)
class CalendarEventSyncTest {

    private val fixture = CalendarProviderFixture()
    private val koin get() = GlobalContext.get()
    private val calendarRepository: CalendarRepository by lazy { koin.get() }
    private val keyValueRepository: KeyValueRepository by lazy { koin.get() }
    private val syncCalendarEvents: SyncCalendarEventsUseCase by lazy { koin.get() }

    private val timeZone = TimeZone.currentSystemDefault()
    private val today = Clock.System.todayIn(timeZone)
    private val yesterday = today.minus(1, DateTimeUnit.DAY)

    private var calendarId = 0L
    private val account = CalendarAccount(id = "$ACCOUNT_TYPE/$ACCOUNT_A", name = ACCOUNT_A, calendars = emptyList())

    @Before
    fun setUp() {
        fixture.deleteAll()
        fixture.grantPermission()
        calendarId = fixture.insertCalendar(ACCOUNT_A, "Work", RED)
    }

    @After
    fun tearDown() = fixture.deleteAll()

    @Test
    fun readsTimedEventsWithRecurringOccurrences() = runBlocking {
        val timed = insertEvent("Timed", at(yesterday, 10, 0), at(yesterday, 11, 0))
        val recurring = insertEvent(
            "Recurring",
            at(today.minus(5, DateTimeUnit.DAY), 8, 0),
            at(today.minus(5, DateTimeUnit.DAY), 8, 30),
            recurrenceRule = "FREQ=DAILY;COUNT=3",
        )
        fixture.insertEvent(
            ACCOUNT_A, calendarId, "AllDay",
            start = yesterday.atStartOfDayUtc(), end = today.atStartOfDayUtc(), allDay = true,
        )
        insertEvent("Canceled", at(yesterday, 12, 0), at(yesterday, 13, 0), canceled = true)

        val instances = calendarRepository.getEventInstances(
            calendarId,
            at(today.minus(10, DateTimeUnit.DAY), 0, 0),
            at(today.plus(1, DateTimeUnit.DAY), 0, 0),
        )

        assertEquals(
            listOf(recurring, recurring, recurring, timed),
            instances.orEmpty().sortedBy { it.start }.map { it.eventId },
        )
    }

    @Test
    fun syncStoresEventsSplitByDayWithMinutesPerDay() = runBlocking {
        activateCalendar()
        // crosses midnight, and overlaps with the second one
        insertEvent("Night", at(yesterday, 22, 0), at(today, 1, 0))
        insertEvent("Overlap", at(yesterday, 22, 30), at(yesterday, 23, 30))

        assertTrue(syncCalendarEvents())

        assertEquals(3, calendarRepository.observeEventSegments().first().size)
        assertEquals(
            listOf(
                DailyCalendarMinutes(calendarId, yesterday, minutes = 120),
                DailyCalendarMinutes(calendarId, today, minutes = 60),
            ),
            calendarRepository.observeDailyMinutes().first(),
        )
        assertNotNull(keyValueRepository.getInstant(KeyValueKey.LAST_EVENT_SYNC))
    }

    @Test
    fun deactivatingCalendarRemovesItsEventData() = runBlocking {
        val calendar = activateCalendar()
        insertEvent("Meeting", at(yesterday, 10, 0), at(yesterday, 11, 0))
        syncCalendarEvents()

        calendarRepository.setCalendarActive(account, calendar, active = false)

        assertEquals(emptyList<Any>(), calendarRepository.observeEventSegments().first())
        assertEquals(emptyList<Any>(), calendarRepository.observeDailyMinutes().first())
    }

    @Test
    fun observerSyncsAutomaticallyWhileAppIsOpen() = runBlocking {
        // Resuming the activity makes the app notice the granted permission
        ActivityScenario.launch(MainActivity::class.java).use {
            activateCalendar()

            val eventId = insertEvent("Meeting", at(yesterday, 10, 0), at(yesterday, 10, 45))

            val segments = withTimeoutOrNull(TIMEOUT_MS) {
                calendarRepository.observeEventSegments().first { segments -> segments.any { it.eventId == eventId } }
            }
            assertNotNull("Event was not synced automatically", segments)
            assertEquals(
                listOf(DailyCalendarMinutes(calendarId, yesterday, minutes = 45)),
                withTimeoutOrNull(TIMEOUT_MS) { calendarRepository.observeDailyMinutes().first { it.isNotEmpty() } },
            )
        }
    }

    @Test
    fun workerSyncsEvents() = runBlocking {
        activateCalendar()
        val eventId = insertEvent("Meeting", at(yesterday, 10, 0), at(yesterday, 11, 0))

        val worker = TestListenableWorkerBuilder<SyncCalendarEventsWorker>(
            InstrumentationRegistry.getInstrumentation().targetContext,
        ).setWorkerFactory(KoinWorkerFactory()).build()

        assertEquals(ListenableWorker.Result.success(), worker.doWork())
        assertTrue(calendarRepository.observeEventSegments().first().any { it.eventId == eventId })
    }

    private suspend fun activateCalendar(): Calendar {
        val calendar = Calendar(id = calendarId, name = "Work", color = RED)
        calendarRepository.setCalendarActive(account, calendar, active = true)
        return calendar
    }

    private fun insertEvent(
        title: String,
        start: Instant,
        end: Instant,
        canceled: Boolean = false,
        recurrenceRule: String? = null,
    ) = fixture.insertEvent(ACCOUNT_A, calendarId, title, start, end, canceled = canceled, recurrenceRule = recurrenceRule)

    private fun at(date: LocalDate, hour: Int, minute: Int): Instant =
        date.atTime(LocalTime(hour, minute)).toInstant(timeZone)

    private fun LocalDate.atStartOfDayUtc(): Instant = atTime(LocalTime(0, 0)).toInstant(TimeZone.UTC)

    private companion object {
        const val TIMEOUT_MS = 10_000L
        const val RED = 0xFFFF0000.toInt()
    }
}
