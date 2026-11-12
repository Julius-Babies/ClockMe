package es.jvbabi.clockme.data.calendar.local

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ActiveCalendarDao {

    @Transaction
    @Query("SELECT * FROM calendar_account ORDER BY name")
    fun observeAccountsWithCalendars(): Flow<List<AccountWithActiveCalendars>>

    @Query("SELECT * FROM active_calendar")
    suspend fun getCalendars(): List<ActiveCalendarEntity>

    @Upsert
    suspend fun upsertAccount(account: CalendarAccountEntity)

    @Upsert
    suspend fun upsertCalendar(calendar: ActiveCalendarEntity)

    @Query("DELETE FROM active_calendar WHERE id = :id")
    suspend fun deleteCalendar(id: Long)

    @Query("DELETE FROM calendar_account WHERE id NOT IN (SELECT DISTINCT account_id FROM active_calendar)")
    suspend fun deleteEmptyAccounts()

    /** Event data is only kept for active calendars. */
    @Query("DELETE FROM event_segment WHERE calendar_id NOT IN (SELECT id FROM active_calendar)")
    suspend fun deleteSegmentsOfInactiveCalendars()

    @Query("DELETE FROM daily_calendar_minutes WHERE calendar_id NOT IN (SELECT id FROM active_calendar)")
    suspend fun deleteDailyMinutesOfInactiveCalendars()

    @Query("SELECT id FROM active_calendar")
    suspend fun getCalendarIds(): List<Long>

    /** Stores [calendar] together with its [account]. */
    @Transaction
    suspend fun activate(account: CalendarAccountEntity, calendar: ActiveCalendarEntity) {
        upsertAccount(account)
        upsertCalendar(calendar)
    }

    /** Removes the calendar with its event data, and its account if it has no active calendars left. */
    @Transaction
    suspend fun deactivate(calendarId: Long) {
        deleteCalendar(calendarId)
        deleteEmptyAccounts()
        deleteEventDataOfInactiveCalendars()
    }

    /**
     * Aligns the stored calendars with the calendars that exist on the device:
     * updates name, color and account of existing ones, removes deleted ones and empty accounts.
     *
     * @param existing all calendars on the device, keyed by calendar id, with their account
     */
    @Transaction
    suspend fun sync(existing: Map<Long, Pair<CalendarAccountEntity, ActiveCalendarEntity>>) {
        getCalendars().forEach { stored ->
            val current = existing[stored.id]
            if (current == null) {
                deleteCalendar(stored.id)
            } else {
                val (account, calendar) = current
                upsertAccount(account)
                if (calendar != stored) upsertCalendar(calendar)
            }
        }
        deleteEmptyAccounts()
        deleteEventDataOfInactiveCalendars()
    }

    private suspend fun deleteEventDataOfInactiveCalendars() {
        deleteSegmentsOfInactiveCalendars()
        deleteDailyMinutesOfInactiveCalendars()
    }
}
