package es.jvbabi.clockme.sync

import android.util.Log
import es.jvbabi.clockme.data.calendar.CalendarRepository
import es.jvbabi.clockme.domain.calendar.SyncCalendarEventsUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

/**
 * Keeps the stored events up to date while the app process is alive: syncs on every change in
 * the device calendars and whenever a calendar gets (de)activated.
 * When the app is not running, [CalendarEventSyncScheduler] takes over.
 */
class CalendarEventSyncObserver(
    private val calendarRepository: CalendarRepository,
    private val syncCalendarEvents: SyncCalendarEventsUseCase,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    @OptIn(FlowPreview::class)
    fun start() {
        if (job != null) return
        job = scope.launch {
            merge(
                calendarRepository.observeCalendarProviderChanges(),
                calendarRepository.observeActiveCalendars()
                    .map { accounts -> accounts.flatMap { it.calendars }.map { it.id }.toSet() }
                    .distinctUntilChanged()
                    .map { },
            )
                // The provider often notifies several times for one change
                .debounce(DEBOUNCE_MILLIS)
                .conflate()
                .collect { syncSafely() }
        }
    }

    /** A failed sync must not end the observer (or crash the app), the next change retries. */
    private suspend fun syncSafely() {
        try {
            syncCalendarEvents()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Syncing calendar events failed", e)
        }
    }

    private companion object {
        const val TAG = "CalendarEventSync"
        const val DEBOUNCE_MILLIS = 500L
    }
}
