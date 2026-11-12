package es.jvbabi.clockme.data.permission

import android.Manifest

/** Runtime permissions used by the app. Each one must also be declared in the manifest. */
enum class AppPermission(val manifestName: String) {
    READ_CALENDAR(Manifest.permission.READ_CALENDAR),
}
