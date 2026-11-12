package es.jvbabi.clockme.core.ui.chart

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DonutChartTest {

    @get:Rule
    val compose = createComposeRule()

    private val clicks = mutableListOf<Long?>()

    private fun setChart() {
        compose.setContent {
            DonutChart(
                // Clockwise from the top: slice 1 covers the right half, slice 2 the left half
                slices = listOf(DonutSlice(1, 30f, Color.Red), DonutSlice(2, 30f, Color.Blue)),
                selectedId = null,
                onSliceClick = { clicks += it },
                // Ring radius: (200 - 28 - 2 * 10) / 2 = 76dp around the center
                modifier = Modifier.size(200.dp).testTag("chart"),
            )
        }
        compose.waitForIdle()
    }

    private fun tapAt(xDp: Float, yDp: Float) {
        compose.onNodeWithTag("chart").performTouchInput {
            click(Offset(xDp.dp.toPx(), yDp.dp.toPx()))
        }
    }

    @Test
    fun tappingASliceReportsItsId() {
        setChart()

        tapAt(176f, 100f) // right
        tapAt(24f, 100f) // left

        assertEquals(listOf<Long?>(1, 2), clicks)
    }

    @Test
    fun tappingBesideTheRingReportsNull() {
        setChart()

        tapAt(100f, 100f) // center
        tapAt(2f, 2f) // corner

        assertEquals(listOf<Long?>(null, null), clicks)
    }
}
