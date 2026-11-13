package es.jvbabi.clockme.feature.calendars.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasScrollAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import es.jvbabi.clockme.R
import es.jvbabi.clockme.data.calendar.Calendar
import es.jvbabi.clockme.data.calendar.CalendarAccount
import es.jvbabi.clockme.data.group.CalendarGroup
import es.jvbabi.clockme.ui.theme.ClockMeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CalendarsContentTest {

    @get:Rule
    val compose = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private fun string(id: Int) = context.getString(id)

    private val work = Calendar(id = 1, name = "Work", color = 0xFF4285F4.toInt())
    private val personal = Calendar(id = 2, name = "Personal", color = 0xFF0B8043.toInt())
    private val account = CalendarAccount(id = "LOCAL/jane", name = "jane@example.com", calendars = listOf(personal, work))
    private val group = CalendarGroup(id = 7, name = "University", color = GROUP_COLORS[0], calendarIds = setOf(work.id))

    private val actions = mutableListOf<CalendarsAction>()

    private fun setContent(state: CalendarsUiState) {
        compose.setContent {
            ClockMeTheme(dynamicColor = false) {
                CalendarsContent(uiState = state, onAction = { actions += it })
            }
        }
    }

    private fun scrollTo(text: String) {
        compose.onNode(hasScrollAction()).performScrollToNode(hasText(text))
    }

    @Test
    fun showsCalendarsGroupedByAccountWithTheirState() {
        setContent(CalendarsUiState(accounts = listOf(account), activeCalendarIds = setOf(work.id)))

        compose.onNodeWithText(account.name).assertIsDisplayed()
        compose.onNodeWithText(work.name).assertIsOn()
        compose.onNodeWithText(personal.name).assertIsOff()
    }

    @Test
    fun tappingCalendarReportsItWithItsAccount() {
        setContent(CalendarsUiState(accounts = listOf(account)))

        compose.onNodeWithText(personal.name).performClick()

        assertEquals(listOf<CalendarsAction>(CalendarsAction.CalendarClick(account, personal)), actions)
    }

    @Test
    fun showsHintWithoutCalendars() {
        setContent(CalendarsUiState(accounts = emptyList()))

        compose.onNodeWithText(string(R.string.calendars_empty)).assertIsDisplayed()
    }

    @Test
    fun showsGroupsBelowTheCalendarsWithTheirCalendars() {
        setContent(CalendarsUiState(accounts = listOf(account), activeCalendarIds = setOf(work.id), groups = listOf(group)))

        scrollTo(group.name)
        compose.onNodeWithText(group.name).assertIsDisplayed()
        // Once in the list of calendars, once in the group
        assertEquals(2, compose.onAllNodesWithText(work.name).fetchSemanticsNodes().size)
    }

    @Test
    fun showsHintWithoutGroups() {
        setContent(CalendarsUiState(accounts = listOf(account)))

        scrollTo(string(R.string.groups_empty))
        compose.onNodeWithText(string(R.string.groups_empty)).assertIsDisplayed()
    }

    @Test
    fun creatingAndEditingGroupsIsRequested() {
        setContent(CalendarsUiState(accounts = listOf(account), groups = listOf(group)))

        scrollTo(string(R.string.groups_create))
        compose.onNodeWithText(string(R.string.groups_create)).performClick()
        scrollTo(group.name)
        compose.onNodeWithText(group.name).performClick()

        assertEquals(listOf(CalendarsAction.CreateGroup, CalendarsAction.EditGroup(group)), actions)
    }

    @Test
    fun newGroupNeedsANameAndCantBeDeleted() {
        setContent(CalendarsUiState(accounts = listOf(account), groupEditor = GroupEditorState()))

        compose.onNodeWithText(string(R.string.groups_save)).assertIsNotEnabled()
        compose.onNodeWithText(string(R.string.groups_delete)).assertDoesNotExist()
    }

    @Test
    fun editorOffersOnlyActiveCalendarsAndTellsWhichAreInAnotherGroup() {
        setContent(
            CalendarsUiState(
                accounts = listOf(account),
                activeCalendarIds = setOf(work.id),
                groups = listOf(group),
                groupEditor = GroupEditorState(),
            ),
        )

        // Personal is inactive: only in the calendar list above, not in the sheet
        assertEquals(1, compose.onAllNodesWithText(personal.name).fetchSemanticsNodes().size)
        compose.onNodeWithText(context.getString(R.string.groups_calendar_in_other_group, group.name)).assertIsDisplayed()
    }

    @Test
    fun editorShowsTheGroupAndReportsChanges() {
        setContent(
            CalendarsUiState(
                accounts = listOf(account),
                activeCalendarIds = setOf(work.id, personal.id),
                groups = listOf(group),
                groupEditor = GroupEditorState(group.id, group.name, group.color, group.calendarIds),
            ),
        )

        compose.onNodeWithText(string(R.string.groups_edit)).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.groups_save)).assertIsEnabled()
        // The calendars in the sheet are the last ones with these names
        compose.onAllNodesWithText(personal.name).fetchSemanticsNodes().size.let { count -> assertTrue(count >= 2) }
        compose.onAllNodesWithText(personal.name)[compose.onAllNodesWithText(personal.name).fetchSemanticsNodes().size - 1]
            .assertIsOff()
            .performClick()
        compose.onNodeWithText(string(R.string.groups_delete)).performClick()

        assertEquals(
            listOf(CalendarsAction.EditorCalendarToggle(personal.id), CalendarsAction.EditorDelete),
            actions,
        )
    }
}
