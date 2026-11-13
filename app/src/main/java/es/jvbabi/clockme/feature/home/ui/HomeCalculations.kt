package es.jvbabi.clockme.feature.home.ui

import es.jvbabi.clockme.data.calendar.CalendarAccount
import es.jvbabi.clockme.data.calendar.EventSegment
import es.jvbabi.clockme.data.group.CalendarGroup
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

/**
 * Time per active calendar within [range]. Calendars of a group are kept together: the groups in
 * their order first, then the calendars without group, each in the order of [activeAccounts].
 */
fun calendarTimes(
    activeAccounts: List<CalendarAccount>,
    segments: List<EventSegment>,
    range: ResolvedTimeRange,
    groups: List<CalendarGroup> = emptyList(),
): List<CalendarTime> {
    val clippedByCalendar = segments
        .mapNotNull { it.clipTo(range.from, range.to) }
        .groupBy { it.calendarId }
    val groupOf = groups.flatMap { group -> group.calendarIds.map { it to group.id } }.toMap()
    val groupOrder = groups.mapIndexed { index, group -> group.id to index }.toMap()
    return activeAccounts.flatMap { it.calendars }
        .map { calendar ->
            CalendarTime(
                calendarId = calendar.id,
                name = calendar.name,
                color = calendar.color,
                duration = clippedByCalendar[calendar.id].orEmpty().coveredDuration(),
                groupId = groupOf[calendar.id],
            )
        }
        // Stable: within a group and without group the account order stays
        .sortedBy { calendar -> calendar.groupId?.let { groupOrder[it] } ?: Int.MAX_VALUE }
}

/** The groups that contain at least one of the [calendars], in their order. */
fun groupInfos(groups: List<CalendarGroup>, calendars: List<CalendarTime>): List<GroupInfo> {
    val usedGroupIds = calendars.mapNotNull { it.groupId }.toSet()
    return groups.filter { it.id in usedGroupIds }.map { GroupInfo(it.id, it.name, it.color) }
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
