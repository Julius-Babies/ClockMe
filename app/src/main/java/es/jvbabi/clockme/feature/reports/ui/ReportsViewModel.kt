package es.jvbabi.clockme.feature.reports.ui

import androidx.lifecycle.ViewModel
import es.jvbabi.clockme.core.time.TimeProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.toLocalDateTime

class ReportsViewModel(timeProvider: TimeProvider) : ViewModel() {

    private val _uiState = MutableStateFlow(initialState(timeProvider))
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    fun onAction(action: ReportsAction) {
        _uiState.update { state ->
            when (action) {
                is ReportsAction.FromDateChange -> state.copy(from = LocalDateTime(action.date, state.from.time))
                is ReportsAction.FromTimeChange -> state.copy(from = LocalDateTime(state.from.date, action.time))
                is ReportsAction.ToDateChange -> state.copy(to = LocalDateTime(action.date, state.to.time))
                is ReportsAction.ToTimeChange -> state.copy(to = LocalDateTime(state.to.date, action.time))
            }
        }
    }

    private companion object {
        /** Today so far: from midnight up to the current minute. */
        fun initialState(timeProvider: TimeProvider): ReportsUiState {
            val now = timeProvider.now().toLocalDateTime(timeProvider.timeZone())
            return ReportsUiState(
                from = LocalDateTime(now.date, LocalTime(0, 0)),
                to = LocalDateTime(now.date, LocalTime(now.hour, now.minute)),
            )
        }
    }
}
