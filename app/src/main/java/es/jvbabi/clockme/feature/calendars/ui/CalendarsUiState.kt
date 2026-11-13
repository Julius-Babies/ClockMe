package es.jvbabi.clockme.feature.calendars.ui

import androidx.annotation.ColorInt
import es.jvbabi.clockme.data.calendar.Calendar
import es.jvbabi.clockme.data.calendar.CalendarAccount
import es.jvbabi.clockme.data.group.CalendarGroup

data class CalendarsUiState(
    /** `null` while loading. */
    val accounts: List<CalendarAccount>? = null,
    val activeCalendarIds: Set<Long> = emptySet(),
    val groups: List<CalendarGroup> = emptyList(),
    /** The group being created or edited, `null` if the editor is closed. */
    val groupEditor: GroupEditorState? = null,
) {
    /** Active calendars by id, to show the calendars of a group. */
    val activeCalendarsById: Map<Long, Calendar>
        get() = accounts.orEmpty().flatMap { it.calendars }.filter { it.id in activeCalendarIds }.associateBy { it.id }

    /** Only the active calendars, accounts without any are left out; these can be grouped. */
    val activeAccounts: List<CalendarAccount>
        get() = accounts.orEmpty()
            .map { account -> account.copy(calendars = account.calendars.filter { it.id in activeCalendarIds }) }
            .filter { it.calendars.isNotEmpty() }

    /** The group each calendar is in. */
    val groupOfCalendar: Map<Long, CalendarGroup>
        get() = groups.flatMap { group -> group.calendarIds.map { it to group } }.toMap()
}

data class GroupEditorState(
    /** `null` for a new group. */
    val groupId: Long? = null,
    val name: String = "",
    @param:ColorInt val color: Int? = null,
    val calendarIds: Set<Long> = emptySet(),
) {
    val isNew: Boolean get() = groupId == null
    val canSave: Boolean get() = name.isNotBlank()
}

sealed interface CalendarsAction {
    data class CalendarClick(val account: CalendarAccount, val calendar: Calendar) : CalendarsAction
    data object CreateGroup : CalendarsAction
    data class EditGroup(val group: CalendarGroup) : CalendarsAction
    data class EditorNameChange(val name: String) : CalendarsAction
    data class EditorColorChange(@param:ColorInt val color: Int?) : CalendarsAction
    data class EditorCalendarToggle(val calendarId: Long) : CalendarsAction
    data object EditorSave : CalendarsAction
    data object EditorDelete : CalendarsAction
    data object EditorDismiss : CalendarsAction
}

/** Colors offered for groups. */
val GROUP_COLORS: List<Int> = listOf(
    0xFFD50000, 0xFFF4511E, 0xFFF6BF26, 0xFF33B679, 0xFF0B8043,
    0xFF039BE5, 0xFF3F51B5, 0xFF7986CB, 0xFF8E24AA, 0xFF616161,
).map { it.toInt() }
