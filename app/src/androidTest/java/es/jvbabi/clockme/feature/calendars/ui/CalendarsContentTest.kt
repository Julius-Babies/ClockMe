package es.jvbabi.clockme.feature.calendars.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import es.jvbabi.clockme.R
import es.jvbabi.clockme.data.calendar.Calendar
import es.jvbabi.clockme.data.calendar.CalendarAccount
import es.jvbabi.clockme.ui.theme.ClockMeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CalendarsContentTest {

    @get:Rule
    val compose = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val work = Calendar(id = 1, name = "Work", color = 0xFF4285F4.toInt())
    private val personal = Calendar(id = 2, name = "Personal", color = 0xFF0B8043.toInt())
    private val account = CalendarAccount(id = "LOCAL/jane", name = "jane@example.com", calendars = listOf(personal, work))

    private val clicks = mutableListOf<Pair<CalendarAccount, Calendar>>()

    private fun setContent(state: CalendarsUiState) {
        compose.setContent {
            ClockMeTheme(dynamicColor = false) {
                CalendarsContent(uiState = state, onCalendarClick = { account, calendar -> clicks += account to calendar })
            }
        }
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

        assertEquals(listOf(account to personal), clicks)
    }

    @Test
    fun showsHintWithoutCalendars() {
        setContent(CalendarsUiState(accounts = emptyList()))

        compose.onNodeWithText(context.getString(R.string.calendars_empty)).assertIsDisplayed()
    }
}
