package es.jvbabi.clockme.core.ui.chart

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import es.jvbabi.clockme.ui.theme.ClockMeTheme
import kotlin.math.atan2
import kotlin.math.hypot

data class DonutSlice(
    val id: Long,
    val value: Float,
    val color: Color,
)

/** Angle the ring starts at before it unfolds, it turns from there to 0°. */
private const val INTRO_ROTATION_DEGREES = -30f
private const val INTRO_MILLIS = 1_000
private const val CHANGE_MILLIS = 600

/** Gap between two slices. */
private const val GAP_DEGREES = 2f

/** Alpha of the slices that are not selected while one is. */
private const val DIMMED_ALPHA = 0.3f

/**
 * A ring chart. Slices are clickable: [onSliceClick] gets the slice's id, or `null` for a tap
 * beside the ring. The [selectedId] slice grows outwards, the others fade.
 *
 * It unfolds clockwise once it is shown while turning into place, and animates any change
 * of the values. [centerContent] is not
 * rotated with the ring but fades in.
 */
@Composable
fun DonutChart(
    slices: List<DonutSlice>,
    selectedId: Long?,
    onSliceClick: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 28.dp,
    selectedExtraWidth: Dp = 10.dp,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    /** How strongly the selection is shown, below 1 while it is being dismissed (e.g. predictive back). */
    selectionStrength: Float = 1f,
    /** The ring stays hidden until this is true, then unfolds; e.g. to wait for a splash screen. */
    playIntro: Boolean = true,
    centerContent: @Composable () -> Unit = {},
) {
    val intro = remember { Animatable(0f) }
    LaunchedEffect(playIntro) {
        if (playIntro) intro.animateTo(1f, tween(INTRO_MILLIS, easing = EaseOutCubic))
    }

    // Without any value the ring retracts and the empty track fades in; the slices keep their
    // last proportions meanwhile, and grow out again from there once there are values.
    val isEmpty = slices.none { it.value > 0f }
    val fill by animateFloatAsState(
        targetValue = if (isEmpty) 0f else 1f,
        animationSpec = tween(CHANGE_MILLIS, easing = FastOutSlowInEasing),
        label = "fill",
    )
    var lastSlices by remember { mutableStateOf(slices) }
    if (!isEmpty) lastSlices = slices
    val shownSlices = if (isEmpty) lastSlices else slices

    val values = animatedValues(shownSlices)
    val colors = rememberColors(shownSlices)
    // Slices that left are kept until they have shrunk away
    val ids = (shownSlices.map { it.id } + values.keys).distinct()

    val selection = ids.associateWith { id ->
        key(id) {
            val selected by animateFloatAsState(if (id == selectedId) 1f else 0f, label = "selection")
            val alpha by animateFloatAsState(
                if (selectedId == null || id == selectedId) 1f else DIMMED_ALPHA,
                label = "alpha",
            )
            selected to alpha
        }
    }

    val haptics = LocalHapticFeedback.current
    val currentOnSliceClick by rememberUpdatedState(onSliceClick)
    val currentIds by rememberUpdatedState(ids)
    val currentValues by rememberUpdatedState(values)
    val currentIsEmpty by rememberUpdatedState(isEmpty)

    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { tap ->
                        val geometry = RingGeometry(size.width.toFloat(), size.height.toFloat(), strokeWidth.toPx(), selectedExtraWidth.toPx())
                        val id = if (currentIsEmpty) {
                            null
                        } else {
                            geometry.sliceAt(tap, currentIds.map { it to (currentValues[it] ?: 0f) })
                        }
                        if (id != null) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        currentOnSliceClick(id)
                    }
                },
        ) {
            val geometry = RingGeometry(size.width, size.height, strokeWidth.toPx(), selectedExtraWidth.toPx())
            val total = ids.sumOf { (values[it] ?: 0f).toDouble() }.toFloat()
            // The track only stands in for an empty ring, so the gaps between slices stay clear
            val trackAlpha = (1f - fill) * intro.value
            if (trackAlpha > 0f) {
                drawCircle(
                    color = trackColor.copy(alpha = trackColor.alpha * trackAlpha),
                    radius = geometry.radius,
                    center = geometry.center,
                    style = Stroke(geometry.stroke),
                )
            }
            if (total <= 0f || fill <= 0f) return@Canvas
            val visibleSlices = ids.count { (values[it] ?: 0f) > 0f }
            val gap = if (visibleSlices > 1) GAP_DEGREES else 0f
            val totalSweep = 360f * intro.value * fill

            rotate(INTRO_ROTATION_DEGREES * (1f - intro.value), geometry.center) {
                // Unfolds clockwise from the top
                var angle = -90f
                ids.forEach { id ->
                    val value = values[id] ?: 0f
                    if (value <= 0f) return@forEach
                    val sweep = value / total * totalSweep
                    val (fullSelected, fullAlpha) = selection[id] ?: (0f to 1f)
                    val selected = fullSelected * selectionStrength
                    val alpha = lerp(1f, fullAlpha, selectionStrength)
                    val width = geometry.stroke + geometry.extra * selected
                    // Grow outwards only, the inner edge stays
                    val radius = geometry.radius + geometry.extra * selected / 2f
                    drawArc(
                        color = (colors[id] ?: Color.Gray).copy(alpha = alpha),
                        startAngle = angle + gap / 2f,
                        sweepAngle = (sweep - gap).coerceAtLeast(0f),
                        useCenter = false,
                        topLeft = Offset(geometry.center.x - radius, geometry.center.y - radius),
                        size = Size(radius * 2f, radius * 2f),
                        style = Stroke(width),
                    )
                    angle += sweep
                }
            }
        }
        Box(Modifier.graphicsLayer { alpha = ((intro.value - 0.4f) / 0.6f).coerceIn(0f, 1f) }) {
            centerContent()
        }
    }
}

/**
 * The values of the slices, animated from the previous values to the current ones whenever they
 * change. Removed slices animate to 0 and stay in the map until then.
 */
@Composable
private fun animatedValues(slices: List<DonutSlice>): Map<Long, Float> {
    val targets = slices.associate { it.id to it.value }
    var from by remember { mutableStateOf(targets) }
    var to by remember { mutableStateOf(targets) }
    val progress = remember { Animatable(1f) }

    fun current(): Map<Long, Float> {
        val t = progress.value
        return (to.keys + from.keys).associateWith { lerp(from[it] ?: 0f, to[it] ?: 0f, t) }
    }

    LaunchedEffect(targets) {
        if (targets == to) return@LaunchedEffect
        from = current()
        to = targets
        progress.snapTo(0f)
        progress.animateTo(1f, tween(CHANGE_MILLIS, easing = FastOutSlowInEasing))
        // Forget slices that have shrunk away
        from = to
    }
    return current()
}

/** Colors by id, remembering the ones of removed slices for their exit animation. */
@Composable
private fun rememberColors(slices: List<DonutSlice>): Map<Long, Color> {
    val colors = remember { mutableMapOf<Long, Color>() }
    slices.forEach { colors[it.id] = it.color }
    return colors
}

private class RingGeometry(width: Float, height: Float, val stroke: Float, val extra: Float) {
    val center = Offset(width / 2f, height / 2f)

    /** Radius of the middle of the ring; leaves room for a selected slice to grow. */
    val radius = (minOf(width, height) - stroke - 2f * extra) / 2f

    /** The slice at [point], `null` if it is not on the ring. Assumes the intro has finished. */
    fun sliceAt(point: Offset, slices: List<Pair<Long, Float>>): Long? {
        val dx = point.x - center.x
        val dy = point.y - center.y
        val distance = hypot(dx, dy)
        if (distance < radius - stroke / 2f || distance > radius + stroke / 2f + extra) return null

        val total = slices.sumOf { it.second.toDouble() }.toFloat()
        if (total <= 0f) return null
        // Clockwise from the top, like the slices are drawn
        val angle = (Math.toDegrees(atan2(dx, -dy).toDouble()).toFloat() + 360f) % 360f
        var end = 0f
        slices.forEach { (id, value) ->
            if (value <= 0f) return@forEach
            end += value / total * 360f
            if (angle < end) return id
        }
        return slices.lastOrNull { it.second > 0f }?.first
    }
}

@Preview(showBackground = true)
@Composable
private fun DonutChartPreview() {
    ClockMeTheme(dynamicColor = false) {
        DonutChart(
            slices = listOf(
                DonutSlice(1, 120f, Color(0xFF4285F4)),
                DonutSlice(2, 45f, Color(0xFF0B8043)),
                DonutSlice(3, 30f, Color(0xFFD50000)),
            ),
            selectedId = 2,
            onSliceClick = {},
            modifier = Modifier.size(240.dp),
        ) {
            Text("3 h 15 min", style = MaterialTheme.typography.headlineSmall)
        }
    }
}
