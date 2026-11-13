package es.jvbabi.clockme.feature.home.ui

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import es.jvbabi.clockme.R
import es.jvbabi.clockme.ui.theme.ClockMeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.datetime.LocalDate
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

@RunWith(AndroidJUnit4::class)
class HomeContentTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private fun string(@StringRes id: Int, vararg args: Any) = context.getString(id, *args)

    private val now = Instant.parse("2026-11-12T14:00:00Z")
    private val today = LocalDate(2026, 11, 12)
    private val work = CalendarTime(calendarId = 1, name = "Work", color = 0xFF4285F4.toInt(), duration = 90.minutes)
    private val gym = CalendarTime(calendarId = 2, name = "Gym", color = 0xFF0B8043.toInt(), duration = 30.minutes)
    private val standup = EventItem(
        eventId = 10, calendarId = 1, date = today, title = "Standup", color = work.color,
        start = now - 30.minutes, end = now + 30.minutes, duration = 30.minutes, isOngoing = true,
    )
    private val workout = EventItem(
        eventId = 20, calendarId = 2, date = today, title = "Workout", color = gym.color,
        start = now - 120.minutes, end = now - 90.minutes, duration = 30.minutes, isOngoing = false,
    )

    private val selections = mutableListOf<Long?>()
    private val groupSelections = mutableListOf<Long>()
    private var selectionsCleared = 0
    private val shortcuts = mutableListOf<RangeShortcut>()

    private fun setContent(state: HomeUiState) {
        compose.setContent {
            ClockMeTheme(dynamicColor = false) {
                HomeContent(
                    uiState = state,
                    onCalendarSelect = { selections += it },
                    onGroupSelect = { groupSelections += it },
                    onClearSelection = { selectionsCleared++ },
                    onShortcutSelect = { shortcuts += it },
                )
            }
        }
    }

    private val state = HomeUiState(
        shortcuts = RangeShortcut.entries,
        selectedShortcut = RangeShortcut.TODAY,
        range = ResolvedTimeRange(now - 600.minutes, now),
        calendars = listOf(work, gym),
        events = listOf(standup, workout),
    )

    @Test
    fun showsTotalInTheCenterWithoutSelection() {
        setContent(state)

        compose.onNodeWithText(string(R.string.home_total)).assertIsDisplayed()
        compose.onNodeWithContentDescription(string(R.string.duration_hours, 2)).assertIsDisplayed()
    }

    @Test
    fun showsSelectedCalendarInTheCenterAndInTheListTitle() {
        setContent(state.copy(selectedCalendarId = gym.calendarId, events = listOf(workout)))

        compose.onNodeWithText(gym.name).assertIsDisplayed()
        compose.onNodeWithContentDescription(string(R.string.duration_minutes, 30)).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.home_events_of_calendar, gym.name)).assertIsDisplayed()
        compose.onNodeWithText(standup.title).assertDoesNotExist()
    }

    @Test
    fun listsEventsAndMarksOngoingOnes() {
        setContent(state)

        compose.onNodeWithText(standup.title).assertIsDisplayed()
        compose.onNodeWithText(workout.title).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.home_event_ongoing)).assertIsDisplayed()
    }

    @Test
    fun showsHintWithoutEvents() {
        setContent(state.copy(events = emptyList()))

        compose.onNodeWithText(string(R.string.home_no_events)).assertIsDisplayed()
    }

    @Test
    fun showsHintWithoutActiveCalendars() {
        setContent(state.copy(calendars = emptyList(), events = emptyList()))

        compose.onNodeWithText(string(R.string.home_no_active_calendars)).assertIsDisplayed()
    }

    @Test
    fun tappingSlicesSelectsTheirCalendar() {
        setContent(state)
        compose.waitForIdle()

        // Work covers 3/4 of the ring clockwise from the top, gym the last quarter.
        // Ring of 260dp: radius (260 - 28 - 2 * 10) / 2 = 106dp around the center.
        tapRing(angleDegrees = 90f) // right: work
        tapRing(angleDegrees = 315f) // top left: gym
        tapRing(angleDegrees = null) // center: nothing

        assertEquals(listOf<Long?>(work.calendarId, gym.calendarId), selections)
        assertEquals(0, selectionsCleared)
    }

    @Test
    fun tappingBesideTheRingClearsTheSelection() {
        setContent(state.copy(selectedCalendarId = work.calendarId))
        compose.waitForIdle()

        tapRing(angleDegrees = null)

        assertEquals(emptyList<Long?>(), selections)
        assertEquals(1, selectionsCleared)
    }

    private val sport = GroupInfo(id = 7, name = "Sport", color = null)
    private val groupedState = state.copy(
        calendars = listOf(gym.copy(groupId = sport.id), work),
        groups = listOf(sport),
    )

    @Test
    fun tappingTheGroupBandOrNextToItSelectsTheGroup() {
        setContent(groupedState)
        compose.waitForIdle()

        // Gym (grouped) covers the first quarter clockwise from the top, work the rest.
        // With groups the ring of 260dp has radius (260 - 28 - 2 * (10 + 3 + 5 * 1.6 + 8)) / 2 = 87dp,
        // its outer edge lies at 101dp and the band at 104-109dp.
        tapRing(angleDegrees = 45f, radiusDp = 106f) // on the band
        tapRing(angleDegrees = 45f, radiusDp = 114f) // just outside it
        tapRing(angleDegrees = 45f, radiusDp = 87f) // the slice itself
        tapRing(angleDegrees = 180f, radiusDp = 114f) // outside work, which has no group

        assertEquals(listOf(sport.id, sport.id), groupSelections)
        assertEquals(listOf<Long?>(gym.calendarId), selections)
    }

    @Test
    fun showsSelectedGroupInTheCenterAndInTheListTitle() {
        setContent(groupedState.copy(selectedGroupId = sport.id, events = listOf(workout)))

        compose.onNodeWithText(sport.name).assertIsDisplayed()
        compose.onNodeWithContentDescription(string(R.string.duration_minutes, 30)).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.home_events_of_calendar, sport.name)).assertIsDisplayed()
    }

    @Test
    fun backClearsTheGroupSelection() {
        setContent(groupedState.copy(selectedGroupId = sport.id))

        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }

        assertEquals(1, selectionsCleared)
    }

    @Test
    fun showsShortcutsWithTheSelectedOne() {
        setContent(state)

        compose.onNodeWithText(string(R.string.home_shortcut_today)).assertIsSelected()
        compose.onNodeWithText(string(R.string.home_shortcut_yesterday)).assertIsNotSelected()
        // The row scrolls, the last one may be off screen
        compose.onNodeWithText(string(R.string.home_shortcut_all)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun tappingShortcutSelectsIt() {
        setContent(state)

        compose.onNodeWithText(string(R.string.home_shortcut_yesterday)).performClick()

        assertEquals(listOf(RangeShortcut.YESTERDAY), shortcuts)
    }

    @Test
    fun lastSyncIsShownBrieflyAboveTheBar() {
        compose.mainClock.autoAdvance = false
        setContent(state.copy(lastEventSync = now))
        val hint = string(R.string.home_last_event_sync, "").trim()

        compose.mainClock.advanceTimeBy(1_000)
        compose.onNodeWithText(hint, substring = true).assertIsDisplayed()

        compose.mainClock.advanceTimeBy(3_000)
        compose.onNodeWithText(hint, substring = true).assertDoesNotExist()
    }

    @Test
    fun backClearsTheSelection() {
        setContent(state.copy(selectedCalendarId = work.calendarId))

        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }

        assertEquals(1, selectionsCleared)
    }

    @Test
    fun backIsNotHandledWithoutSelection() {
        setContent(state)

        compose.runOnUiThread {
            assertFalse(compose.activity.onBackPressedDispatcher.hasEnabledCallbacks())
        }
    }

    /** Taps the ring at [angleDegrees] clockwise from the top, or its center if `null`. */
    private fun tapRing(angleDegrees: Float?, radiusDp: Float = 106f) {
        compose.onNodeWithTag(HomeTestTags.RING).performTouchInput {
            val center = Offset(width / 2f, height / 2f)
            val position = if (angleDegrees == null) {
                center
            } else {
                val radius = radiusDp.dp.toPx()
                val radians = Math.toRadians(angleDegrees.toDouble())
                Offset(
                    center.x + (radius * kotlin.math.sin(radians)).toFloat(),
                    center.y - (radius * kotlin.math.cos(radians)).toFloat(),
                )
            }
            click(position)
        }
    }
}
