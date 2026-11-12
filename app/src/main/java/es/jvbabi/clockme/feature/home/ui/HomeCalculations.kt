package es.jvbabi.clockme.feature.home.ui

import es.jvbabi.clockme.data.calendar.CalendarAccount
import es.jvbabi.clockme.data.calendar.EventSegment
import es.jvbabi.clockme.domain.calendar.clipTo
import es.jvbabi.clockme.domain.calendar.coveredDuration
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** Missing bounds follow the clock, and the end never lies after [now]. */
fun TimeRangeFilter.resolve(now: Instant, timeZone: TimeZone): ResolvedTimeRange {
    val from = from ?: now.toLocalDateTime(timeZone).date.atStartOfDayIn(timeZone)
    return ResolvedTimeRange(from = from, to = maxOf(from, minOf(to ?: now, now)))
}

/** The whole [date], from 00:00 until 00:00 of the next day. */
fun TimeRangeFilter.Companion.ofDay(date: LocalDate, timeZone: TimeZone) = TimeRangeFilter(
    from = date.atStartOfDayIn(timeZone),
    to = date.plus(1, DateTimeUnit.DAY).atStartOfDayIn(timeZone),
)

/** Time per active calendar within [range], in the order of [activeAccounts]. */
fun calendarTimes(
    activeAccounts: List<CalendarAccount>,
    segments: List<EventSegment>,
    range: ResolvedTimeRange,
): List<CalendarTime> {
    val clippedByCalendar = segments
        .mapNotNull { it.clipTo(range.from, range.to) }
        .groupBy { it.calendarId }
    return activeAccounts.flatMap { it.calendars }.map { calendar ->
        CalendarTime(
            calendarId = calendar.id,
            name = calendar.name,
            color = calendar.color,
            duration = clippedByCalendar[calendar.id].orEmpty().coveredDuration(),
        )
    }
}

/** Events within [range] in chronological order, like a calendar; only of [calendarId] unless it is `null`. */
fun eventItems(
    activeAccounts: List<CalendarAccount>,
    segments: List<EventSegment>,
    range: ResolvedTimeRange,
    calendarId: Long?,
): List<EventItem> {
    val colors = activeAccounts.flatMap { it.calendars }.associate { it.id to it.color }
    return segments
        .filter { calendarId == null || it.calendarId == calendarId }
        .mapNotNull { segment ->
            val color = colors[segment.calendarId] ?: return@mapNotNull null
            val clipped = segment.clipTo(range.from, range.to) ?: return@mapNotNull null
            EventItem(
                eventId = segment.eventId,
                calendarId = segment.calendarId,
                date = segment.date,
                title = segment.title,
                color = color,
                start = segment.start,
                end = segment.end,
                duration = clipped.end - clipped.start,
                isOngoing = segment.end > range.to,
            )
        }
        .sortedWith(compareBy({ it.start }, { it.end }))
}
