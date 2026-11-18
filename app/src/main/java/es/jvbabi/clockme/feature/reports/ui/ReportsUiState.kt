package es.jvbabi.clockme.feature.reports.ui

import androidx.annotation.ColorInt
import es.jvbabi.clockme.data.calendar.Calendar
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime

/**
 * The span a report covers, from [from] up to [to], both in the device's time zone, and which of
 * the active calendars it takes in.
 */
data class ReportsUiState(
    val from: LocalDateTime,
    val to: LocalDateTime,
    /** `null` while the calendars load. */
    val calendars: ReportCalendarTree? = null,
    /** Left out rather than taken in, so a newly activated calendar is in the report right away. */
    val deselectedCalendarIds: Set<Long> = emptySet(),
) {
    /** A report needs its end after its start. */
    val isRangeValid: Boolean get() = from < to

    fun isSelected(calendar: Calendar): Boolean = calendar.id !in deselectedCalendarIds

    fun selectionOf(group: ReportGroup): GroupSelection {
        val selected = group.calendars.count(::isSelected)
        return when (selected) {
            group.calendars.size -> GroupSelection.ALL
            0 -> GroupSelection.NONE
            else -> GroupSelection.SOME
        }
    }
}

/** The active calendars: in their groups first, then those in none. */
data class ReportCalendarTree(
    val groups: List<ReportGroup>,
    val ungrouped: List<Calendar>,
) {
    val isEmpty: Boolean get() = groups.isEmpty() && ungrouped.isEmpty()
}

/** A group with its active calendars; groups without any are left out. */
data class ReportGroup(
    val id: Long,
    val name: String,
    @param:ColorInt val color: Int?,
    val calendars: List<Calendar>,
)

enum class GroupSelection { ALL, SOME, NONE }

sealed interface ReportsAction {
    data class FromDateChange(val date: LocalDate) : ReportsAction
    data class FromTimeChange(val time: LocalTime) : ReportsAction
    data class ToDateChange(val date: LocalDate) : ReportsAction
    data class ToTimeChange(val time: LocalTime) : ReportsAction
    data class CalendarToggle(val calendarId: Long) : ReportsAction

    /** Takes in all of the group's calendars, or leaves them all out if all were in. */
    data class GroupToggle(val groupId: Long) : ReportsAction
}
