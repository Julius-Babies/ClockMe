package es.jvbabi.clockme.feature.home.ui

import es.jvbabi.clockme.data.permission.PermissionStatus

data class HomeUiState(
    /** `null` while the status is being loaded. */
    val calendarPermission: PermissionStatus? = null,
)
