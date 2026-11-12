package es.jvbabi.clockme

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import es.jvbabi.clockme.core.ui.AppStartState
import es.jvbabi.clockme.core.ui.LocalAppStartState
import es.jvbabi.clockme.navigation.AppNavigation
import es.jvbabi.clockme.ui.theme.ClockMeTheme

class MainActivity : ComponentActivity() {

    private lateinit var appStart: AppStartState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // A recreated activity (e.g. after rotation) shows no splash
        appStart = AppStartState(ready = savedInstanceState != null)
        enableEdgeToEdge()
        if (!appStart.contentReady.value) holdSplashUntilContentIsReady()

        setContent {
            CompositionLocalProvider(LocalAppStartState provides appStart) {
                ClockMeTheme {
                    AppNavigation()
                }
            }
        }
    }

    /**
     * Keeps the splash (or, below Android 12, the window background) until the first screen has
     * loaded, so it never shows a loading state. The screen's own entry animation starts once
     * the splash is gone.
     */
    private fun holdSplashUntilContentIsReady() {
        val content = findViewById<View>(android.R.id.content)
        content.viewTreeObserver.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    if (!appStart.contentReady.value) return false
                    content.viewTreeObserver.removeOnPreDrawListener(this)
                    // In case no splash was shown, its exit listener is never called
                    content.postDelayed(appStart::onSplashDismissed, SPLASH_EXIT_FALLBACK_MILLIS)
                    return true
                }
            },
        )
        // Never keep the user waiting on the splash forever
        content.postDelayed(appStart::onContentReady, MAX_SPLASH_MILLIS)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            splashScreen.setOnExitAnimationListener { splash ->
                appStart.onSplashDismissed()
                splash.animate()
                    .alpha(0f)
                    .setDuration(SPLASH_FADE_MILLIS)
                    .withEndAction { splash.remove() }
                    .start()
            }
        }
    }

    private companion object {
        const val MAX_SPLASH_MILLIS = 3_000L
        const val SPLASH_FADE_MILLIS = 200L
        const val SPLASH_EXIT_FALLBACK_MILLIS = 500L
    }
}
