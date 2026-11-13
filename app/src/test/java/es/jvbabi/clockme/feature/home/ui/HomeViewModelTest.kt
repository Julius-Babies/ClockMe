package es.jvbabi.clockme.feature.home.ui

import es.jvbabi.clockme.data.calendar.Calendar
import es.jvbabi.clockme.data.calendar.CalendarAccount
import es.jvbabi.clockme.data.calendar.EventSegment
import es.jvbabi.clockme.data.permission.PermissionStatus
import es.jvbabi.clockme.testing.FakeCalendarGroupRepository
import es.jvbabi.clockme.testing.FakeCalendarRepository
import es.jvbabi.clockme.testing.FakeKeyValueRepository
import es.jvbabi.clockme.testing.FakePermissionRepository
import es.jvbabi.clockme.testing.FakeTimeProvider
import es.jvbabi.clockme.testing.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class HomeViewModelTest {

    private val berlin = TimeZone.of("Europe/Berlin")
    private val today = LocalDate.parse("2026-11-12")
    private fun at(time: String): Instant = LocalDateTime.parse("${today}T$time").toInstant(berlin)
    private val now = at("15:00")

    private val work = Calendar(id = 1, name = "Work", color = 1)
    private val gym = Calendar(id = 2, name = "Gym", color = 2)
    private val account = CalendarAccount(id = "LOCAL/Device", name = "Device", calendars = listOf(work, gym))

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun navigatesToOnboardingWhenPermissionIsMissing() = runTest {
        val viewModel = HomeViewModel(FakePermissionRepository(PermissionStatus.NotRequested), FakeCalendarRepository(), FakeKeyValueRepository(), FakeTimeProvider(now), FakeCalendarGroupRepository())

        assertEquals(HomeEvent.NavigateToOnboarding, viewModel.events.first())
    }

    @Test
    fun navigatesToOnboardingWhenPermissionIsPermanentlyDenied() = runTest {
        val viewModel = HomeViewModel(FakePermissionRepository(PermissionStatus.PermanentlyDenied), FakeCalendarRepository(), FakeKeyValueRepository(), FakeTimeProvider(now), FakeCalendarGroupRepository())

        assertEquals(HomeEvent.NavigateToOnboarding, viewModel.events.first())
    }

    @Test
    fun staysOnHomeWhenPermissionIsGranted() = runTest {
        val viewModel = HomeViewModel(FakePermissionRepository(PermissionStatus.Granted), FakeCalendarRepository(), FakeKeyValueRepository(), FakeTimeProvider(now), FakeCalendarGroupRepository())

        assertNull(withTimeoutOrNull(1_000) { viewModel.events.first() })
    }

    @Test
    fun navigatesToOnboardingWhenPermissionGetsRevoked() = runTest {
        val repository = FakePermissionRepository(PermissionStatus.Granted)
        val viewModel = HomeViewModel(repository, FakeCalendarRepository(), FakeKeyValueRepository(), FakeTimeProvider(now), FakeCalendarGroupRepository())

        repository.status.value = PermissionStatus.PermanentlyDenied

        assertEquals(HomeEvent.NavigateToOnboarding, viewModel.events.first())
    }

    @Test
    fun showsTimePerCalendarUntilNowAndUpdatesWithTheClock() = runTest {
        val (viewModel, timeProvider) = viewModelWithEvents()

        assertEquals(listOf(60.minutes, 30.minutes), viewModel.uiState.value.calendars!!.map { it.duration })
        assertEquals(90.minutes, viewModel.uiState.value.totalDuration)

        timeProvider.now = at("15:30")

        assertEquals(listOf(90.minutes, 30.minutes), viewModel.uiState.value.calendars!!.map { it.duration })
        assertEquals(ResolvedTimeRange(at("00:00"), at("15:30")), viewModel.uiState.value.range)
    }

    @Test
    fun selectingCalendarFiltersEventsAndSelectingAgainClearsIt() = runTest {
        val (viewModel, _) = viewModelWithEvents()
        assertEquals(listOf(1L, 2L), viewModel.uiState.value.events.map { it.eventId })

        viewModel.onCalendarSelect(work.id)
        assertEquals(work.id, viewModel.uiState.value.selectedCalendarId)
        assertEquals(listOf(2L), viewModel.uiState.value.events.map { it.eventId })

        viewModel.onCalendarSelect(work.id)
        assertNull(viewModel.uiState.value.selectedCalendarId)
        assertEquals(listOf(1L, 2L), viewModel.uiState.value.events.map { it.eventId })
    }

    @Test
    fun filterIsUsed() = runTest {
        val (viewModel, _) = viewModelWithEvents()

        viewModel.onFilterChange(TimeRangeFilter(from = at("11:45"), to = at("14:30")))

        assertEquals(listOf(30.minutes, 15.minutes), viewModel.uiState.value.calendars!!.map { it.duration })
        // the work event runs past the end of the range
        assertEquals(listOf(false, true), viewModel.uiState.value.events.map { it.isOngoing })
    }

    @Test
    fun shortcutsAreOfferedAndSelectable() = runTest {
        val (viewModel, _) = viewModelWithEvents()
        assertEquals(RangeShortcut.entries, viewModel.uiState.value.shortcuts)
        assertEquals(RangeShortcut.TODAY, viewModel.uiState.value.selectedShortcut)

        viewModel.onShortcutSelect(RangeShortcut.YESTERDAY)

        assertEquals(RangeShortcut.YESTERDAY, viewModel.uiState.value.selectedShortcut)
        assertEquals(
            ResolvedTimeRange(at("00:00") - 24.hours, at("00:00")),
            viewModel.uiState.value.range,
        )
        assertEquals(emptyList<Long>(), viewModel.uiState.value.events.map { it.eventId })
    }

    @Test
    fun customFilterDeselectsTheShortcut() = runTest {
        val (viewModel, _) = viewModelWithEvents()

        viewModel.onFilterChange(TimeRangeFilter(from = at("11:45"), to = at("14:30")))

        assertNull(viewModel.uiState.value.selectedShortcut)
    }

    @Test
    fun isReadyOnceLoadedWithPermission() = runTest {
        val (viewModel, _) = viewModelWithEvents()

        assertTrue(viewModel.uiState.value.isReady)
    }

    @Test
    fun isNotReadyWithoutPermissionAsOnboardingTakesOver() = runTest {
        val viewModel = HomeViewModel(
            FakePermissionRepository(PermissionStatus.NotRequested),
            FakeCalendarRepository(),
            FakeKeyValueRepository(),
            FakeTimeProvider(now),
            FakeCalendarGroupRepository(),
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        assertEquals(false, viewModel.uiState.value.hasCalendarPermission)
        assertFalse(viewModel.uiState.value.isReady)
    }

    /** Gym 11:30-12:00 (id 1), work from 14:00 (id 2, ongoing); now is 15:00. */
    private suspend fun TestScope.viewModelWithEvents(): Pair<HomeViewModel, FakeTimeProvider> {
        val calendarRepository = FakeCalendarRepository()
        calendarRepository.setCalendarActive(account, work, active = true)
        calendarRepository.setCalendarActive(account, gym, active = true)
        calendarRepository.replaceEventData(
            calendarId = gym.id, from = today, to = today,
            segments = listOf(EventSegment(gym.id, 1, today, at("11:30"), at("12:00"), "Gym")),
            dailyMinutes = emptyList(),
        )
        calendarRepository.replaceEventData(
            calendarId = work.id, from = today, to = today,
            segments = listOf(EventSegment(work.id, 2, today, at("14:00"), at("17:00"), "Work")),
            dailyMinutes = emptyList(),
        )
        val timeProvider = FakeTimeProvider(now)
        val viewModel = HomeViewModel(
            FakePermissionRepository(PermissionStatus.Granted),
            calendarRepository,
            FakeKeyValueRepository(),
            timeProvider,
            FakeCalendarGroupRepository(),
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return viewModel to timeProvider
    }

    @Test
    fun syncsActiveCalendarsWhenDeviceCalendarsChange() = runTest {
        val calendarRepository = FakeCalendarRepository()
        HomeViewModel(FakePermissionRepository(PermissionStatus.Granted), calendarRepository, FakeKeyValueRepository(), FakeTimeProvider(now), FakeCalendarGroupRepository())
        assertEquals(1, calendarRepository.syncCount)

        calendarRepository.accounts.value = listOf(
            CalendarAccount(id = "LOCAL/Device", name = "Device", calendars = emptyList()),
        )
        assertEquals(2, calendarRepository.syncCount)
    }
}
