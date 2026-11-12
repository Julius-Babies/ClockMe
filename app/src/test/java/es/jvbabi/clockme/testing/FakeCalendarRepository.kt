package es.jvbabi.clockme.testing

import es.jvbabi.clockme.data.calendar.Calendar
import es.jvbabi.clockme.data.calendar.CalendarAccount
import es.jvbabi.clockme.data.calendar.CalendarRepository
import es.jvbabi.clockme.data.calendar.DailyCalendarMinutes
import es.jvbabi.clockme.data.calendar.EventInstance
import es.jvbabi.clockme.data.calendar.EventSegment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

class FakeCalendarRepository(
    initialAccounts: List<CalendarAccount> = emptyList(),
) : CalendarRepository {

    val accounts = MutableStateFlow(initialAccounts)
    val activeCalendars = MutableStateFlow<List<CalendarAccount>>(emptyList())

    /** Instances on the device per calendar id; `null` simulates an unreadable provider. */
    var eventInstances: Map<Long, List<EventInstance>>? = emptyMap()

    /** Requested windows of [getEventInstances]. */
    val instanceRequests = mutableListOf<Pair<Instant, Instant>>()

    val providerChanges = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Stored event data per calendar id, as passed to [replaceEventData]. */
    val storedSegments = MutableStateFlow<Map<Long, List<EventSegment>>>(emptyMap())
    val storedDailyMinutes = MutableStateFlow<Map<Long, List<DailyCalendarMinutes>>>(emptyMap())
    val replacedRanges = mutableListOf<Triple<Long, LocalDate, LocalDate>>()

    var syncCount = 0
        private set

    override fun observeAccounts(): Flow<List<CalendarAccount>> = accounts

    override fun observeActiveCalendars(): Flow<List<CalendarAccount>> = activeCalendars

    override suspend fun setCalendarActive(account: CalendarAccount, calendar: Calendar, active: Boolean) {
        activeCalendars.update { current ->
            val others = current.filter { it.id != account.id }
            val calendars = current.find { it.id == account.id }?.calendars.orEmpty()
                .filter { it.id != calendar.id }
                .let { if (active) it + calendar else it }
            if (calendars.isEmpty()) others else others + account.copy(calendars = calendars)
        }
    }

    override suspend fun syncActiveCalendars() {
        syncCount++
    }

    override suspend fun getActiveCalendarIds(): List<Long> =
        activeCalendars.value.flatMap { it.calendars }.map { it.id }

    override suspend fun getEventInstances(calendarId: Long, from: Instant, to: Instant): List<EventInstance>? {
        instanceRequests += from to to
        return eventInstances?.let { it[calendarId].orEmpty() }
    }

    override fun observeCalendarProviderChanges(): Flow<Unit> = providerChanges

    override suspend fun replaceEventData(
        calendarId: Long,
        from: LocalDate,
        to: LocalDate,
        segments: List<EventSegment>,
        dailyMinutes: List<DailyCalendarMinutes>,
    ) {
        replacedRanges += Triple(calendarId, from, to)
        storedSegments.update { it + (calendarId to segments) }
        storedDailyMinutes.update { it + (calendarId to dailyMinutes) }
    }

    override fun observeEventSegments(): Flow<List<EventSegment>> =
        storedSegments.map { it.values.flatten() }

    override fun observeEventSegments(from: LocalDate, to: LocalDate): Flow<List<EventSegment>> =
        storedSegments.map { stored -> stored.values.flatten().filter { it.date in from..to } }

    override fun observeDailyMinutes(): Flow<List<DailyCalendarMinutes>> =
        MutableStateFlow(storedDailyMinutes.value.values.flatten())
}
