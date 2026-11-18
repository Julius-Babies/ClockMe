package es.jvbabi.clockme.feature.reports.ui

import es.jvbabi.clockme.testing.FakeTimeProvider
import es.jvbabi.clockme.testing.MainDispatcherRule
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ReportsViewModelTest {

    private val berlin = TimeZone.of("Europe/Berlin")
    private val now = LocalDateTime.parse("2026-11-18T14:37:52").toInstant(berlin)

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel() = ReportsViewModel(FakeTimeProvider(now, berlin))

    @Test
    fun startsWithTodaySoFar() {
        val state = viewModel().uiState.value

        assertEquals(LocalDateTime.parse("2026-11-18T00:00"), state.from)
        assertEquals(LocalDateTime.parse("2026-11-18T14:37"), state.to)
        assertTrue(state.isRangeValid)
    }

    @Test
    fun changingADateKeepsItsTime() {
        val viewModel = viewModel()

        viewModel.onAction(ReportsAction.FromDateChange(LocalDate.parse("2026-11-01")))
        viewModel.onAction(ReportsAction.ToDateChange(LocalDate.parse("2026-11-20")))

        assertEquals(LocalDateTime.parse("2026-11-01T00:00"), viewModel.uiState.value.from)
        assertEquals(LocalDateTime.parse("2026-11-20T14:37"), viewModel.uiState.value.to)
    }

    @Test
    fun changingATimeKeepsItsDate() {
        val viewModel = viewModel()

        viewModel.onAction(ReportsAction.FromTimeChange(LocalTime(8, 15)))
        viewModel.onAction(ReportsAction.ToTimeChange(LocalTime(17, 45)))

        assertEquals(LocalDateTime.parse("2026-11-18T08:15"), viewModel.uiState.value.from)
        assertEquals(LocalDateTime.parse("2026-11-18T17:45"), viewModel.uiState.value.to)
    }

    @Test
    fun anEndBeforeTheStartIsInvalid() {
        val viewModel = viewModel()

        viewModel.onAction(ReportsAction.FromDateChange(LocalDate.parse("2026-11-19")))

        assertFalse(viewModel.uiState.value.isRangeValid)
    }
}
