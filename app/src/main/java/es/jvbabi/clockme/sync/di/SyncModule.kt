package es.jvbabi.clockme.sync.di

import es.jvbabi.clockme.sync.CalendarEventSyncObserver
import es.jvbabi.clockme.sync.CalendarEventSyncScheduler
import es.jvbabi.clockme.sync.SyncCalendarEventsWorker
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val syncModule = module {
    singleOf(::CalendarEventSyncObserver)
    singleOf(::CalendarEventSyncScheduler)
    workerOf(::SyncCalendarEventsWorker)
}
