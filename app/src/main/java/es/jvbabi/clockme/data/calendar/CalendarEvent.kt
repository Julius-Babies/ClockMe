package es.jvbabi.clockme.data.calendar

import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/** One occurrence of an event in the device calendar (a recurring event has many). */
data class EventInstance(
    val eventId: Long,
    val calendarId: Long,
    val start: Instant,
    val end: Instant,
    val title: String = "",
)

/** The part of an [EventInstance] that falls on one [date]. Events across midnight have several. */
data class EventSegment(
    val calendarId: Long,
    val eventId: Long,
    val date: LocalDate,
    val start: Instant,
    val end: Instant,
    val title: String = "",
)

/** Minutes covered by events of a calendar on a [date]; overlapping events count once. */
data class DailyCalendarMinutes(
    val calendarId: Long,
    val date: LocalDate,
    val minutes: Int,
)
