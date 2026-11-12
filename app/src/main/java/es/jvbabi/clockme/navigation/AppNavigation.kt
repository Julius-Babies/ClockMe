package es.jvbabi.clockme.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import es.jvbabi.clockme.feature.calendars.ui.CalendarsScreen
import es.jvbabi.clockme.feature.home.ui.HomeScreen
import es.jvbabi.clockme.feature.onboarding.ui.OnboardingRequestPermissionScreen

/**
 * Between two tabs the pages fade into each other: they sit side by side in the bottom bar, not
 * on top of one another, so sliding one in over the other would say the wrong thing.
 */
private val TAB_TRANSITION: Map<String, Any> =
    NavDisplay.transitionSpec { tabFade() } +
        NavDisplay.popTransitionSpec { tabFade() } +
        NavDisplay.predictivePopTransitionSpec { tabFade() }

private fun tabFade() = fadeIn(tween(TAB_FADE_MILLIS)) togetherWith fadeOut(tween(TAB_FADE_MILLIS))

private const val TAB_FADE_MILLIS = 200

private val BOTTOM_NAV_BAR_MARGIN = 16.dp

@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(Screen.Home)

    val localDensity = LocalDensity.current
    var bottomNavBarHeight by remember { mutableStateOf(0.dp) }
    val bottomNavBarPadding =
        BOTTOM_NAV_BAR_MARGIN + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()

    Box(modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalBottomNavBarHeight provides bottomNavBarHeight + bottomNavBarPadding) {
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    // Scopes ViewModels to their back stack entry
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                entryProvider = entryProvider {
                    entry<Screen.Home>(metadata = TAB_TRANSITION) {
                        HomeScreen(
                            onNavigateToOnboarding = { backStack.replaceAll(Screen.OnboardingRequestPermission) },
                        )
                    }
                    entry<Screen.Calendars>(metadata = TAB_TRANSITION) {
                        CalendarsScreen()
                    }
                    entry<Screen.OnboardingRequestPermission> {
                        OnboardingRequestPermissionScreen(
                            onNavigateToHome = { backStack.replaceAll(Screen.Home) },
                        )
                    }
                },
            )
        }

        // Kept while the bar leaves, so it goes as it was rather than with nothing selected.
        val topTab = backStack.lastOrNull() as? Screen.Tab
        var shownTab by remember { mutableStateOf<Screen.Tab>(Screen.Home) }
        if (topTab != null) shownTab = topTab
        AnimatedVisibility(
            visible = topTab != null,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            val currentTab = shownTab
            BottomNavBar(
                selected = currentTab,
                onSelect = { tab -> if (tab != currentTab) backStack.selectTab(tab) },
                modifier = Modifier
                    .padding(bottom = bottomNavBarPadding)
                    .onSizeChanged { (_, h) -> bottomNavBarHeight = with(localDensity) { h.toDp() } },
            )
        }
    }
}

/** Home is the root and every other tab lies on top of it, so back from any of them leads there. */
private fun NavBackStack<NavKey>.selectTab(tab: Screen.Tab) {
    removeAll { it is Screen.Tab && it != Screen.Home }
    if (tab != Screen.Home) add(tab)
}
