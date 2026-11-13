package es.jvbabi.clockme.data.calendar

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

interface CalendarRepository {

    /**
     * All accounts with their calendars on the device. Updates when calendars change on the
     * device or the permission changes. Emits an empty list while READ_CALENDAR is not granted.
     */
    fun observeAccounts(): Flow<List<CalendarAccount>>

    /** The calendars activated in the app, grouped by their account. */
    fun observeActiveCalendars(): Flow<List<CalendarAccount>>

    /** Activates or deactivates [calendar]. Its [account] is stored along with it. */
    suspend fun setCalendarActive(account: CalendarAccount, calendar: Calendar, active: Boolean)

    /**
     * Removes active calendars that no longer exist on the device (and accounts left without
     * calendars), and updates name and color of the others. Calendars that no longer exist are
     * also removed from all groups. Does nothing without permission.
     */
    suspend fun syncActiveCalendars()

    /** Ids of the calendars activated in the app. */
    suspend fun getActiveCalendarIds(): List<Long>

    /**
     * Occurrences of timed events of [calendarId] that overlap [from] until [to], with recurring
     * events expanded. All-day, canceled and declined events are left out.
     * `null` if the calendar provider can't be read (e.g. missing permission).
     */
    suspend fun getEventInstances(calendarId: Long, from: Instant, to: Instant): List<EventInstance>?

    /**
     * Emits whenever anything in the device calendars changes, while READ_CALENDAR is granted.
     * Emits once right away.
     */
    fun observeCalendarProviderChanges(): Flow<Unit>

    /**
     * Replaces the stored event data of [calendarId] between [from] and [to] (inclusive).
     * Ignored if the calendar is not active anymore.
     */
    suspend fun replaceEventData(
        calendarId: Long,
        from: LocalDate,
        to: LocalDate,
        segments: List<EventSegment>,
        dailyMinutes: List<DailyCalendarMinutes>,
    )

    /** Stored event segments of all active calendars. */
    fun observeEventSegments(): Flow<List<EventSegment>>

    /** Stored event segments of all active calendars on the days [from] until [to] (inclusive). */
    fun observeEventSegments(from: LocalDate, to: LocalDate): Flow<List<EventSegment>>

    /** Stored minutes per calendar and day of all active calendars. */
    fun observeDailyMinutes(): Flow<List<DailyCalendarMinutes>>
}
