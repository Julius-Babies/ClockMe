package es.jvbabi.clockme.data.calendar.local

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

@Dao
interface CalendarEventDao {

    @Query("SELECT * FROM event_segment ORDER BY start_time")
    fun observeSegments(): Flow<List<EventSegmentEntity>>

    @Query("SELECT * FROM event_segment WHERE date BETWEEN :from AND :to ORDER BY start_time")
    fun observeSegments(from: LocalDate, to: LocalDate): Flow<List<EventSegmentEntity>>

    @Query("SELECT * FROM daily_calendar_minutes ORDER BY date, calendar_id")
    fun observeDailyMinutes(): Flow<List<DailyCalendarMinutesEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM active_calendar WHERE id = :calendarId)")
    suspend fun isActive(calendarId: Long): Boolean

    @Query("DELETE FROM event_segment WHERE calendar_id = :calendarId AND date BETWEEN :from AND :to")
    suspend fun deleteSegments(calendarId: Long, from: LocalDate, to: LocalDate)

    @Query("DELETE FROM daily_calendar_minutes WHERE calendar_id = :calendarId AND date BETWEEN :from AND :to")
    suspend fun deleteDailyMinutes(calendarId: Long, from: LocalDate, to: LocalDate)

    // Upsert: the provider may report an occurrence twice
    @Upsert
    suspend fun insertSegments(segments: List<EventSegmentEntity>)

    @Upsert
    suspend fun insertDailyMinutes(minutes: List<DailyCalendarMinutesEntity>)

    /**
     * Replaces all event data of [calendarId] between [from] and [to] (inclusive).
     * Skipped if the calendar is no longer active, so a deactivated calendar gets no data back.
     */
    @Transaction
    suspend fun replace(
        calendarId: Long,
        from: LocalDate,
        to: LocalDate,
        segments: List<EventSegmentEntity>,
        dailyMinutes: List<DailyCalendarMinutesEntity>,
    ) {
        if (!isActive(calendarId)) return
        deleteSegments(calendarId, from, to)
        deleteDailyMinutes(calendarId, from, to)
        insertSegments(segments)
        insertDailyMinutes(dailyMinutes)
    }
}
