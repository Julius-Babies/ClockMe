package es.jvbabi.clockme.feature.home.ui

import androidx.annotation.ColorInt
import kotlinx.datetime.LocalDate
import kotlin.time.Duration
import kotlin.time.Instant

data class HomeUiState(
    val shortcuts: List<RangeShortcut> = emptyList(),
    /** `null` if a custom [filter] is used. */
    val selectedShortcut: RangeShortcut? = RangeShortcut.TODAY,
    val filter: TimeRangeFilter = TimeRangeFilter(),
    /** The [filter] resolved against the current time, `null` while loading. */
    val range: ResolvedTimeRange? = null,
    /**
     * Time per active calendar within [range], `null` while loading. Calendars of a group are
     * next to each other: groups first, then calendars without a group.
     */
    val calendars: List<CalendarTime>? = null,
    /** Groups of the [calendars], in the same order. */
    val groups: List<GroupInfo> = emptyList(),
    val selectedCalendarId: Long? = null,
    /** Events within [range], only of the selected calendar if there is one. */
    val events: List<EventItem> = emptyList(),
    /** When the calendar events were last synced, `null` if never. */
    val lastEventSync: Instant? = null,
    /** `null` while unknown. Without permission home navigates to the onboarding. */
    val hasCalendarPermission: Boolean? = null,
) {
    /** Loaded and staying on home, so the splash screen can go. */
    val isReady: Boolean get() = calendars != null && hasCalendarPermission == true

    val selectedCalendar: CalendarTime? get() = calendars?.find { it.calendarId == selectedCalendarId }

    /** Sum of all calendars, so it matches the ring. */
    val totalDuration: Duration get() = calendars.orEmpty().fold(Duration.ZERO) { sum, it -> sum + it.duration }
}

/**
 * Which time range home shows. A `null` bound follows the clock:
 * [from] defaults to the start of today, [to] to now. The end is always capped at now.
 */
data class TimeRangeFilter(
    val from: Instant? = null,
    val to: Instant? = null,
) {
    companion object
}

data class ResolvedTimeRange(val from: Instant, val to: Instant)

data class CalendarTime(
    val calendarId: Long,
    val name: String,
    @param:ColorInt val color: Int,
    /** Time covered by events of the calendar, overlapping events count once. */
    val duration: Duration,
    val groupId: Long? = null,
)

data class GroupInfo(
    val id: Long,
    val name: String,
    @param:ColorInt val color: Int?,
)

data class EventItem(
    val eventId: Long,
    val calendarId: Long,
    /** The day this part of the event lies on; events across midnight have one item per day. */
    val date: LocalDate,
    val title: String,
    @param:ColorInt val color: Int,
    val start: Instant,
    val end: Instant,
    /** Part of the event within the range. */
    val duration: Duration,
    /** Still running at the end of the range. */
    val isOngoing: Boolean,
)
