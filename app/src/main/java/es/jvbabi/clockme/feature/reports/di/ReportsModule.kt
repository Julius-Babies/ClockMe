package es.jvbabi.clockme.feature.reports.di

import es.jvbabi.clockme.feature.reports.ui.ReportsViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val reportsModule = module {
    viewModelOf(::ReportsViewModel)
}
