package es.jvbabi.clockme.data.group.local

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CalendarGroupDao {

    @Transaction
    @Query("SELECT * FROM calendar_group ORDER BY name COLLATE NOCASE")
    fun observeGroups(): Flow<List<CalendarGroupWithMembers>>

    @Insert
    suspend fun insert(group: CalendarGroupEntity): Long

    @Query("UPDATE calendar_group SET name = :name, color = :color WHERE id = :id")
    suspend fun update(id: Long, name: String, color: Int?)

    @Query("DELETE FROM calendar_group WHERE id = :id")
    suspend fun deleteGroup(id: Long)

    /** Moves the calendar into the group, out of any other group. */
    @Upsert
    suspend fun addMember(member: CalendarGroupMemberEntity)

    @Query("DELETE FROM calendar_group_member WHERE group_id = :groupId AND calendar_id = :calendarId")
    suspend fun removeMember(groupId: Long, calendarId: Long)

    @Query("DELETE FROM calendar_group_member WHERE group_id = :groupId")
    suspend fun removeMembers(groupId: Long)

    @Query("DELETE FROM calendar_group_member WHERE calendar_id NOT IN (:existingCalendarIds)")
    suspend fun removeMembersNotIn(existingCalendarIds: List<Long>)

    /** Deletes the group with its members (Room doesn't enforce foreign keys here). */
    @Transaction
    suspend fun delete(id: Long) {
        removeMembers(id)
        deleteGroup(id)
    }

    /** Replaces name, color and calendars of the group; calendars of other groups move here. */
    @Transaction
    suspend fun save(id: Long, name: String, color: Int?, calendarIds: Set<Long>) {
        update(id, name, color)
        removeMembers(id)
        calendarIds.forEach { addMember(CalendarGroupMemberEntity(id, it)) }
    }

    @Transaction
    suspend fun create(name: String, color: Int?, calendarIds: Set<Long>): Long {
        val id = insert(CalendarGroupEntity(name = name, color = color))
        calendarIds.forEach { addMember(CalendarGroupMemberEntity(id, it)) }
        return id
    }
}
