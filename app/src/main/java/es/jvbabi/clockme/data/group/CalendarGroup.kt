package es.jvbabi.clockme.data.group

import androidx.annotation.ColorInt

/** A user defined group of calendars. A calendar is in at most one group. */
data class CalendarGroup(
    val id: Long,
    val name: String,
    @param:ColorInt val color: Int?,
    val calendarIds: Set<Long>,
)
