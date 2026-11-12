package es.jvbabi.clockme.domain.calendar

import es.jvbabi.clockme.data.calendar.DailyCalendarMinutes
import es.jvbabi.clockme.data.calendar.EventInstance
import es.jvbabi.clockme.data.calendar.EventSegment
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration
import kotlin.time.Instant

/**
 * Splits the event at every local midnight in [timeZone], so each segment lies on one day.
 * Events without duration have no segments.
 */
fun EventInstance.splitIntoDays(timeZone: TimeZone): List<EventSegment> {
    if (end <= start) return emptyList()
    return buildList {
        var date = start.toLocalDateTime(timeZone).date
        var segmentStart = start
        while (segmentStart < end) {
            val nextDay = date.plus(1, DateTimeUnit.DAY)
            val segmentEnd = minOf(end, nextDay.atStartOfDayIn(timeZone))
            add(EventSegment(calendarId, eventId, date, segmentStart, segmentEnd))
            segmentStart = segmentEnd
            date = nextDay
        }
    }
}

/**
 * Sums up the time covered per calendar and day. Overlapping segments of the same calendar
 * count only once; partial minutes are cut off.
 */
fun List<EventSegment>.minutesPerCalendarAndDay(): List<DailyCalendarMinutes> =
    groupBy { it.calendarId to it.date }
        .map { (key, segments) ->
            val (calendarId, date) = key
            DailyCalendarMinutes(
                calendarId = calendarId,
                date = date,
                minutes = segments.coveredDuration().inWholeMinutes.toInt(),
            )
        }
        .sortedWith(compareBy({ it.date }, { it.calendarId }))

/** Length of the union of all segments. */
private fun List<EventSegment>.coveredDuration(): Duration {
    var total = Duration.ZERO
    var currentStart: Instant? = null
    var currentEnd: Instant? = null
    for (segment in sortedBy { it.start }) {
        if (currentEnd == null || segment.start > currentEnd) {
            if (currentStart != null) total += currentEnd!! - currentStart
            currentStart = segment.start
            currentEnd = segment.end
        } else if (segment.end > currentEnd) {
            currentEnd = segment.end
        }
    }
    if (currentStart != null) total += currentEnd!! - currentStart
    return total
}
