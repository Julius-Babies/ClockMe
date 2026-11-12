package es.jvbabi.clockme.data.calendar.local

import androidx.room3.Embedded
import androidx.room3.Relation

data class AccountWithActiveCalendars(
    @Embedded val account: CalendarAccountEntity,
    @Relation(parentColumns = ["id"], entityColumns = ["account_id"])
    val calendars: List<ActiveCalendarEntity>,
)
