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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import es.jvbabi.clockme.ui.theme.CirclesTheme
import kotlin.math.atan2
import kotlin.math.hypot

data class DonutSlice(
    val id: Long,
    val value: Float,
    val color: Color,
    /** Slices of a group must be next to each other, the group's band is drawn around them. */
    val groupId: Long? = null,
)

/** A band drawn just outside the ring around the slices of the group; it is clickable too. */
data class DonutGroup(
    val id: Long,
    val color: Color,
)

/** Angle the ring starts at before it unfolds, it turns from there to 0°. */
private const val INTRO_ROTATION_DEGREES = -30f
private const val INTRO_MILLIS = 1_000

private object IntroNotPlayed
private const val CHANGE_MILLIS = 600

/** Gap between two slices. */
private const val GAP_DEGREES = 2f

/** Alpha of the slices that are not selected while one is. */
private const val DIMMED_ALPHA = 0.3f

/** A selected group's band gets this much wider. */
private const val SELECTED_BAND_SCALE = 1.6f

/**
 * A ring chart. Slices are clickable: [onSliceClick] gets the slice's id, or `null` for a tap
 * beside the ring. The [selectedId] slice grows outwards, the others fade.
 *
 * [groups] get a band around their slices that hugs the ring and moves out with grown slices.
 * Tapping a band, or the space around it, reports the group to [onGroupClick]. The slices of the
 * [selectedGroupId] grow like a selected slice and its band widens.
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
    /** `false` shows the ring right away without unfolding, e.g. for later pages of a pager. */
    animateIntro: Boolean = true,
    /** The ring unfolds again whenever this changes, e.g. on a mode change. */
    introKey: Any? = null,
    /** `false` shows new values right away, e.g. while the caller animates them itself. */
    animateChanges: Boolean = true,
    groups: List<DonutGroup> = emptyList(),
    selectedGroupId: Long? = null,
    onGroupClick: (Long) -> Unit = {},
    groupBandWidth: Dp = 5.dp,
    groupBandGap: Dp = 3.dp,
    /** Room outside the bands that still counts as a tap on them. */
    groupTouchPadding: Dp = 8.dp,
    centerContent: @Composable () -> Unit = {},
) {
    val intro = remember { Animatable(if (animateIntro) 0f else 1f) }
    // The key the intro was last played for; a ring shown without intro counts as played
    var playedKey by remember { mutableStateOf<Any?>(if (animateIntro) IntroNotPlayed else introKey) }
    LaunchedEffect(playIntro, introKey) {
        if (!playIntro || playedKey == introKey) return@LaunchedEffect
        playedKey = introKey
        intro.snapTo(0f)
        intro.animateTo(1f, tween(INTRO_MILLIS, easing = EaseOutCubic))
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

    val values = animatedValues(shownSlices, animateChanges)
    val colors = rememberColors(shownSlices)
    val groupOfSlice = rememberGroups(shownSlices)
    val groupColors = groups.associate { it.id to it.color }
    // Room for the bands only if there are groups, so the ring keeps its size otherwise
    val hasGroups = groups.isNotEmpty()
    // Slices that left are kept where they were until they have shrunk away
    val ids = rememberStableOrder(shownSlices.map { it.id }, values.keys)

    val selection = ids.associateWith { id ->
        key(id) {
            val highlighted = id == selectedId || (selectedGroupId != null && groupOfSlice[id] == selectedGroupId)
            val selected by animateFloatAsState(if (highlighted) 1f else 0f, label = "selection")
            val alpha by animateFloatAsState(
                if ((selectedId == null && selectedGroupId == null) || highlighted) 1f else DIMMED_ALPHA,
                label = "alpha",
            )
            selected to alpha
        }
    }
    val groupIds = (groups.map { it.id } + groupOfSlice.values).distinct()
    val groupSelection = groupIds.associateWith { id ->
        key(id) {
            animateFloatAsState(if (id == selectedGroupId) 1f else 0f, label = "group selection").value
        }
    }

    val haptics = LocalHapticFeedback.current
    val currentOnSliceClick by rememberUpdatedState(onSliceClick)
    val currentOnGroupClick by rememberUpdatedState(onGroupClick)
    val currentIds by rememberUpdatedState(ids)
    val currentValues by rememberUpdatedState(values)
    val currentIsEmpty by rememberUpdatedState(isEmpty)
    val currentHasGroups by rememberUpdatedState(hasGroups)
    val currentGroupOfSlice by rememberUpdatedState(groupOfSlice)
    val currentSelection by rememberUpdatedState(selection)

    fun Density.geometry(width: Float, height: Float, withGroups: Boolean) = RingGeometry(
        width,
        height,
        strokeWidth.toPx(),
        selectedExtraWidth.toPx(),
        band = if (withGroups) groupBandWidth.toPx() else 0f,
        bandGap = if (withGroups) groupBandGap.toPx() else 0f,
        touchPadding = if (withGroups) groupTouchPadding.toPx() else 0f,
    )

    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { tap ->
                        val geometry = geometry(size.width.toFloat(), size.height.toFloat(), currentHasGroups)
                        val hit = if (currentIsEmpty) {
                            null
                        } else {
                            geometry.hitAt(
                                point = tap,
                                slices = currentIds.map { it to (currentValues[it] ?: 0f) },
                                groupOf = currentGroupOfSlice,
                                selectedOf = { currentSelection[it]?.first ?: 0f },
                            )
                        }
                        if (hit != null) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        when (hit) {
                            is RingHit.Group -> currentOnGroupClick(hit.id)
                            is RingHit.Slice -> currentOnSliceClick(hit.id)
                            null -> currentOnSliceClick(null)
                        }
                    }
                },
        ) {
            val geometry = geometry(size.width, size.height, hasGroups)
            // While unfolding the ring also grows from its outer edge inwards
            val stroke = geometry.stroke * intro.value
            val ringRadius = geometry.outerEdge - stroke / 2f
            val total = ids.sumOf { (values[it] ?: 0f).toDouble() }.toFloat()
            // The track only stands in for an empty ring, so the gaps between slices stay clear
            val trackAlpha = (1f - fill) * intro.value
            if (trackAlpha > 0f) {
                drawCircle(
                    color = trackColor.copy(alpha = trackColor.alpha * trackAlpha),
                    radius = ringRadius,
                    center = geometry.center,
                    style = Stroke(stroke),
                )
            }
            if (total <= 0f || fill <= 0f) return@Canvas
            val totalSweep = 360f * intro.value * fill
            // The gap around a slice grows and shrinks with it and with the rest of the ring,
            // so it never opens or closes at once while slices come or go
            fun gapOf(sweep: Float) = minOf(GAP_DEGREES, sweep / 2f, (totalSweep - sweep) / 2f).coerceAtLeast(0f)

            rotate(INTRO_ROTATION_DEGREES * (1f - intro.value), geometry.center) {
                // Unfolds clockwise from the top
                var angle = -90f
                // Start and end angle of each group, for the bands
                val groupArcs = linkedMapOf<Long, Pair<Float, Float>>()
                // How far the grown slices of a group push its band out
                val groupPush = mutableMapOf<Long, Float>()
                val highlightedGroup = selectedGroupId ?: selectedId?.let { groupOfSlice[it] }
                val hasSelection = selectedId != null || selectedGroupId != null
                ids.forEach { id ->
                    val value = values[id] ?: 0f
                    if (value <= 0f) return@forEach
                    val sweep = value / total * totalSweep
                    val (fullSelected, fullAlpha) = selection[id] ?: (0f to 1f)
                    val selected = fullSelected * selectionStrength
                    val alpha = lerp(1f, fullAlpha, selectionStrength)
                    val width = stroke + geometry.extra * selected
                    // Grow outwards only, the inner edge stays
                    val radius = ringRadius + geometry.extra * selected / 2f
                    val gap = gapOf(sweep)
                    drawArc(
                        color = (colors[id] ?: Color.Gray).copy(alpha = alpha),
                        startAngle = angle + gap / 2f,
                        sweepAngle = (sweep - gap).coerceAtLeast(0f),
                        useCenter = false,
                        topLeft = Offset(geometry.center.x - radius, geometry.center.y - radius),
                        size = Size(radius * 2f, radius * 2f),
                        style = Stroke(width),
                    )
                    groupOfSlice[id]?.let { groupId ->
                        val (start, _) = groupArcs[groupId] ?: (angle to angle)
                        groupArcs[groupId] = start to angle + sweep
                        groupPush[groupId] = maxOf(groupPush[groupId] ?: 0f, selected)
                    }
                    angle += sweep
                }

                groupArcs.forEach { (groupId, arc) ->
                    val (start, end) = arc
                    val scale = lerp(1f, SELECTED_BAND_SCALE, (groupSelection[groupId] ?: 0f) * selectionStrength)
                    val band = geometry.band * scale * intro.value
                    // Hugs the ring from its outer edge, even while the ring grows in the intro
                    val bandRadius = geometry.bandInnerEdge(groupPush[groupId] ?: 0f) + band / 2f
                    // Fades with the slices when something outside the group is selected
                    val dimmed = hasSelection && highlightedGroup != groupId
                    val alpha = if (dimmed) lerp(1f, DIMMED_ALPHA, selectionStrength) else 1f
                    val gap = gapOf(end - start)
                    drawArc(
                        color = (groupColors[groupId] ?: Color.Gray).copy(alpha = alpha),
                        startAngle = start + gap / 2f,
                        sweepAngle = (end - start - gap).coerceAtLeast(0f),
                        useCenter = false,
                        topLeft = Offset(geometry.center.x - bandRadius, geometry.center.y - bandRadius),
                        size = Size(bandRadius * 2f, bandRadius * 2f),
                        style = Stroke(band, cap = StrokeCap.Round),
                    )
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
 * change. Removed slices animate to 0 and stay in the map until then. Without [animate] they
 * follow right away, and a later animation starts from there.
 */
@Composable
private fun animatedValues(slices: List<DonutSlice>, animate: Boolean): Map<Long, Float> {
    val targets = slices.associate { it.id to it.value }
    var from by remember { mutableStateOf(targets) }
    var to by remember { mutableStateOf(targets) }
    val progress = remember { Animatable(1f) }
    if (!animate) {
        from = targets
        to = targets
        return targets
    }

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

/**
 * The order to draw the slices in: that of [current], with the [leaving] ones still at the place
 * they had, right after the slice they followed before, so they shrink away where they were.
 */
@Composable
private fun rememberStableOrder(current: List<Long>, leaving: Set<Long>): List<Long> {
    val previous = remember { mutableListOf<Long>() }
    val order = stableOrder(previous, current, leaving)
    previous.clear()
    previous.addAll(order)
    return order
}

internal fun stableOrder(previous: List<Long>, current: List<Long>, leaving: Collection<Long>): List<Long> {
    val gone = leaving.filter { it !in current }.toSet()
    if (gone.isEmpty()) return current
    val order = current.toMutableList()
    previous.forEachIndexed { index, id ->
        if (id !in gone) return@forEachIndexed
        // After the nearest slice before it that is still drawn, or first if there is none
        val before = previous.subList(0, index).lastOrNull { it in order }
        order.add(if (before == null) 0 else order.indexOf(before) + 1, id)
    }
    // Leaving slices the previous order did not know yet go last
    return order + gone.filter { it !in order }
}

/** Groups by slice id, remembering the ones of removed slices for their exit animation. */
@Composable
private fun rememberGroups(slices: List<DonutSlice>): Map<Long, Long> {
    val groups = remember { mutableMapOf<Long, Long>() }
    slices.forEach { slice -> if (slice.groupId != null) groups[slice.id] = slice.groupId else groups.remove(slice.id) }
    return groups
}

/** Colors by id, remembering the ones of removed slices for their exit animation. */
@Composable
private fun rememberColors(slices: List<DonutSlice>): Map<Long, Color> {
    val colors = remember { mutableMapOf<Long, Color>() }
    slices.forEach { colors[it.id] = it.color }
    return colors
}

private sealed interface RingHit {
    data class Slice(val id: Long) : RingHit
    data class Group(val id: Long) : RingHit
}

private class RingGeometry(
    width: Float,
    height: Float,
    val stroke: Float,
    val extra: Float,
    val band: Float = 0f,
    private val bandGap: Float = 0f,
    touchPadding: Float = 0f,
) {
    val center = Offset(width / 2f, height / 2f)

    /** Taps in the corners outside the circle hit nothing. */
    private val maxRadius = minOf(width, height) / 2f

    /**
     * Radius of the middle of the ring; leaves room for a selected slice, the group bands pushed
     * out by it and the touch area around them.
     */
    val radius = (minOf(width, height) - stroke - 2f * (extra + bandGap + band * SELECTED_BAND_SCALE + touchPadding)) / 2f

    val outerEdge = radius + stroke / 2f

    /** A group's band lies just outside the ring, pushed out as far as its slices have grown ([push] 0..1). */
    fun bandInnerEdge(push: Float) = outerEdge + bandGap + extra * push

    /**
     * What is at [point], `null` if neither a slice nor a band. Assumes the intro has finished.
     * Outside the ring, a group's band takes everything up to the edge of the chart; between a
     * slice and its band the area is split in the middle.
     */
    fun hitAt(
        point: Offset,
        slices: List<Pair<Long, Float>>,
        groupOf: Map<Long, Long>,
        selectedOf: (Long) -> Float,
    ): RingHit? {
        val dx = point.x - center.x
        val dy = point.y - center.y
        val distance = hypot(dx, dy)
        if (distance < radius - stroke / 2f || distance > maxRadius) return null
        val id = sliceAtAngle(dx, dy, slices) ?: return null
        val sliceOuterEdge = outerEdge + extra * selectedOf(id)

        val groupId = groupOf[id]?.takeIf { band > 0f }
        if (groupId != null) {
            val push = slices.filter { groupOf[it.first] == groupId }.maxOf { selectedOf(it.first) }
            val boundary = (sliceOuterEdge + bandInnerEdge(push)) / 2f
            return if (distance >= boundary) RingHit.Group(groupId) else RingHit.Slice(id)
        }
        return if (distance <= outerEdge + extra) RingHit.Slice(id) else null
    }

    /** The slice in the direction of ([dx], [dy]) from the center, clockwise from the top like they are drawn. */
    private fun sliceAtAngle(dx: Float, dy: Float, slices: List<Pair<Long, Float>>): Long? {
        val total = slices.sumOf { it.second.toDouble() }.toFloat()
        if (total <= 0f) return null
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
    CirclesTheme(dynamicColor = false) {
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
