package es.jvbabi.clockme.feature.calendars.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.jvbabi.clockme.data.calendar.Calendar
import es.jvbabi.clockme.data.calendar.CalendarAccount
import es.jvbabi.clockme.data.calendar.CalendarRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CalendarsViewModel(
    private val calendarRepository: CalendarRepository,
) : ViewModel() {

    val uiState: StateFlow<CalendarsUiState> = combine(
        calendarRepository.observeAccounts(),
        calendarRepository.observeActiveCalendars(),
    ) { accounts, activeAccounts ->
        CalendarsUiState(
            accounts = accounts,
            activeCalendarIds = activeAccounts.flatMap { it.calendars }.map { it.id }.toSet(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarsUiState())

    fun onCalendarClick(account: CalendarAccount, calendar: Calendar) {
        val active = calendar.id !in uiState.value.activeCalendarIds
        viewModelScope.launch { calendarRepository.setCalendarActive(account, calendar, active) }
    }
}
