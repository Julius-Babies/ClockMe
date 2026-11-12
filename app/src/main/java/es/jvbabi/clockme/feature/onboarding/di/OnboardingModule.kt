package es.jvbabi.clockme.feature.onboarding.di

import es.jvbabi.clockme.feature.onboarding.ui.OnboardingRequestPermissionViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val onboardingModule = module {
    viewModelOf(::OnboardingRequestPermissionViewModel)
}
