package es.jvbabi.clockme.feature.home.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.jvbabi.clockme.data.permission.AppPermission
import es.jvbabi.clockme.data.permission.PermissionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val permissionRepository: PermissionRepository,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = permissionRepository
        .observe(AppPermission.READ_CALENDAR)
        .map { HomeUiState(calendarPermission = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun onRequestCalendarPermission() {
        viewModelScope.launch { permissionRepository.requestViaDialog(AppPermission.READ_CALENDAR) }
    }

    fun onOpenAppSettings() = permissionRepository.openAppSettings()
}
