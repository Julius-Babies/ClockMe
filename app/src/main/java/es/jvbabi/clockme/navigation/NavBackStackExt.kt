package es.jvbabi.clockme.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

/** Pushes [screen] onto the back stack. */
fun NavBackStack<NavKey>.navigateTo(screen: Screen) {
    add(screen)
}

/** Makes [screen] the only entry, so back from it leaves the app. */
fun NavBackStack<NavKey>.replaceAll(screen: Screen) {
    // Add first so the back stack is never empty in between
    add(screen)
    subList(0, size - 1).clear()
}
