package es.jvbabi.clockme.testing

import es.jvbabi.clockme.data.group.CalendarGroup
import es.jvbabi.clockme.data.group.CalendarGroupRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeCalendarGroupRepository : CalendarGroupRepository {

    val groups = MutableStateFlow<List<CalendarGroup>>(emptyList())
    private var nextId = 1L

    override fun observeGroups(): Flow<List<CalendarGroup>> = groups.map { it.sortedBy { group -> group.name } }

    override suspend fun createGroup(name: String, color: Int?, calendarIds: Set<Long>): Long {
        val id = nextId++
        groups.update { all -> all.withoutCalendars(calendarIds) + CalendarGroup(id, name, color, calendarIds) }
        return id
    }

    override suspend fun updateGroup(id: Long, name: String, color: Int?, calendarIds: Set<Long>) {
        groups.update { all ->
            all.withoutCalendars(calendarIds).map { if (it.id == id) CalendarGroup(id, name, color, calendarIds) else it }
        }
    }

    /** A calendar is in at most one group. */
    private fun List<CalendarGroup>.withoutCalendars(calendarIds: Set<Long>) =
        map { it.copy(calendarIds = it.calendarIds - calendarIds) }

    override suspend fun deleteGroup(id: Long) {
        groups.update { all -> all.filter { it.id != id } }
    }

    override suspend fun setCalendarInGroup(groupId: Long, calendarId: Long, inGroup: Boolean) {
        groups.update { all ->
            all.map {
                when {
                    it.id == groupId && inGroup -> it.copy(calendarIds = it.calendarIds + calendarId)
                    else -> it.copy(calendarIds = it.calendarIds - calendarId)
                }
            }
        }
    }

    override suspend fun removeCalendarsNotIn(existingCalendarIds: Set<Long>) {
        groups.update { all -> all.map { it.copy(calendarIds = it.calendarIds intersect existingCalendarIds) } }
    }
}
