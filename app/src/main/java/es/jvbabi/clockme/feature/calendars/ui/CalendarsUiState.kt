package es.jvbabi.clockme.feature.calendars.ui

import es.jvbabi.clockme.data.calendar.CalendarAccount

data class CalendarsUiState(
    /** `null` while loading. */
    val accounts: List<CalendarAccount>? = null,
    val activeCalendarIds: Set<Long> = emptySet(),
)
