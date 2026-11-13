package es.jvbabi.clockme.feature.calendars.ui

import es.jvbabi.clockme.data.calendar.Calendar
import es.jvbabi.clockme.data.calendar.CalendarAccount
import es.jvbabi.clockme.data.group.CalendarGroup
import es.jvbabi.clockme.testing.FakeCalendarGroupRepository
import es.jvbabi.clockme.testing.FakeCalendarRepository
import es.jvbabi.clockme.testing.MainDispatcherRule
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CalendarsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val calendar = Calendar(id = 1, name = "Personal", color = 0xFF0000FF.toInt())
    private val work = Calendar(id = 2, name = "Work", color = 0xFF00FF00.toInt())
    private val account = CalendarAccount(id = "LOCAL/Device", name = "Device", calendars = listOf(calendar, work))

    private val calendarRepository = FakeCalendarRepository(listOf(account))
    private val groupRepository = FakeCalendarGroupRepository()

    /** Keeps uiState subscribed like the screen does. */
    private fun TestScope.createViewModel(): CalendarsViewModel {
        val viewModel = CalendarsViewModel(calendarRepository, groupRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return viewModel
    }

    @Test
    fun exposesCalendarAccounts() = runTest {
        val viewModel = createViewModel()

        assertEquals(listOf(account), viewModel.uiState.value.accounts)

        calendarRepository.accounts.value = emptyList()
        assertEquals(emptyList<CalendarAccount>(), viewModel.uiState.value.accounts)
    }

    @Test
    fun clickingCalendarTogglesIt() = runTest {
        val viewModel = createViewModel()

        viewModel.onAction(CalendarsAction.CalendarClick(account, calendar))
        assertEquals(setOf(1L), viewModel.uiState.value.activeCalendarIds)

        viewModel.onAction(CalendarsAction.CalendarClick(account, calendar))
        assertEquals(emptySet<Long>(), viewModel.uiState.value.activeCalendarIds)
    }

    @Test
    fun createsGroupWithNameColorAndCalendars() = runTest {
        val viewModel = createViewModel()

        viewModel.onAction(CalendarsAction.CreateGroup)
        assertTrue(viewModel.uiState.value.groupEditor!!.isNew)
        viewModel.onAction(CalendarsAction.EditorNameChange("University"))
        viewModel.onAction(CalendarsAction.EditorColorChange(GROUP_COLORS[2]))
        viewModel.onAction(CalendarsAction.EditorCalendarToggle(calendar.id))
        viewModel.onAction(CalendarsAction.EditorCalendarToggle(work.id))
        viewModel.onAction(CalendarsAction.EditorCalendarToggle(work.id))
        viewModel.onAction(CalendarsAction.EditorSave)

        assertNull(viewModel.uiState.value.groupEditor)
        assertEquals(
            listOf(CalendarGroup(id = 1, name = "University", color = GROUP_COLORS[2], calendarIds = setOf(calendar.id))),
            viewModel.uiState.value.groups,
        )
    }

    @Test
    fun groupWithoutNameCantBeSaved() = runTest {
        val viewModel = createViewModel()
        viewModel.onAction(CalendarsAction.CreateGroup)
        viewModel.onAction(CalendarsAction.EditorNameChange("  "))

        assertFalse(viewModel.uiState.value.groupEditor!!.canSave)
        viewModel.onAction(CalendarsAction.EditorSave)

        assertEquals(emptyList<CalendarGroup>(), viewModel.uiState.value.groups)
    }

    @Test
    fun editsExistingGroup() = runTest {
        val id = groupRepository.createGroup("Uni", null, setOf(calendar.id))
        val viewModel = createViewModel()
        val group = viewModel.uiState.value.groups.single()

        viewModel.onAction(CalendarsAction.EditGroup(group))
        assertEquals(GroupEditorState(id, "Uni", null, setOf(calendar.id)), viewModel.uiState.value.groupEditor)

        viewModel.onAction(CalendarsAction.EditorNameChange("University"))
        viewModel.onAction(CalendarsAction.EditorCalendarToggle(calendar.id))
        viewModel.onAction(CalendarsAction.EditorCalendarToggle(work.id))
        viewModel.onAction(CalendarsAction.EditorSave)

        assertEquals(listOf(CalendarGroup(id, "University", null, setOf(work.id))), viewModel.uiState.value.groups)
    }

    @Test
    fun addingCalendarToAnotherGroupMovesIt() = runTest {
        val uni = groupRepository.createGroup("Uni", null, setOf(calendar.id))
        val viewModel = createViewModel()

        viewModel.onAction(CalendarsAction.CreateGroup)
        viewModel.onAction(CalendarsAction.EditorNameChange("Sports"))
        viewModel.onAction(CalendarsAction.EditorCalendarToggle(calendar.id))
        viewModel.onAction(CalendarsAction.EditorSave)

        val groups = viewModel.uiState.value.groups.associateBy { it.name }
        assertEquals(emptySet<Long>(), groups.getValue("Uni").calendarIds)
        assertEquals(setOf(calendar.id), groups.getValue("Sports").calendarIds)
        assertEquals(uni, groups.getValue("Uni").id)
    }

    @Test
    fun onlyActiveCalendarsCanBeGrouped() = runTest {
        val viewModel = createViewModel()
        viewModel.onAction(CalendarsAction.CalendarClick(account, work))

        assertEquals(listOf(account.copy(calendars = listOf(work))), viewModel.uiState.value.activeAccounts)
    }

    @Test
    fun deletesGroup() = runTest {
        groupRepository.createGroup("Uni", null, emptySet())
        val viewModel = createViewModel()

        viewModel.onAction(CalendarsAction.EditGroup(viewModel.uiState.value.groups.single()))
        viewModel.onAction(CalendarsAction.EditorDelete)

        assertEquals(emptyList<CalendarGroup>(), viewModel.uiState.value.groups)
        assertNull(viewModel.uiState.value.groupEditor)
    }

    @Test
    fun dismissingEditorDiscardsChanges() = runTest {
        groupRepository.createGroup("Uni", null, emptySet())
        val viewModel = createViewModel()

        viewModel.onAction(CalendarsAction.EditGroup(viewModel.uiState.value.groups.single()))
        viewModel.onAction(CalendarsAction.EditorNameChange("Something else"))
        viewModel.onAction(CalendarsAction.EditorDismiss)

        assertNull(viewModel.uiState.value.groupEditor)
        assertEquals("Uni", viewModel.uiState.value.groups.single().name)
    }
}
