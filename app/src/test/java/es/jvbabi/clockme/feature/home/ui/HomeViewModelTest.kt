package es.jvbabi.clockme.feature.home.ui

import es.jvbabi.clockme.data.permission.PermissionStatus
import es.jvbabi.clockme.testing.FakePermissionRepository
import es.jvbabi.clockme.testing.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun navigatesToOnboardingWhenPermissionIsMissing() = runTest {
        val viewModel = HomeViewModel(FakePermissionRepository(PermissionStatus.NotRequested))

        assertEquals(HomeEvent.NavigateToOnboarding, viewModel.events.first())
    }

    @Test
    fun navigatesToOnboardingWhenPermissionIsPermanentlyDenied() = runTest {
        val viewModel = HomeViewModel(FakePermissionRepository(PermissionStatus.PermanentlyDenied))

        assertEquals(HomeEvent.NavigateToOnboarding, viewModel.events.first())
    }

    @Test
    fun staysOnHomeWhenPermissionIsGranted() = runTest {
        val viewModel = HomeViewModel(FakePermissionRepository(PermissionStatus.Granted))

        assertNull(withTimeoutOrNull(1_000) { viewModel.events.first() })
    }

    @Test
    fun navigatesToOnboardingWhenPermissionGetsRevoked() = runTest {
        val repository = FakePermissionRepository(PermissionStatus.Granted)
        val viewModel = HomeViewModel(repository)

        repository.status.value = PermissionStatus.PermanentlyDenied

        assertEquals(HomeEvent.NavigateToOnboarding, viewModel.events.first())
    }
}
