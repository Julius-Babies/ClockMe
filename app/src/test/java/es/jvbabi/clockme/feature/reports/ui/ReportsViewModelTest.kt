package es.jvbabi.clockme.feature.reports.ui

import es.jvbabi.clockme.data.calendar.Calendar
import es.jvbabi.clockme.data.calendar.CalendarAccount
import es.jvbabi.clockme.data.group.CalendarGroup
import es.jvbabi.clockme.testing.FakeCalendarGroupRepository
import es.jvbabi.clockme.testing.FakeCalendarRepository
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

    private val lectures = Calendar(id = 1, name = "Lectures", color = 1)
    private val study = Calendar(id = 2, name = "Study", color = 2)
    private val gym = Calendar(id = 3, name = "Gym", color = 3)
    private val inactive = Calendar(id = 4, name = "Inactive", color = 4)
    private val university = CalendarGroup(id = 10, name = "University", color = null, calendarIds = setOf(lectures.id, study.id))
    private val empty = CalendarGroup(id = 11, name = "Empty", color = null, calendarIds = setOf(inactive.id))

    private val calendarRepository = FakeCalendarRepository().apply {
        activeCalendars.value = listOf(CalendarAccount(id = "LOCAL/Device", name = "Device", calendars = listOf(study, gym, lectures)))
    }
    private val groupRepository = FakeCalendarGroupRepository().apply { groups.value = listOf(university, empty) }

    private fun viewModel() = ReportsViewModel(FakeTimeProvider(now, berlin), calendarRepository, groupRepository)

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

    @Test
    fun treeHasGroupsWithTheirActiveCalendarsThenTheUngroupedOnes() {
        val tree = viewModel().uiState.value.calendars

        assertEquals(
            ReportCalendarTree(
                groups = listOf(ReportGroup(university.id, university.name, null, listOf(lectures, study))),
                ungrouped = listOf(gym),
            ),
            tree,
        )
    }

    @Test
    fun allCalendarsAreSelectedAtFirst() {
        val state = viewModel().uiState.value

        assertTrue(listOf(lectures, study, gym).all(state::isSelected))
        assertEquals(GroupSelection.ALL, state.selectionOf(state.calendars!!.groups.single()))
    }

    @Test
    fun leavingOutOneCalendarOfAGroupSelectsItPartly() {
        val viewModel = viewModel()

        viewModel.onAction(ReportsAction.CalendarToggle(study.id))

        val state = viewModel.uiState.value
        assertFalse(state.isSelected(study))
        assertEquals(GroupSelection.SOME, state.selectionOf(state.calendars!!.groups.single()))
    }

    @Test
    fun togglingAPartlySelectedGroupTakesInAllItsCalendars() {
        val viewModel = viewModel()
        viewModel.onAction(ReportsAction.CalendarToggle(study.id))

        viewModel.onAction(ReportsAction.GroupToggle(university.id))

        val state = viewModel.uiState.value
        assertEquals(GroupSelection.ALL, state.selectionOf(state.calendars!!.groups.single()))
    }

    @Test
    fun togglingAFullySelectedGroupLeavesOutOnlyItsCalendars() {
        val viewModel = viewModel()

        viewModel.onAction(ReportsAction.GroupToggle(university.id))

        val state = viewModel.uiState.value
        assertEquals(GroupSelection.NONE, state.selectionOf(state.calendars!!.groups.single()))
        assertTrue(state.isSelected(gym))
    }

    @Test
    fun aNewlyActivatedCalendarIsSelected() {
        val viewModel = viewModel()
        val work = Calendar(id = 5, name = "Work", color = 5)

        calendarRepository.activeCalendars.value = listOf(
            CalendarAccount(id = "LOCAL/Device", name = "Device", calendars = listOf(study, gym, lectures, work)),
        )

        val state = viewModel.uiState.value
        assertEquals(listOf(gym, work), state.calendars!!.ungrouped)
        assertTrue(state.isSelected(work))
    }
}
