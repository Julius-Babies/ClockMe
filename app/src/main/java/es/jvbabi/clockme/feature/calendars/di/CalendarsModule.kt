package es.jvbabi.clockme.feature.calendars.di

import es.jvbabi.clockme.feature.calendars.ui.CalendarsViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val calendarsModule = module {
    viewModelOf(::CalendarsViewModel)
}
