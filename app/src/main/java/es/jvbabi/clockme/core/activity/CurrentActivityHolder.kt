package es.jvbabi.clockme.core.activity

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.ref.WeakReference

/**
 * Keeps a weak reference to the current [ComponentActivity] so that non-UI layers
 * (e.g. repositories) can launch activity-bound APIs like permission dialogs.
 *
 * Must be registered via [Application.registerActivityLifecycleCallbacks].
 */
class CurrentActivityHolder : Application.ActivityLifecycleCallbacks {

    private var activityRef: WeakReference<ComponentActivity>? = null

    val currentActivity: ComponentActivity?
        get() = activityRef?.get()

    private val _resumeCount = MutableStateFlow(0L)

    /** Increments every time an activity is resumed, e.g. after returning from system settings. */
    val resumeCount: StateFlow<Long> = _resumeCount.asStateFlow()

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = track(activity)

    override fun onActivityStarted(activity: Activity) = track(activity)

    override fun onActivityResumed(activity: Activity) {
        track(activity)
        _resumeCount.value++
    }

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) {
        if (activityRef?.get() === activity) activityRef = null
    }

    private fun track(activity: Activity) {
        if (activity is ComponentActivity) activityRef = WeakReference(activity)
    }
}
