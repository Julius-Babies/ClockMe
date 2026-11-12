package es.jvbabi.clockme.data.calendar.local

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/**
 * The part of an event occurrence that falls on one day. A recurring event has one row per
 * occurrence and day, so the key is the event together with the start of the segment.
 */
@Entity(
    tableName = "event_segment",
    primaryKeys = ["event_id", "start_time"],
    indices = [Index("calendar_id", "date")],
)
data class EventSegmentEntity(
    @ColumnInfo(name = "calendar_id") val calendarId: Long,
    @ColumnInfo(name = "event_id") val eventId: Long,
    val date: LocalDate,
    @ColumnInfo(name = "start_time") val start: Instant,
    @ColumnInfo(name = "end_time") val end: Instant,
    val title: String,
)
