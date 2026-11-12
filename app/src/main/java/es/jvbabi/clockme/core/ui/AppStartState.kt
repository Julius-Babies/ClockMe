package es.jvbabi.clockme.core.ui

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Coordinates the splash screen with the first screen: the splash stays until the screen
 * reports [onContentReady], and the screen waits for [splashDismissed] before it starts
 * animations that should be seen.
 */
class AppStartState(ready: Boolean = false) {

    private val _contentReady = MutableStateFlow(ready)
    val contentReady: StateFlow<Boolean> = _contentReady.asStateFlow()

    private val _splashDismissed = MutableStateFlow(ready)
    val splashDismissed: StateFlow<Boolean> = _splashDismissed.asStateFlow()

    /** The first screen shows its real content, no loading state. */
    fun onContentReady() {
        _contentReady.value = true
    }

    fun onSplashDismissed() {
        _splashDismissed.value = true
    }
}

/** Defaults to an app that has already started, e.g. in previews and UI tests. */
val LocalAppStartState = staticCompositionLocalOf { AppStartState(ready = true) }
