package es.jvbabi.clockme.data.calendar.local

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/** The account (group) an active calendar belongs to. */
@Entity(tableName = "calendar_account")
data class CalendarAccountEntity(
    @PrimaryKey val id: String,
    val name: String,
)
