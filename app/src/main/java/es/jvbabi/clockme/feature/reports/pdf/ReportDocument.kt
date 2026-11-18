package es.jvbabi.clockme.feature.reports.pdf

import androidx.annotation.ColorInt
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.time.Duration

/** Everything a report shows, already cut to its span and in the device's time zone. */
data class ReportDocument(
    val from: LocalDateTime,
    val to: LocalDateTime,
    val createdAt: LocalDateTime,
    /** The calendars with time in the span, grouped ones first, each group's together. */
    val calendars: List<ReportCalendar>,
    /** The groups of [calendars], for the bands around the ring. */
    val groups: List<ReportCalendarGroup>,
    /** `null` if the report leaves out the single events. */
    val events: List<ReportEvent>?,
) {
    val total: Duration get() = calendars.fold(Duration.ZERO) { sum, it -> sum + it.duration }
}

data class ReportCalendar(
    val id: Long,
    val name: String,
    @param:ColorInt val color: Int,
    val duration: Duration,
    val groupId: Long? = null,
)

data class ReportCalendarGroup(
    val id: Long,
    val name: String,
    @param:ColorInt val color: Int?,
)

/** The part of an event on one day within the span; an event across midnight has one per day. */
data class ReportEvent(
    val date: LocalDate,
    val start: LocalDateTime,
    val end: LocalDateTime,
    val title: String,
    @param:ColorInt val color: Int,
    val calendarName: String,
    /** Real time between [start] and [end], right across a change of daylight saving time. */
    val duration: Duration,
)
