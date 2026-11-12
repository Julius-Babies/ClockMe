package es.jvbabi.clockme.data.permission

import kotlinx.coroutines.flow.Flow

interface PermissionRepository {

    /** Emits the current status and updates on every change (e.g. after returning from settings). */
    fun observe(permission: AppPermission): Flow<PermissionStatus>

    /** Shows the system permission dialog and suspends until the user responded. */
    suspend fun requestViaDialog(permission: AppPermission): PermissionStatus

    /** Opens the app info screen in the system settings, where the permission can be granted. */
    fun openAppSettings()
}
