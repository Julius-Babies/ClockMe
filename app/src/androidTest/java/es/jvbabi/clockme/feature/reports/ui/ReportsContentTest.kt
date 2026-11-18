package es.jvbabi.clockme.feature.reports.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import es.jvbabi.clockme.R
import es.jvbabi.clockme.data.calendar.Calendar
import es.jvbabi.clockme.ui.theme.CirclesTheme
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import kotlin.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReportsContentTest {

    @get:Rule
    val compose = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private fun string(id: Int) = context.getString(id)

    private val actions = mutableListOf<ReportsAction>()

    private val lectures = Calendar(id = 1, name = "Lectures", color = 0xFF4285F4.toInt())
    private val study = Calendar(id = 2, name = "Study", color = 0xFF0B8043.toInt())
    private val gym = Calendar(id = 3, name = "Gym", color = 0xFFF4511E.toInt())
    private val tree = ReportCalendarTree(
        groups = listOf(ReportGroup(id = 10, name = "University", color = null, calendars = listOf(lectures, study))),
        ungrouped = listOf(gym),
    )

    private fun setContent(
        from: String = "2026-11-18T08:15",
        to: String = "2026-11-18T14:30",
        deselected: Set<Long> = emptySet(),
    ) {
        compose.setContent {
            CirclesTheme(dynamicColor = false) {
                ReportsContent(
                    uiState = ReportsUiState(
                        from = LocalDateTime.parse(from),
                        to = LocalDateTime.parse(to),
                        calendars = tree,
                        deselectedCalendarIds = deselected,
                        preview = ReportPreview(
                            calendars = listOf(lectures, study, gym)
                                .filter { it.id !in deselected }
                                .map { PreviewCalendar(it.id, it.color, Duration.ZERO) },
                            eventCount = 0,
                        ),
                    ),
                    onAction = { actions += it },
                )
            }
        }
    }

    @Test
    fun confirmingTheTimePickerKeepsTheShownTime() {
        setContent()

        compose.onNodeWithTag(ReportsTestTags.FROM_TIME).performClick()
        compose.onNodeWithText(string(R.string.dialog_ok)).performClick()

        assertEquals(listOf(ReportsAction.FromTimeChange(LocalTime(8, 15))), actions)
    }

    @Test
    fun aPickedDateLeadsOnToItsTime() {
        setContent()

        compose.onNodeWithTag(ReportsTestTags.TO_DATE).performClick()
        compose.onNodeWithText(string(R.string.dialog_ok)).performClick()
        compose.onNodeWithText(string(R.string.dialog_ok)).performClick()

        assertEquals(
            listOf(ReportsAction.ToDateChange(LocalDate(2026, 11, 18)), ReportsAction.ToTimeChange(LocalTime(14, 30))),
            actions,
        )
    }

    @Test
    fun cancellingTheTimeAfterADateKeepsTheDate() {
        setContent()

        compose.onNodeWithTag(ReportsTestTags.FROM_DATE).performClick()
        compose.onNodeWithText(string(R.string.dialog_ok)).performClick()
        compose.onNodeWithText(string(R.string.dialog_cancel)).performClick()

        assertEquals(listOf(ReportsAction.FromDateChange(LocalDate(2026, 11, 18))), actions)
        compose.onNodeWithText(string(R.string.dialog_ok)).assertDoesNotExist()
    }

    @Test
    fun cancellingAPickerChangesNothing() {
        setContent()

        compose.onNodeWithTag(ReportsTestTags.TO_TIME).performClick()
        compose.onNodeWithText(string(R.string.dialog_cancel)).performClick()

        assertTrue(actions.isEmpty())
    }

    @Test
    fun warnsWhenTheEndIsBeforeTheStart() {
        setContent(from = "2026-11-19T08:00", to = "2026-11-18T08:00")

        compose.onNodeWithText(string(R.string.reports_range_invalid)).assertIsDisplayed()
    }

    @Test
    fun groupCalendarsShowOnlyOnceTheGroupIsOpened() {
        setContent()

        compose.onNodeWithTag(ReportsTestTags.calendar(gym.id)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag(ReportsTestTags.calendar(lectures.id)).assertDoesNotExist()

        compose.onNodeWithTag(ReportsTestTags.group(10)).performScrollTo().performClick()

        compose.onNodeWithTag(ReportsTestTags.calendar(lectures.id)).performScrollTo().assertIsDisplayed()
        assertTrue(actions.isEmpty())
    }

    @Test
    fun aPartlySelectedGroupShowsAnIndeterminateBox() {
        setContent(deselected = setOf(study.id))

        compose.onNodeWithTag(ReportsTestTags.groupCheckbox(10)).performScrollTo()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.Indeterminate))
        compose.onNodeWithText(context.getString(R.string.reports_group_count, 1, 2)).assertIsDisplayed()
    }

    @Test
    fun tappingTheGroupBoxTogglesTheGroup() {
        setContent()

        compose.onNodeWithTag(ReportsTestTags.groupCheckbox(10)).performScrollTo().assertIsOn().performClick()

        assertEquals(listOf(ReportsAction.GroupToggle(10)), actions)
    }

    @Test
    fun tappingACalendarTogglesIt() {
        setContent(deselected = setOf(gym.id))

        compose.onNodeWithTag(ReportsTestTags.calendar(gym.id)).performScrollTo().assertIsOff().performClick()

        assertEquals(listOf(ReportsAction.CalendarToggle(gym.id)), actions)
    }

    @Test
    fun tappingTheEventsOptionTogglesIt() {
        setContent()

        compose.onNodeWithTag(ReportsTestTags.INCLUDE_EVENTS).performScrollTo().assertIsOn().performClick()

        assertEquals(listOf<ReportsAction>(ReportsAction.IncludeEventsToggle), actions)
    }

    @Test
    fun tappingCreateCreatesTheReport() {
        setContent()

        compose.onNodeWithTag(ReportsTestTags.CREATE).performScrollTo().assertIsEnabled().performClick()

        assertEquals(listOf<ReportsAction>(ReportsAction.Create), actions)
    }

    @Test
    fun noReportWithoutAnyCalendar() {
        setContent(deselected = setOf(lectures.id, study.id, gym.id))

        compose.onNodeWithTag(ReportsTestTags.CREATE).performScrollTo().assertIsNotEnabled()
    }
}
