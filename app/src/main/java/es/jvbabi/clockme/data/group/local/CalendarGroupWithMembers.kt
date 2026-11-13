package es.jvbabi.clockme.data.group.local

import androidx.room3.Embedded
import androidx.room3.Relation

data class CalendarGroupWithMembers(
    @Embedded val group: CalendarGroupEntity,
    @Relation(parentColumns = ["id"], entityColumns = ["group_id"])
    val members: List<CalendarGroupMemberEntity>,
)
