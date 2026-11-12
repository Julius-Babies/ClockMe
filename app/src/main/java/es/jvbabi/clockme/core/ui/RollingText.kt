package es.jvbabi.clockme.core.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/**
 * Text whose characters roll like an odometer when it changes: each changed character slides
 * up if the text got larger, down if it got smaller. Unchanged characters stay still, added or
 * removed ones grow in or shrink away. Nothing is clipped while animating.
 */
@Composable
fun RollingText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    fontWeight: FontWeight? = null,
) {
    var previous by remember { mutableStateOf(text) }
    // Characters of the longest text so far, so removed ones can still be shown while leaving
    var slots by remember { mutableStateOf(text.toList()) }
    val rollUp = remember(text) {
        val up = text.compareAsNumbers(previous) >= 0
        previous = text
        slots = List(maxOf(slots.size, text.length)) { index -> text.getOrNull(index) ?: slots[index] }
        up
    }

    Row(modifier.clearAndSetSemantics { contentDescription = text }) {
        slots.forEachIndexed { index, lastChar ->
            AnimatedVisibility(
                visible = index < text.length,
                enter = expandHorizontally(SIZE_SPEC, clip = false) + fadeIn(),
                exit = shrinkHorizontally(SIZE_SPEC, clip = false) + fadeOut(),
            ) {
                AnimatedContent(
                    targetState = text.getOrNull(index) ?: lastChar,
                    transitionSpec = {
                        val direction = if (rollUp) 1 else -1
                        (slideInVertically(SLIDE_SPEC) { it * direction } + fadeIn()) togetherWith
                            (slideOutVertically(SLIDE_SPEC) { -it * direction } + fadeOut()) using
                            SizeTransform(clip = false) { _, _ -> SIZE_SPEC }
                    },
                    label = "char$index",
                ) { char ->
                    Text(text = char.toString(), style = style, fontWeight = fontWeight)
                }
            }
        }
    }
}

private val SLIDE_SPEC = spring<IntOffset>(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
private val SIZE_SPEC = spring<IntSize>(stiffness = Spring.StiffnessMediumLow)

/** Compares by the digits in the texts, e.g. "1h05" > "45min"; falls back to the text. */
private fun String.compareAsNumbers(other: String): Int {
    val digits = filter { it.isDigit() }.toBigIntegerOrNull()
    val otherDigits = other.filter { it.isDigit() }.toBigIntegerOrNull()
    if (digits == null || otherDigits == null) return compareTo(other)
    // More digit groups (hours) always means more
    val groups = split(Regex("\\D+")).count { it.isNotEmpty() }
    val otherGroups = other.split(Regex("\\D+")).count { it.isNotEmpty() }
    return if (groups != otherGroups) groups.compareTo(otherGroups) else digits.compareTo(otherDigits)
}
