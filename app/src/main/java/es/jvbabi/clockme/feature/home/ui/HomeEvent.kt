package es.jvbabi.clockme.feature.home.ui

sealed interface HomeEvent {
    data object NavigateToOnboarding : HomeEvent
}
