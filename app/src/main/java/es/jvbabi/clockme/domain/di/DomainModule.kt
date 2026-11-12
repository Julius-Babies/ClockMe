package es.jvbabi.clockme.domain.di

import es.jvbabi.clockme.domain.calendar.SyncCalendarEventsUseCase
import org.koin.dsl.module

val domainModule = module {
    // Single instance, so concurrent syncs (app and background) are serialized by its mutex
    single { SyncCalendarEventsUseCase(get(), get()) }
}
