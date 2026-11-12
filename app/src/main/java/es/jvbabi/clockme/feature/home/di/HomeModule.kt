package es.jvbabi.clockme.feature.home.di

import es.jvbabi.clockme.feature.home.ui.HomeViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val homeModule = module {
    viewModelOf(::HomeViewModel)
}
