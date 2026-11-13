package es.jvbabi.clockme.data.group

import es.jvbabi.clockme.data.group.local.CalendarGroupDao
import es.jvbabi.clockme.data.group.local.CalendarGroupMemberEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomCalendarGroupRepository(
    private val dao: CalendarGroupDao,
) : CalendarGroupRepository {

    override fun observeGroups(): Flow<List<CalendarGroup>> = dao.observeGroups().map { groups ->
        groups.map { (group, members) ->
            CalendarGroup(
                id = group.id,
                name = group.name,
                color = group.color,
                calendarIds = members.map { it.calendarId }.toSet(),
            )
        }
    }

    override suspend fun createGroup(name: String, color: Int?, calendarIds: Set<Long>): Long =
        dao.create(name.trim(), color, calendarIds)

    override suspend fun updateGroup(id: Long, name: String, color: Int?, calendarIds: Set<Long>) =
        dao.save(id, name.trim(), color, calendarIds)

    override suspend fun deleteGroup(id: Long) = dao.delete(id)

    override suspend fun setCalendarInGroup(groupId: Long, calendarId: Long, inGroup: Boolean) {
        if (inGroup) {
            dao.addMember(CalendarGroupMemberEntity(groupId, calendarId))
        } else {
            dao.removeMember(groupId, calendarId)
        }
    }

    override suspend fun removeCalendarsNotIn(existingCalendarIds: Set<Long>) =
        dao.removeMembersNotIn(existingCalendarIds.toList())
}
