package es.jvbabi.clockme.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import es.jvbabi.clockme.domain.calendar.SyncCalendarEventsUseCase

class SyncCalendarEventsWorker(
    context: Context,
    params: WorkerParameters,
    private val syncCalendarEvents: SyncCalendarEventsUseCase,
    private val scheduler: CalendarEventSyncScheduler,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (inputData.getBoolean(KEY_REARM_TRIGGER, false)) scheduler.rearmOnCalendarChange()
        // Without permission there is nothing to sync, retrying wouldn't help
        syncCalendarEvents()
        return Result.success()
    }

    companion object {
        const val KEY_REARM_TRIGGER = "rearm_trigger"
    }
}
