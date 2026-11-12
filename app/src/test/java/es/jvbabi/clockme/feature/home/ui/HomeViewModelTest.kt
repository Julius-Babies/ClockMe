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
    fun exposesActiveCalendarCount() = runTest {
        val calendarRepository = FakeCalendarRepository()
        val viewModel = HomeViewModel(FakePermissionRepository(PermissionStatus.Granted), calendarRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        assertEquals(0, viewModel.uiState.value.activeCalendarCount)

        val account = CalendarAccount(id = "LOCAL/Device", name = "Device", calendars = emptyList())
        calendarRepository.setCalendarActive(account, Calendar(id = 1, name = "Work", color = 0), active = true)
        calendarRepository.setCalendarActive(account, Calendar(id = 2, name = "Personal", color = 0), active = true)

        assertEquals(2, viewModel.uiState.value.activeCalendarCount)
    }

    @Test
    fun syncsActiveCalendarsWhenDeviceCalendarsChange() = runTest {
        val calendarRepository = FakeCalendarRepository()
        HomeViewModel(FakePermissionRepository(PermissionStatus.Granted), calendarRepository)
        assertEquals(1, calendarRepository.syncCount)

        calendarRepository.accounts.value = listOf(
            CalendarAccount(id = "LOCAL/Device", name = "Device", calendars = emptyList()),
        )
        assertEquals(2, calendarRepository.syncCount)
    }
}
