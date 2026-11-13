package es.jvbabi.clockme.data.group.local

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index

/**
 * A device calendar in a group. A calendar is in at most one group, so it is the key.
 * Removed with the group, see [CalendarGroupDao.delete].
 */
@Entity(
    tableName = "calendar_group_member",
    primaryKeys = ["calendar_id"],
    indices = [Index("group_id")],
)
data class CalendarGroupMemberEntity(
    @ColumnInfo(name = "group_id") val groupId: Long,
    @ColumnInfo(name = "calendar_id") val calendarId: Long,
)
