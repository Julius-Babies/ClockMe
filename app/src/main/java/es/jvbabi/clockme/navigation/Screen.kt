package es.jvbabi.clockme.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * All navigation destinations of the app.
 * To add a screen: add a subtype here and register it in [AppNavigation].
 */
@Serializable
sealed interface Screen : NavKey {

    @Serializable
    data object Home : Screen
}
