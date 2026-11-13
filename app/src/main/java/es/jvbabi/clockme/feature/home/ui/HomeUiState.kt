package es.jvbabi.clockme.feature.home.ui

import androidx.annotation.ColorInt
import kotlinx.datetime.LocalDate
import kotlin.time.Duration
import kotlin.time.Instant

data class HomeUiState(
    val shortcuts: List<RangeShortcut> = emptyList(),
    /** `null` if a custom [filter] is used. */
    val selectedShortcut: RangeShortcut? = RangeShortcut.TODAY,
    /** The range of the current page. */
    val filter: TimeRangeFilter = TimeRangeFilter(),
    /** `null` while loading. */
    val today: LocalDate? = null,
    /** What home pages through, `null` for a single page (all time or a custom filter). */
    val unit: PageUnit? = null,
    /** The current page: 0 is the current unit, -1 the one before and so on. */
    val pageOffset: Int = 0,
    /** The loaded pages by offset: the current one and its neighbours. */
    val pages: Map<Int, HomePage> = emptyMap(),
    /** `null` while loading. */
    val hasActiveCalendars: Boolean? = null,
    /** Groups of the active calendars, in the order of the calendars in a page. */
    val groups: List<GroupInfo> = emptyList(),
    /** Either a calendar or a group is selected, never both. */
    val selectedCalendarId: Long? = null,
    val selectedGroupId: Long? = null,
    /** When the calendar events were last synced, `null` if never. */
    val lastEventSync: Instant? = null,
    /** `null` while unknown. Without permission home navigates to the onboarding. */
    val hasCalendarPermission: Boolean? = null,
) {
    val currentPage: HomePage? get() = pages[pageOffset]

    /** Loaded and staying on home, so the splash screen can go. */
    val isReady: Boolean
        get() = hasCalendarPermission == true && (hasActiveCalendars == false || currentPage != null)

    /** The range of the current page, `null` while loading. */
    val range: ResolvedTimeRange? get() = currentPage?.range

    /** Time per active calendar on the current page, `null` while loading. */
    val calendars: List<CalendarTime>? get() = currentPage?.calendars

    /** Events of the current page. */
    val events: List<EventItem> get() = currentPage?.events.orEmpty()

    val selectedCalendar: CalendarTime? get() = currentPage?.calendar(selectedCalendarId)

    val selectedGroup: GroupInfo? get() = groups.find { it.id == selectedGroupId }

    val hasSelection: Boolean get() = selectedCalendarId != null || selectedGroupId != null

    val totalDuration: Duration get() = currentPage?.totalDuration ?: Duration.ZERO

    fun groupDuration(groupId: Long): Duration = currentPage?.groupDuration(groupId) ?: Duration.ZERO
}

/** One page of home: the time per calendar and the events within one unit (or the whole range). */
data class HomePage(
    val offset: Int,
    /** First day of the page's unit, `null` for a page without unit. */
    val firstDay: LocalDate?,
    val range: ResolvedTimeRange,
    /**
     * Time per active calendar within [range]. Calendars of a group are next to each other:
     * groups first, then calendars without a group.
     */
    val calendars: List<CalendarTime>,
    /** Events within [range], only of the selected calendar or group if there is one. */
    val events: List<EventItem>,
) {
    fun calendar(id: Long?): CalendarTime? = id?.let { calendars.find { it.calendarId == id } }

    /** Sum of all calendars, so it matches the ring. */
    val totalDuration: Duration get() = calendars.fold(Duration.ZERO) { sum, it -> sum + it.duration }

    /** Sum of the calendars in the group. */
    fun groupDuration(groupId: Long): Duration =
        calendars.filter { it.groupId == groupId }.fold(Duration.ZERO) { sum, it -> sum + it.duration }
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
