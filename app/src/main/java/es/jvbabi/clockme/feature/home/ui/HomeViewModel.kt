package es.jvbabi.clockme.feature.home.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.jvbabi.clockme.core.time.TimeProvider
import es.jvbabi.clockme.data.calendar.CalendarRepository
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

    /** A shortcut, or a custom filter if `null`. */
    private val selectedShortcut = MutableStateFlow<RangeShortcut?>(RangeShortcut.TODAY)
    private val customFilter = MutableStateFlow(TimeRangeFilter())
    private val selectedCalendarId = MutableStateFlow<Long?>(null)

    private data class Range(
        val today: LocalDate,
        val shortcut: RangeShortcut?,
        val filter: TimeRangeFilter,
        val resolved: ResolvedTimeRange,
    )

    private val rangeWithFilter = combine(
        selectedShortcut,
        customFilter,
        timeProvider.ticks(1.seconds),
    ) { shortcut, customFilter, now ->
        val timeZone = timeProvider.timeZone()
        val today = now.toLocalDateTime(timeZone).date
        val filter = shortcut?.toFilter(today, timeZone) ?: customFilter
        Range(today, shortcut, filter, filter.resolve(now, timeZone))
    }
        // One clock for the whole state
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    private val range = rangeWithFilter.map { it.resolved }

    /** Only re-queried when the range covers other days, not on every tick. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val segments = range
        .map { range ->
            val timeZone = timeProvider.timeZone()
            range.from.toLocalDateTime(timeZone).date..range.to.toLocalDateTime(timeZone).date
        }
        .distinctUntilChanged()
        .flatMapLatest { days -> calendarRepository.observeEventSegments(days.start, days.endInclusive) }

    val uiState: StateFlow<HomeUiState> = combine(
        rangeWithFilter,
        segments,
        combine(calendarRepository.observeActiveCalendars(), groupRepository.observeGroups(), ::Pair),
        combine(
            selectedCalendarId,
            keyValueRepository.observeInstant(KeyValueKey.LAST_EVENT_SYNC),
            permissionRepository.observe(AppPermission.READ_CALENDAR),
            ::Triple,
        ),
    ) { (today, shortcut, filter, range), segments, (activeAccounts, groups), (selectedId, lastEventSync, permission) ->
        val calendars = calendarTimes(activeAccounts, segments, range, groups)
        // Drop the selection if the calendar is gone
        val selected = selectedId?.takeIf { id -> calendars.any { it.calendarId == id } }
        HomeUiState(
            shortcuts = RangeShortcut.available(today),
            selectedShortcut = shortcut,
            filter = filter,
            range = range,
            calendars = calendars,
            groups = groupInfos(groups, calendars),
            selectedCalendarId = selected,
            events = eventItems(activeAccounts, segments, range, selected),
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
        selectedCalendarId.update { current -> if (calendarId == current) null else calendarId }
    }

    fun onClearSelection() {
        selectedCalendarId.value = null
    }

    fun onShortcutSelect(shortcut: RangeShortcut) {
        selectedShortcut.value = shortcut
    }

    /** Uses a custom range instead of a shortcut. */
    fun onFilterChange(filter: TimeRangeFilter) {
        customFilter.value = filter
        selectedShortcut.value = null
    }
}
