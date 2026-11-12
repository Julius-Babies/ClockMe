package es.jvbabi.clockme.data.di

import es.jvbabi.clockme.data.calendar.AndroidCalendarRepository
import es.jvbabi.clockme.data.calendar.CalendarRepository
import es.jvbabi.clockme.data.database.ClockMeDatabase
import es.jvbabi.clockme.data.permission.AndroidPermissionRepository
import es.jvbabi.clockme.data.permission.PermissionRepository
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val dataModule = module {
    single { ClockMeDatabase.create(androidContext()) }

    singleOf(::AndroidPermissionRepository) { bind<PermissionRepository>() }
    singleOf(::AndroidCalendarRepository) { bind<CalendarRepository>() }
}
