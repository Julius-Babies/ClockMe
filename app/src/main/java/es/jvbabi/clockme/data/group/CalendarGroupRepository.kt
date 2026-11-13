package es.jvbabi.clockme.data.group

import kotlinx.coroutines.flow.Flow

/** Groups of calendars; a calendar is in at most one group, adding it elsewhere moves it. */
interface CalendarGroupRepository {

    /** All groups, sorted by name. */
    fun observeGroups(): Flow<List<CalendarGroup>>

    /** @return the id of the new group */
    suspend fun createGroup(name: String, color: Int?, calendarIds: Set<Long>): Long

    /** Replaces name, color and calendars of the group; calendars of other groups move here. */
    suspend fun updateGroup(id: Long, name: String, color: Int?, calendarIds: Set<Long>)

    suspend fun deleteGroup(id: Long)

    suspend fun setCalendarInGroup(groupId: Long, calendarId: Long, inGroup: Boolean)

    /** Removes calendars that no longer exist on the device from all groups. */
    suspend fun removeCalendarsNotIn(existingCalendarIds: Set<Long>)
}
