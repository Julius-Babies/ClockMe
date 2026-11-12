package es.jvbabi.clockme.feature.onboarding.ui

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import es.jvbabi.clockme.R
import es.jvbabi.clockme.data.permission.PermissionStatus
import es.jvbabi.clockme.ui.theme.ClockMeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingRequestPermissionContentTest {

    @get:Rule
    val compose = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private var clicks = 0

    private fun setContent(status: PermissionStatus?) {
        compose.setContent {
            ClockMeTheme(dynamicColor = false) {
                OnboardingRequestPermissionContent(
                    uiState = OnboardingRequestPermissionUiState(calendarPermission = status),
                    onGrantPermissionClick = { clicks++ },
                )
            }
        }
    }

    @Test
    fun explainsWhyThePermissionIsNeeded() {
        setContent(PermissionStatus.NotRequested)

        val appName = context.getString(R.string.app_name)
        compose.onNodeWithText(context.getString(R.string.onboarding_calendar_permission_description, appName))
            .assertIsDisplayed()
    }

    @Test
    fun offersTheDialogWhileItCanBeShown() {
        listOf(PermissionStatus.NotRequested, PermissionStatus.Denied).forEach { status ->
            clicks = 0
            setContentOnce(status)
            compose.onNodeWithText(context.getString(R.string.permission_action_request)).performClick()
            assertEquals(1, clicks)
        }
    }

    @Test
    fun offersTheSettingsWhenPermanentlyDenied() {
        setContent(PermissionStatus.PermanentlyDenied)

        compose.onNodeWithText(context.getString(R.string.permission_action_open_settings)).performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun buttonIsDisabledWhileLoading() {
        setContent(null)

        compose.onNodeWithText(context.getString(R.string.permission_action_request)).assertIsNotEnabled()
    }

    /** setContent may only be called once per test, so the state is switched within the content. */
    private var statusState: MutableState<PermissionStatus?>? = null

    private fun setContentOnce(status: PermissionStatus) {
        val state = statusState
        if (state == null) {
            val newState = mutableStateOf<PermissionStatus?>(status)
            statusState = newState
            compose.setContent {
                ClockMeTheme(dynamicColor = false) {
                    OnboardingRequestPermissionContent(
                        uiState = OnboardingRequestPermissionUiState(calendarPermission = newState.value),
                        onGrantPermissionClick = { clicks++ },
                    )
                }
            }
        } else {
            state.value = status
        }
        compose.waitForIdle()
    }
}
