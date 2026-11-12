package es.jvbabi.clockme.feature.home.ui

import kotlin.time.Instant

data class HomeUiState(
    /** `null` while loading. */
    val activeCalendarCount: Int? = null,
    /** When the calendar events were last synced, `null` if never. */
    val lastEventSync: Instant? = null,
)
