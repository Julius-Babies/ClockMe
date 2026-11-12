package es.jvbabi.clockme.feature.onboarding.ui

import es.jvbabi.clockme.data.permission.PermissionStatus

data class OnboardingRequestPermissionUiState(
    /** `null` while the status is being loaded. */
    val calendarPermission: PermissionStatus? = null,
)
