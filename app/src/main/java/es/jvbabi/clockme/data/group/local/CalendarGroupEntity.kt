package es.jvbabi.clockme.data.group.local

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "calendar_group")
data class CalendarGroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: Int?,
)
