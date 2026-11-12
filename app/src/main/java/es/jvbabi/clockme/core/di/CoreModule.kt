package es.jvbabi.clockme.core.di

import es.jvbabi.clockme.core.activity.CurrentActivityHolder
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val coreModule = module {
    singleOf(::CurrentActivityHolder)
}
