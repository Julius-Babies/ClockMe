package es.jvbabi.clockme.feature.home.ui

import es.jvbabi.clockme.data.calendar.CalendarAccount

data class HomeUiState(
    /** `null` while loading. */
    val accounts: List<CalendarAccount>? = null,
    val activeCalendarIds: Set<Long> = emptySet(),
)
