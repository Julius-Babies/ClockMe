package es.jvbabi.clockme.data.calendar.local

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

/** A calendar the user activated in the app. [id] is the id in the system calendar provider. */
@Entity(
    tableName = "active_calendar",
    foreignKeys = [
        ForeignKey(
            entity = CalendarAccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["account_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("account_id")],
)
data class ActiveCalendarEntity(
    @PrimaryKey val id: Long,
    @ColumnInfo(name = "account_id") val accountId: String,
    val name: String,
    val color: Int,
)
