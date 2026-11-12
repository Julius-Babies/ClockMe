package es.jvbabi.clockme.testing

import es.jvbabi.clockme.data.permission.AppPermission
import es.jvbabi.clockme.data.permission.PermissionRepository
import es.jvbabi.clockme.data.permission.PermissionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakePermissionRepository(
    initialStatus: PermissionStatus = PermissionStatus.NotRequested,
) : PermissionRepository {

    val status = MutableStateFlow(initialStatus)

    /** Status the next [requestViaDialog] call results in. */
    var dialogResult: PermissionStatus = PermissionStatus.Granted

    var dialogRequests = 0
        private set
    var appSettingsOpened = 0
        private set

    override fun observe(permission: AppPermission): Flow<PermissionStatus> = status

    override suspend fun requestViaDialog(permission: AppPermission): PermissionStatus {
        dialogRequests++
        status.value = dialogResult
        return dialogResult
    }

    override fun openAppSettings() {
        appSettingsOpened++
    }
}
