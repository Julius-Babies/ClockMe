package es.jvbabi.clockme.feature.reports.ui

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime

/** The span a report covers, from [from] up to [to], both in the device's time zone. */
data class ReportsUiState(
    val from: LocalDateTime,
    val to: LocalDateTime,
) {
    /** A report needs its end after its start. */
    val isRangeValid: Boolean get() = from < to
}

sealed interface ReportsAction {
    data class FromDateChange(val date: LocalDate) : ReportsAction
    data class FromTimeChange(val time: LocalTime) : ReportsAction
    data class ToDateChange(val date: LocalDate) : ReportsAction
    data class ToTimeChange(val time: LocalTime) : ReportsAction
}
