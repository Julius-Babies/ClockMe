package es.jvbabi.clockme.data.calendar

import androidx.annotation.ColorInt

/**
 * An account on the device that owns calendars (e.g. a Google account or a local account).
 *
 * @property id Unique id built from the account type and name, as Android has no single account id.
 */
data class CalendarAccount(
    val id: String,
    val name: String,
    val calendars: List<Calendar>,
)

data class Calendar(
    val id: Long,
    val name: String,
    @param:ColorInt val color: Int,
)
