package es.jvbabi.clockme.feature.home.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.jvbabi.clockme.data.calendar.CalendarRepository
import es.jvbabi.clockme.data.permission.AppPermission
import es.jvbabi.clockme.data.permission.PermissionRepository
import es.jvbabi.clockme.data.permission.PermissionStatus
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Home is the root of the app and stays on the back stack while other tabs are shown,
 * so it takes care of app-wide checks: the calendar permission and syncing active calendars.
 */
class HomeViewModel(
    permissionRepository: PermissionRepository,
    private val calendarRepository: CalendarRepository,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = calendarRepository
        .observeActiveCalendars()
        .map { accounts -> HomeUiState(activeCalendarCount = accounts.sumOf { it.calendars.size }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

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
}
