package es.jvbabi.clockme.feature.reports.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.jvbabi.clockme.core.time.TimeProvider
import es.jvbabi.clockme.data.calendar.Calendar
import es.jvbabi.clockme.data.calendar.CalendarAccount
import es.jvbabi.clockme.data.calendar.CalendarRepository
import es.jvbabi.clockme.data.calendar.EventSegment
import es.jvbabi.clockme.data.group.CalendarGroup
import es.jvbabi.clockme.data.group.CalendarGroupRepository
import es.jvbabi.clockme.domain.calendar.clipTo
import es.jvbabi.clockme.domain.calendar.coveredDuration
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModel(
    timeProvider: TimeProvider,
    calendarRepository: CalendarRepository,
    groupRepository: CalendarGroupRepository,
) : ViewModel() {

    private val selection = MutableStateFlow(initialState(timeProvider))

    private val calendarTree = combine(
        calendarRepository.observeActiveCalendars(),
        groupRepository.observeGroups(),
        ::buildCalendarTree,
    )

    /** The stored segments of the days the span touches; they are cut to the span itself later. */
    private val segments = selection
        .map { it.from.date to it.to.date }
        .distinctUntilChanged()
        .flatMapLatest { (from, to) ->
            if (from <= to) calendarRepository.observeEventSegments(from, to) else flowOf(emptyList())
        }

    val uiState: StateFlow<ReportsUiState> = combine(selection, calendarTree, segments) { state, tree, segments ->
        val withTree = state.copy(calendars = tree)
        withTree.copy(preview = buildPreview(withTree, tree, segments, timeProvider.timeZone()))
    }.stateIn(viewModelScope, SharingStarted.Eagerly, selection.value)

    fun onAction(action: ReportsAction) {
        when (action) {
            is ReportsAction.FromDateChange -> selection.update { it.copy(from = LocalDateTime(action.date, it.from.time)) }
            is ReportsAction.FromTimeChange -> selection.update { it.copy(from = LocalDateTime(it.from.date, action.time)) }
            is ReportsAction.ToDateChange -> selection.update { it.copy(to = LocalDateTime(action.date, it.to.time)) }
            is ReportsAction.ToTimeChange -> selection.update { it.copy(to = LocalDateTime(it.to.date, action.time)) }
            is ReportsAction.CalendarToggle -> selection.update { state ->
                val id = action.calendarId
                val deselected = state.deselectedCalendarIds
                state.copy(deselectedCalendarIds = if (id in deselected) deselected - id else deselected + id)
            }
            ReportsAction.IncludeEventsToggle -> selection.update { it.copy(includeEvents = !it.includeEvents) }
            is ReportsAction.GroupToggle -> {
                val state = uiState.value
                val group = state.calendars?.groups?.find { it.id == action.groupId } ?: return
                val ids = group.calendars.map { it.id }.toSet()
                val allIn = state.selectionOf(group) == GroupSelection.ALL
                selection.update {
                    it.copy(deselectedCalendarIds = if (allIn) it.deselectedCalendarIds + ids else it.deselectedCalendarIds - ids)
                }
            }
        }
    }

    internal companion object {
        /** This month so far: from midnight on its first up to the current minute. */
        fun initialState(timeProvider: TimeProvider): ReportsUiState {
            val now = timeProvider.now().toLocalDateTime(timeProvider.timeZone())
            return ReportsUiState(
                from = LocalDateTime(LocalDate(now.year, now.month, 1), LocalTime(0, 0)),
                to = LocalDateTime(now.date, LocalTime(now.hour, now.minute)),
            )
        }

        fun buildPreview(
            state: ReportsUiState,
            tree: ReportCalendarTree,
            segments: List<EventSegment>,
            timeZone: TimeZone,
        ): ReportPreview {
            if (!state.isRangeValid) return ReportPreview(emptyList(), eventCount = 0)
            val from = state.from.toInstant(timeZone)
            val to = state.to.toInstant(timeZone)
            val byCalendar = segments.mapNotNull { it.clipTo(from, to) }.groupBy { it.calendarId }
            fun previewOf(calendar: Calendar, groupId: Long?) = PreviewCalendar(
                calendarId = calendar.id,
                color = calendar.color,
                duration = byCalendar[calendar.id].orEmpty().coveredDuration(),
                groupId = groupId,
            )
            val grouped = tree.groups.flatMap { group ->
                group.calendars.filter(state::isSelected).map { previewOf(it, group.id) }
            }
            val ungrouped = tree.ungrouped.filter(state::isSelected).map { previewOf(it, null) }
            val calendars = grouped + ungrouped
            val selectedIds = calendars.map { it.calendarId }.toSet()
            val eventCount = byCalendar.filterKeys { it in selectedIds }.values.flatten().map { it.eventId }.distinct().size
            return ReportPreview(calendars, eventCount)
        }

        fun buildCalendarTree(activeAccounts: List<CalendarAccount>, groups: List<CalendarGroup>): ReportCalendarTree {
            val active = activeAccounts.flatMap { it.calendars }.distinctBy { it.id }.sortedBy { it.name.lowercase() }
            val reportGroups = groups.mapNotNull { group ->
                val calendars = active.filter { it.id in group.calendarIds }
                if (calendars.isEmpty()) null else ReportGroup(group.id, group.name, group.color, calendars)
            }
            val grouped = reportGroups.flatMap { it.calendars }.map { it.id }.toSet()
            return ReportCalendarTree(groups = reportGroups, ungrouped = active.filter { it.id !in grouped })
        }
    }
}
