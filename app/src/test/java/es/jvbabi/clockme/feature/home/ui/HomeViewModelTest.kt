package es.jvbabi.clockme.feature.home.ui

import es.jvbabi.clockme.data.permission.PermissionStatus
import es.jvbabi.clockme.data.calendar.Calendar
import es.jvbabi.clockme.data.calendar.CalendarAccount
import es.jvbabi.clockme.testing.FakeCalendarRepository
import es.jvbabi.clockme.testing.FakePermissionRepository
import es.jvbabi.clockme.testing.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
        val viewModel = HomeViewModel(FakePermissionRepository(PermissionStatus.NotRequested), FakeCalendarRepository())

        assertEquals(HomeEvent.NavigateToOnboarding, viewModel.events.first())
    }

    @Test
    fun navigatesToOnboardingWhenPermissionIsPermanentlyDenied() = runTest {
        val viewModel = HomeViewModel(FakePermissionRepository(PermissionStatus.PermanentlyDenied), FakeCalendarRepository())

        assertEquals(HomeEvent.NavigateToOnboarding, viewModel.events.first())
    }

    @Test
    fun staysOnHomeWhenPermissionIsGranted() = runTest {
        val viewModel = HomeViewModel(FakePermissionRepository(PermissionStatus.Granted), FakeCalendarRepository())

        assertNull(withTimeoutOrNull(1_000) { viewModel.events.first() })
    }

    @Test
    fun navigatesToOnboardingWhenPermissionGetsRevoked() = runTest {
        val repository = FakePermissionRepository(PermissionStatus.Granted)
        val viewModel = HomeViewModel(repository, FakeCalendarRepository())

        repository.status.value = PermissionStatus.PermanentlyDenied

        assertEquals(HomeEvent.NavigateToOnboarding, viewModel.events.first())
    }

    @Test
    fun exposesCalendarAccounts() = runTest {
        val account = CalendarAccount(
            id = "LOCAL/Device",
            name = "Device",
            calendars = listOf(Calendar(id = 1, name = "Personal", color = 0xFF0000FF.toInt())),
        )
        val calendarRepository = FakeCalendarRepository(listOf(account))
        val viewModel = HomeViewModel(
            FakePermissionRepository(PermissionStatus.Granted),
            calendarRepository,
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        assertEquals(listOf(account), viewModel.uiState.value.accounts)

        calendarRepository.accounts.value = emptyList()
        assertEquals(emptyList<CalendarAccount>(), viewModel.uiState.value.accounts)
    }
}
