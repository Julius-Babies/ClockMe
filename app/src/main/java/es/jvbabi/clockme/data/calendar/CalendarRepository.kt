package es.jvbabi.clockme.data.calendar

import kotlinx.coroutines.flow.Flow

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
     * calendars), and updates name and color of the others. Does nothing without permission.
     */
    suspend fun syncActiveCalendars()
}
