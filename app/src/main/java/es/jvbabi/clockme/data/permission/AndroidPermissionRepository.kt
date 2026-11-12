package es.jvbabi.clockme.data.permission

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import es.jvbabi.clockme.core.activity.CurrentActivityHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.coroutines.resume

class AndroidPermissionRepository(
    private val context: Context,
    private val activityHolder: CurrentActivityHolder,
) : PermissionRepository {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Triggers a re-check after a dialog result. Resumes are covered by [CurrentActivityHolder.resumeCount]. */
    private val refreshSignal = MutableStateFlow(0L)

    override fun observe(permission: AppPermission): Flow<PermissionStatus> =
        combine(activityHolder.resumeCount, refreshSignal) { _, _ -> currentStatus(permission) }
            .distinctUntilChanged()

    override suspend fun requestViaDialog(permission: AppPermission): PermissionStatus =
        withContext(Dispatchers.Main.immediate) {
            val activity = activityHolder.currentActivity
                ?: error("No activity available to request ${permission.manifestName}")

            suspendCancellableCoroutine { continuation ->
                val launcher = activity.activityResultRegistry.register(
                    "permission_${UUID.randomUUID()}",
                    ActivityResultContracts.RequestPermission(),
                ) {
                    markRequested(permission)
                    refreshSignal.value++
                    if (continuation.isActive) continuation.resume(currentStatus(permission))
                }
                continuation.invokeOnCancellation { launcher.unregister() }
                launcher.launch(permission.manifestName)
            }
        }

    override fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null),
        )
        val activity = activityHolder.currentActivity
        if (activity != null) {
            activity.startActivity(intent)
        } else {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    private fun currentStatus(permission: AppPermission): PermissionStatus {
        val granted = ContextCompat.checkSelfPermission(context, permission.manifestName) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) return PermissionStatus.Granted

        val showRationale = activityHolder.currentActivity
            ?.shouldShowRequestPermissionRationale(permission.manifestName) == true

        return when {
            showRationale -> PermissionStatus.Denied
            !wasRequested(permission) -> PermissionStatus.NotRequested
            // Requested before, no rationale anymore -> the system won't show the dialog again
            else -> PermissionStatus.PermanentlyDenied
        }
    }

    private fun wasRequested(permission: AppPermission) =
        prefs.getBoolean(permission.manifestName, false)

    private fun markRequested(permission: AppPermission) =
        prefs.edit { putBoolean(permission.manifestName, true) }

    private companion object {
        const val PREFS_NAME = "permissions"
    }
}
