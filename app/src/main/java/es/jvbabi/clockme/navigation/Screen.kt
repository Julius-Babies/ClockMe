package es.jvbabi.clockme.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * All navigation destinations of the app.
 * To add a screen: add a subtype here and register it in [AppNavigation].
 */
@Serializable
sealed interface Screen : NavKey {

    /** A destination of the [BottomNavBar]; the bar shows while one of them is on top. */
    @Serializable
    sealed interface Tab : Screen

    @Serializable
    data object Home : Tab

    /** All calendars on the device, to pick the active ones. */
    @Serializable
    data object Calendars : Tab

    @Serializable
    data object OnboardingRequestPermission : Screen
}
