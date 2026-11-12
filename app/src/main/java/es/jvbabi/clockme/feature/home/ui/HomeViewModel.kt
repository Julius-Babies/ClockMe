package es.jvbabi.clockme.feature.home.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.jvbabi.clockme.data.calendar.Calendar
import es.jvbabi.clockme.data.calendar.CalendarAccount
import es.jvbabi.clockme.data.calendar.CalendarRepository
import es.jvbabi.clockme.data.permission.AppPermission
import es.jvbabi.clockme.data.permission.PermissionRepository
import es.jvbabi.clockme.data.permission.PermissionStatus
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    permissionRepository: PermissionRepository,
    private val calendarRepository: CalendarRepository,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        calendarRepository.observeAccounts(),
        calendarRepository.observeActiveCalendars(),
    ) { accounts, activeAccounts ->
        HomeUiState(
            accounts = accounts,
            activeCalendarIds = activeAccounts.flatMap { it.calendars }.map { it.id }.toSet(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private val _events = Channel<HomeEvent>(Channel.BUFFERED)
    val events: Flow<HomeEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            permissionRepository.observe(AppPermission.READ_CALENDAR)
                .first { it != PermissionStatus.Granted }
            _events.send(HomeEvent.NavigateToOnboarding)
        }
        // Drop active calendars that were deleted on the device, whenever the device calendars change
        viewModelScope.launch {
            calendarRepository.observeAccounts().collect { calendarRepository.syncActiveCalendars() }
        }
    }

    fun onCalendarClick(account: CalendarAccount, calendar: Calendar) {
        val active = calendar.id !in uiState.value.activeCalendarIds
        viewModelScope.launch { calendarRepository.setCalendarActive(account, calendar, active) }
    }
}
