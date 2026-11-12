package es.jvbabi.clockme.data.permission

sealed interface PermissionStatus {

    data object Granted : PermissionStatus

    /** Never requested yet, the system dialog can be shown. */
    data object NotRequested : PermissionStatus

    /** Denied before, the system dialog can still be shown (a rationale should be displayed). */
    data object Denied : PermissionStatus

    /** Denied permanently, can only be granted in the app settings. */
    data object PermanentlyDenied : PermissionStatus
}

val PermissionStatus.canRequestViaDialog: Boolean
    get() = this is PermissionStatus.NotRequested || this is PermissionStatus.Denied
