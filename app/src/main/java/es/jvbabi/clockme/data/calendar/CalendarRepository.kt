package es.jvbabi.clockme.data.calendar

import kotlinx.coroutines.flow.Flow

interface CalendarRepository {

    /**
     * All accounts with their calendars. Updates when calendars change on the device
     * or the permission changes. Emits an empty list while READ_CALENDAR is not granted.
     */
    fun observeAccounts(): Flow<List<CalendarAccount>>
}
