package es.jvbabi.clockme.feature.calendars.ui

import es.jvbabi.clockme.data.calendar.Calendar
import es.jvbabi.clockme.data.calendar.CalendarAccount
import es.jvbabi.clockme.testing.FakeCalendarRepository
import es.jvbabi.clockme.testing.MainDispatcherRule
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CalendarsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val calendar = Calendar(id = 1, name = "Personal", color = 0xFF0000FF.toInt())
    private val account = CalendarAccount(id = "LOCAL/Device", name = "Device", calendars = listOf(calendar))

    /** Keeps uiState subscribed like the screen does. */
    private fun TestScope.subscribe(viewModel: CalendarsViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    }

    @Test
    fun exposesCalendarAccounts() = runTest {
        val calendarRepository = FakeCalendarRepository(listOf(account))
        val viewModel = CalendarsViewModel(calendarRepository)
        subscribe(viewModel)

        assertEquals(listOf(account), viewModel.uiState.value.accounts)

        calendarRepository.accounts.value = emptyList()
        assertEquals(emptyList<CalendarAccount>(), viewModel.uiState.value.accounts)
    }

    @Test
    fun clickingCalendarTogglesIt() = runTest {
        val calendarRepository = FakeCalendarRepository(listOf(account))
        val viewModel = CalendarsViewModel(calendarRepository)
        subscribe(viewModel)

        viewModel.onCalendarClick(account, calendar)
        assertEquals(setOf(1L), viewModel.uiState.value.activeCalendarIds)
        assertEquals(listOf(account), calendarRepository.activeCalendars.value)

        viewModel.onCalendarClick(account, calendar)
        assertEquals(emptySet<Long>(), viewModel.uiState.value.activeCalendarIds)
        assertEquals(emptyList<CalendarAccount>(), calendarRepository.activeCalendars.value)
    }
}
