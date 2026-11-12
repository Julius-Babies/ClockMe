package es.jvbabi.clockme.feature.onboarding.ui

import es.jvbabi.clockme.data.permission.PermissionStatus
import es.jvbabi.clockme.testing.FakePermissionRepository
import es.jvbabi.clockme.testing.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class OnboardingRequestPermissionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    /** Keeps uiState subscribed like the screen does. */
    private fun TestScope.subscribe(viewModel: OnboardingRequestPermissionViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    }

    @Test
    fun exposesPermissionStatus() = runTest {
        val viewModel = OnboardingRequestPermissionViewModel(
            FakePermissionRepository(PermissionStatus.Denied),
        )
        subscribe(viewModel)

        assertEquals(PermissionStatus.Denied, viewModel.uiState.value.calendarPermission)
    }

    @Test
    fun grantClickShowsDialogWhenNotRequested() = runTest {
        val repository = FakePermissionRepository(PermissionStatus.NotRequested)
        val viewModel = OnboardingRequestPermissionViewModel(repository)
        subscribe(viewModel)

        viewModel.onGrantPermissionClick()

        assertEquals(1, repository.dialogRequests)
        assertEquals(0, repository.appSettingsOpened)
    }

    @Test
    fun grantClickShowsDialogWhenDenied() = runTest {
        val repository = FakePermissionRepository(PermissionStatus.Denied)
        val viewModel = OnboardingRequestPermissionViewModel(repository)
        subscribe(viewModel)

        viewModel.onGrantPermissionClick()

        assertEquals(1, repository.dialogRequests)
        assertEquals(0, repository.appSettingsOpened)
    }

    @Test
    fun grantClickOpensSettingsWithHintWhenPermanentlyDenied() = runTest {
        val repository = FakePermissionRepository(PermissionStatus.PermanentlyDenied)
        val viewModel = OnboardingRequestPermissionViewModel(repository)
        subscribe(viewModel)

        viewModel.onGrantPermissionClick()

        assertEquals(OnboardingRequestPermissionEvent.ShowAppSettingsHint, viewModel.events.first())
        assertEquals(0, repository.dialogRequests)
        assertEquals(1, repository.appSettingsOpened)
    }

    @Test
    fun navigatesToHomeWhenGrantedViaDialog() = runTest {
        val repository = FakePermissionRepository(PermissionStatus.NotRequested)
        repository.dialogResult = PermissionStatus.Granted
        val viewModel = OnboardingRequestPermissionViewModel(repository)
        subscribe(viewModel)

        viewModel.onGrantPermissionClick()

        assertEquals(OnboardingRequestPermissionEvent.NavigateToHome, viewModel.events.first())
    }

    @Test
    fun navigatesToHomeWhenGrantedInSettings() = runTest {
        val repository = FakePermissionRepository(PermissionStatus.PermanentlyDenied)
        val viewModel = OnboardingRequestPermissionViewModel(repository)

        // User grants the permission in the app settings and comes back
        repository.status.value = PermissionStatus.Granted

        assertEquals(OnboardingRequestPermissionEvent.NavigateToHome, viewModel.events.first())
    }

    @Test
    fun staysOnOnboardingWhenDialogIsDenied() = runTest {
        val repository = FakePermissionRepository(PermissionStatus.NotRequested)
        repository.dialogResult = PermissionStatus.Denied
        val viewModel = OnboardingRequestPermissionViewModel(repository)
        subscribe(viewModel)

        viewModel.onGrantPermissionClick()

        assertNull(withTimeoutOrNull(1_000) { viewModel.events.first() })
        assertEquals(PermissionStatus.Denied, viewModel.uiState.value.calendarPermission)
    }
}
