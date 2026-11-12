package es.jvbabi.clockme.feature.onboarding.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

class OnboardingRequestPermissionViewModel(
    private val permissionRepository: PermissionRepository,
) : ViewModel() {

    private val calendarPermission = permissionRepository.observe(AppPermission.READ_CALENDAR)

    val uiState: StateFlow<OnboardingRequestPermissionUiState> = calendarPermission
        .map { OnboardingRequestPermissionUiState(calendarPermission = it) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            OnboardingRequestPermissionUiState(),
        )

    private val _events = Channel<OnboardingRequestPermissionEvent>(Channel.BUFFERED)
    val events: Flow<OnboardingRequestPermissionEvent> = _events.receiveAsFlow()

    init {
        // Covers the dialog as well as returning from the app settings
        viewModelScope.launch {
            calendarPermission.first { it == PermissionStatus.Granted }
            _events.send(OnboardingRequestPermissionEvent.NavigateToHome)
        }
    }

    fun onGrantPermissionClick() {
        when (uiState.value.calendarPermission) {
            PermissionStatus.NotRequested, PermissionStatus.Denied -> viewModelScope.launch {
                permissionRepository.requestViaDialog(AppPermission.READ_CALENDAR)
            }
            PermissionStatus.PermanentlyDenied -> viewModelScope.launch {
                _events.send(OnboardingRequestPermissionEvent.ShowAppSettingsHint)
                permissionRepository.openAppSettings()
            }
            // Navigation to home is triggered by the observer in init
            PermissionStatus.Granted, null -> Unit
        }
    }
}
