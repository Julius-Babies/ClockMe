package es.jvbabi.clockme.sync

import android.content.Context
import android.provider.CalendarContract
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

/**
 * Schedules syncing the calendar events in the background, also while the app is not running:
 * - whenever the device calendars change (content URI trigger, re-armed after every run)
 * - once a day, so the sync window moves along and nothing is missed
 */
class CalendarEventSyncScheduler(context: Context) {

    private val workManager = WorkManager.getInstance(context)

    fun schedule() {
        scheduleOnCalendarChange(ExistingWorkPolicy.KEEP)
        workManager.enqueueUniquePeriodicWork(
            DAILY_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<SyncCalendarEventsWorker>(1, TimeUnit.DAYS).build(),
        )
    }

    /**
     * A content URI trigger fires only once, so the worker re-arms it on every run. Appending
     * makes the new request wait until the running one has finished instead of cancelling it.
     */
    internal fun rearmOnCalendarChange() = scheduleOnCalendarChange(ExistingWorkPolicy.APPEND_OR_REPLACE)

    private fun scheduleOnCalendarChange(policy: ExistingWorkPolicy) {
        val request = OneTimeWorkRequestBuilder<SyncCalendarEventsWorker>()
            .setConstraints(
                Constraints.Builder()
                    .addContentUriTrigger(CalendarContract.CONTENT_URI, triggerForDescendants = true)
                    .setTriggerContentUpdateDelay(TRIGGER_DELAY_SECONDS, TimeUnit.SECONDS)
                    .setTriggerContentMaxDelay(TRIGGER_MAX_DELAY_SECONDS, TimeUnit.SECONDS)
                    .build(),
            )
            .setInputData(workDataOf(SyncCalendarEventsWorker.KEY_REARM_TRIGGER to true))
            .build()
        workManager.enqueueUniqueWork(CONTENT_TRIGGER_WORK, policy, request)
    }

    companion object {
        const val CONTENT_TRIGGER_WORK = "calendar_event_sync_on_change"
        const val DAILY_WORK = "calendar_event_sync_daily"

        private const val TRIGGER_DELAY_SECONDS = 5L
        private const val TRIGGER_MAX_DELAY_SECONDS = 60L
    }
}
