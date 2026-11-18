package es.jvbabi.clockme.di

import es.jvbabi.clockme.core.di.coreModule
import es.jvbabi.clockme.data.di.dataModule
import es.jvbabi.clockme.domain.di.domainModule
import es.jvbabi.clockme.feature.calendars.di.calendarsModule
import es.jvbabi.clockme.feature.home.di.homeModule
import es.jvbabi.clockme.feature.onboarding.di.onboardingModule
import es.jvbabi.clockme.feature.reports.di.reportsModule
import es.jvbabi.clockme.sync.di.syncModule
import org.koin.dsl.module

val appModule = module {
    includes(
        coreModule,
        dataModule,
        domainModule,
        syncModule,
        homeModule,
        calendarsModule,
        reportsModule,
        onboardingModule,
    )
}
