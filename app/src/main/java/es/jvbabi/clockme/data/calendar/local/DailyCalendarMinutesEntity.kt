package es.jvbabi.clockme.data.calendar.local

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import kotlinx.datetime.LocalDate

/** Minutes covered by events of a calendar on a day, overlapping events counted once. */
@Entity(
    tableName = "daily_calendar_minutes",
    primaryKeys = ["calendar_id", "date"],
)
data class DailyCalendarMinutesEntity(
    @ColumnInfo(name = "calendar_id") val calendarId: Long,
    val date: LocalDate,
    val minutes: Int,
)
