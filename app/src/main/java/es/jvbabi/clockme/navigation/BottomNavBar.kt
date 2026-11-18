package es.jvbabi.clockme.navigation

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.phosphor.icons.PhIcons
import com.phosphor.icons.filled.ChartBarFill
import com.phosphor.icons.filled.HouseFill
import com.phosphor.icons.regular.ChartBar
import com.phosphor.icons.regular.House
import es.jvbabi.clockme.R
import es.jvbabi.clockme.ui.theme.ClockMeTheme

/**
 * How much of the bottom edge the [BottomNavBar] covers, the system bar below it included. The
 * bar floats over the screens, so a screen keeps its own content clear of it with this.
 */
val LocalBottomNavBarHeight = compositionLocalOf<Dp> { 0.dp }

/** In the order the bar shows them. */
private val TABS = listOf(Screen.Home, Screen.Reports)

private val NAV_ITEM_WIDTH = 92.dp
private val NAV_ITEM_HEIGHT = 64.dp
private val NAV_ITEM_SPACING = 4.dp

/** Lifts the bar off whatever scrolls beneath it. */
private val NAV_BAR_ELEVATION = 6.dp

/** [onSelect] is told about the tab that is already [selected] as well: a second tap may mean something. */
@Composable
fun BottomNavBar(
    selected: Screen.Tab,
    onSelect: (Screen.Tab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .shadow(NAV_BAR_ELEVATION, RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surface)
            .padding(4.dp),
    ) {
        // Behind the items rather than on one of them, so it can slide from one to the next.
        val indicatorOffset by animateDpAsState(
            targetValue = (NAV_ITEM_WIDTH + NAV_ITEM_SPACING) * TABS.indexOf(selected),
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
        )
        Box(
            Modifier
                .offset { IntOffset(indicatorOffset.roundToPx(), 0) }
                .size(NAV_ITEM_WIDTH, NAV_ITEM_HEIGHT)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(NAV_ITEM_SPACING)) {
            NavItem(
                label = R.string.nav_home,
                icon = PhIcons.Regular.House,
                selectedIcon = PhIcons.Filled.HouseFill,
                isSelected = selected == Screen.Home,
                onClick = { onSelect(Screen.Home) },
            )
            NavItem(
                label = R.string.nav_reports,
                icon = PhIcons.Regular.ChartBar,
                selectedIcon = PhIcons.Filled.ChartBarFill,
                isSelected = selected == Screen.Reports,
                onClick = { onSelect(Screen.Reports) },
            )
        }
    }
}

@Composable
private fun NavItem(
    @StringRes label: Int,
    icon: ImageVector,
    selectedIcon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val contentColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
    )
    Column(
        Modifier
            .size(NAV_ITEM_WIDTH, NAV_ITEM_HEIGHT)
            .clip(RoundedCornerShape(50))
            .selectable(selected = isSelected, role = Role.Tab, onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = if (isSelected) selectedIcon else icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = stringResource(label),
            color = contentColor,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Preview
@Composable
private fun BottomNavBarPreview() {
    ClockMeTheme(dynamicColor = false) {
        BottomNavBar(selected = Screen.Home, onSelect = {})
    }
}
