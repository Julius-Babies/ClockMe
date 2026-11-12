package es.jvbabi.clockme.di

import es.jvbabi.clockme.core.di.coreModule
import es.jvbabi.clockme.data.di.dataModule
import es.jvbabi.clockme.feature.home.di.homeModule
import es.jvbabi.clockme.feature.onboarding.di.onboardingModule
import org.koin.dsl.module

val appModule = module {
    includes(
        coreModule,
        dataModule,
        homeModule,
        onboardingModule,
    )
}
