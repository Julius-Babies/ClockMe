package es.jvbabi.clockme.feature.reports.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import es.jvbabi.clockme.R
import es.jvbabi.clockme.ui.theme.CirclesTheme
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
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

    private fun setContent(
        from: String = "2026-11-18T08:15",
        to: String = "2026-11-18T14:30",
    ) {
        compose.setContent {
            CirclesTheme(dynamicColor = false) {
                ReportsContent(
                    uiState = ReportsUiState(LocalDateTime.parse(from), LocalDateTime.parse(to)),
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
    fun confirmingTheDatePickerKeepsTheShownDate() {
        setContent()

        compose.onNodeWithTag(ReportsTestTags.TO_DATE).performClick()
        compose.onNodeWithText(string(R.string.dialog_ok)).performClick()

        assertEquals(listOf(ReportsAction.ToDateChange(LocalDate(2026, 11, 18))), actions)
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
}
