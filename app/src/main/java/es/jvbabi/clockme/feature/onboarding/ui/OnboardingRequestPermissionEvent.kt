package es.jvbabi.clockme.feature.onboarding.ui

sealed interface OnboardingRequestPermissionEvent {
    data object NavigateToHome : OnboardingRequestPermissionEvent

    /** The app settings are being opened, tell the user what to do there. */
    data object ShowAppSettingsHint : OnboardingRequestPermissionEvent
}
