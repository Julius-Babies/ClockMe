package es.jvbabi.clockme.feature.calendars.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.jvbabi.clockme.data.calendar.Calendar
import es.jvbabi.clockme.data.calendar.CalendarAccount
import es.jvbabi.clockme.data.calendar.CalendarRepository
import es.jvbabi.clockme.data.group.CalendarGroupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CalendarsViewModel(
    private val calendarRepository: CalendarRepository,
    private val groupRepository: CalendarGroupRepository,
) : ViewModel() {

    private val groupEditor = MutableStateFlow<GroupEditorState?>(null)

    val uiState: StateFlow<CalendarsUiState> = combine(
        calendarRepository.observeAccounts(),
        calendarRepository.observeActiveCalendars(),
        groupRepository.observeGroups(),
        groupEditor,
    ) { accounts, activeAccounts, groups, editor ->
        CalendarsUiState(
            accounts = accounts,
            activeCalendarIds = activeAccounts.flatMap { it.calendars }.map { it.id }.toSet(),
            groups = groups,
            groupEditor = editor,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarsUiState())

    fun onAction(action: CalendarsAction) {
        when (action) {
            is CalendarsAction.CalendarClick -> onCalendarClick(action.account, action.calendar)
            CalendarsAction.CreateGroup -> groupEditor.value = GroupEditorState()
            is CalendarsAction.EditGroup -> groupEditor.value = GroupEditorState(
                groupId = action.group.id,
                name = action.group.name,
                color = action.group.color,
                calendarIds = action.group.calendarIds,
            )
            is CalendarsAction.EditorNameChange -> groupEditor.update { it?.copy(name = action.name) }
            is CalendarsAction.EditorColorChange -> groupEditor.update { it?.copy(color = action.color) }
            is CalendarsAction.EditorCalendarToggle -> groupEditor.update { editor ->
                editor?.copy(
                    calendarIds = if (action.calendarId in editor.calendarIds) {
                        editor.calendarIds - action.calendarId
                    } else {
                        editor.calendarIds + action.calendarId
                    },
                )
            }
            CalendarsAction.EditorSave -> saveGroup()
            CalendarsAction.EditorDelete -> deleteGroup()
            CalendarsAction.EditorDismiss -> groupEditor.value = null
        }
    }

    private fun onCalendarClick(account: CalendarAccount, calendar: Calendar) {
        val active = calendar.id !in uiState.value.activeCalendarIds
        viewModelScope.launch { calendarRepository.setCalendarActive(account, calendar, active) }
    }

    private fun saveGroup() {
        val editor = groupEditor.value?.takeIf { it.canSave } ?: return
        groupEditor.value = null
        viewModelScope.launch {
            if (editor.groupId == null) {
                groupRepository.createGroup(editor.name, editor.color, editor.calendarIds)
            } else {
                groupRepository.updateGroup(editor.groupId, editor.name, editor.color, editor.calendarIds)
            }
        }
    }

    private fun deleteGroup() {
        val groupId = groupEditor.value?.groupId ?: return
        groupEditor.value = null
        viewModelScope.launch { groupRepository.deleteGroup(groupId) }
    }
}
