package es.jvbabi.clockme.feature.home.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.jvbabi.clockme.core.time.TimeProvider
import es.jvbabi.clockme.data.calendar.CalendarRepository
import es.jvbabi.clockme.data.calendar.EventSegment
import es.jvbabi.clockme.data.group.CalendarGroupRepository
import es.jvbabi.clockme.data.keyvalue.KeyValueKey
import es.jvbabi.clockme.data.keyvalue.KeyValueRepository
import es.jvbabi.clockme.data.keyvalue.observeInstant
import es.jvbabi.clockme.data.permission.AppPermission
import es.jvbabi.clockme.data.permission.PermissionRepository
import es.jvbabi.clockme.data.permission.PermissionStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.seconds

/**
 * Home is the root of the app and stays on the back stack while other tabs are shown,
 * so it takes care of app-wide checks: the calendar permission and syncing active calendars.
 */
class HomeViewModel(
    private val permissionRepository: PermissionRepository,
    private val calendarRepository: CalendarRepository,
    keyValueRepository: KeyValueRepository,
    private val timeProvider: TimeProvider,
    groupRepository: CalendarGroupRepository,
) : ViewModel() {

    /** What home shows: a shortcut (or a custom filter if `null`) and the page within its unit. */
    private data class Mode(
        val shortcut: RangeShortcut? = RangeShortcut.TODAY,
        val customFilter: TimeRangeFilter = TimeRangeFilter(),
        val offset: Int = 0,
    )

    private val mode = MutableStateFlow(Mode())
    private val selection = MutableStateFlow<Selection?>(null)

    private sealed interface Selection {
        data class Calendar(val id: Long) : Selection
        data class Group(val id: Long) : Selection
    }

    private data class PageRange(
        val offset: Int,
        val firstDay: LocalDate?,
        val filter: TimeRangeFilter,
        val resolved: ResolvedTimeRange,
        val days: ClosedRange<LocalDate>,
    )

    private data class Pages(val today: LocalDate, val mode: Mode, val pages: List<PageRange>) {
        val days: ClosedRange<LocalDate> get() = pages.minOf { it.days.start }..pages.maxOf { it.days.endInclusive }
    }

    /** The current page and its neighbours, so they are ready while swiping. */
    private val pages = combine(mode, timeProvider.ticks(1.seconds)) { mode, now ->
        val timeZone = timeProvider.timeZone()
        val today = now.toLocalDateTime(timeZone).date
        val unit = mode.shortcut?.unit
        val offsets = if (unit == null) listOf(0) else (mode.offset - 1..mode.offset + 1).filter { it <= 0 }
        val pages = offsets.map { offset ->
            val filter = unit?.filter(today, offset, timeZone)
                ?: mode.shortcut?.toFilter(today, timeZone)
                ?: mode.customFilter
            val resolved = filter.resolve(now, timeZone)
            PageRange(
                offset = offset,
                firstDay = unit?.firstDay(today, offset),
                filter = filter,
                resolved = resolved,
                days = resolved.from.toLocalDateTime(timeZone).date..resolved.to.toLocalDateTime(timeZone).date,
            )
        }
        Pages(today, mode, pages)
    }
        // One clock for the whole state
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    private data class LoadedSegments(val days: ClosedRange<LocalDate>, val segments: List<EventSegment>)

    /** Only re-queried when the pages cover other days, not on every tick. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val segments = pages
        .map { it.days }
        .distinctUntilChanged()
        .flatMapLatest { days ->
            calendarRepository.observeEventSegments(days.start, days.endInclusive).map { LoadedSegments(days, it) }
        }

    val uiState: StateFlow<HomeUiState> = combine(
        pages,
        segments,
        combine(calendarRepository.observeActiveCalendars(), groupRepository.observeGroups(), ::Pair),
        combine(
            selection,
            keyValueRepository.observeInstant(KeyValueKey.LAST_EVENT_SYNC),
            permissionRepository.observe(AppPermission.READ_CALENDAR),
            ::Triple,
        ),
    ) { pages, loaded, (activeAccounts, groups), (selection, lastEventSync, permission) ->
        val activeIds = activeAccounts.flatMap { it.calendars }.map { it.id }.toSet()
        // Drop the selection if the calendar or group is gone
        val selectedCalendar = (selection as? Selection.Calendar)?.id?.takeIf { it in activeIds }
        val selectedGroup = (selection as? Selection.Group)?.id
            ?.let { id -> groups.find { it.id == id } }
            ?.takeIf { group -> group.calendarIds.any { it in activeIds } }
        val shownCalendarIds = when {
            selectedCalendar != null -> setOf(selectedCalendar)
            selectedGroup != null -> selectedGroup.calendarIds intersect activeIds
            else -> null
        }
        val homePages = pages.pages
            // Pages whose days are not loaded yet would show too little
            .filter { it.days.start >= loaded.days.start && it.days.endInclusive <= loaded.days.endInclusive }
            .map { page ->
                HomePage(
                    offset = page.offset,
                    firstDay = page.firstDay,
                    range = page.resolved,
                    calendars = calendarTimes(activeAccounts, loaded.segments, page.resolved, groups),
                    events = eventItems(activeAccounts, loaded.segments, page.resolved, shownCalendarIds),
                )
            }
            .associateBy { it.offset }
        HomeUiState(
            shortcuts = RangeShortcut.available(pages.today),
            selectedShortcut = pages.mode.shortcut,
            filter = pages.pages.first { it.offset == pages.mode.offset }.filter,
            today = pages.today,
            unit = pages.mode.shortcut?.unit,
            pageOffset = pages.mode.offset,
            pages = homePages,
            hasActiveCalendars = activeIds.isNotEmpty(),
            groups = groupInfos(groups, homePages.values.firstOrNull()?.calendars.orEmpty()),
            selectedCalendarId = selectedCalendar,
            selectedGroupId = selectedGroup?.id,
            lastEventSync = lastEventSync,
            hasCalendarPermission = permission == PermissionStatus.Granted,
        )
    }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private val _events = Channel<HomeEvent>(Channel.BUFFERED)
    val events: Flow<HomeEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            permissionRepository.observe(AppPermission.READ_CALENDAR)
                .first { it != PermissionStatus.Granted }
            _events.send(HomeEvent.NavigateToOnboarding)
        }
        // Drop active calendars that were deleted on the device, whenever the device calendars change
        viewModelScope.launch {
            calendarRepository.observeAccounts().collect { calendarRepository.syncActiveCalendars() }
        }
    }

    /** Selects the calendar, or clears the selection if it is already selected or `null`. */
    fun onCalendarSelect(calendarId: Long?) {
        val new = calendarId?.let(Selection::Calendar)
        selection.update { current -> if (new == current) null else new }
    }

    /** Selects the group instead of a calendar, or clears the selection if it is already selected. */
    fun onGroupSelect(groupId: Long) {
        val new = Selection.Group(groupId)
        selection.update { current -> if (new == current) null else new }
    }

    fun onClearSelection() {
        selection.value = null
    }

    /** Shows the shortcut's own page, from where the user can page through its unit. */
    fun onShortcutSelect(shortcut: RangeShortcut) {
        mode.update { it.copy(shortcut = shortcut, offset = shortcut.anchorOffset) }
    }

    /** Goes to the page [offset] units from the current one; there are no pages in the future. */
    fun onPageChange(offset: Int) {
        mode.update { if (it.shortcut?.unit == null) it else it.copy(offset = offset.coerceAtMost(0)) }
    }

    /** Uses a custom range instead of a shortcut. */
    fun onFilterChange(filter: TimeRangeFilter) {
        mode.value = Mode(shortcut = null, customFilter = filter)
    }
}
