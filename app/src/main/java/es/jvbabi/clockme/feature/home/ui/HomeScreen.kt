package es.jvbabi.clockme.feature.home.ui

import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.Stable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.Spring
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.Constraints
import kotlinx.coroutines.flow.drop
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.runtime.SideEffect
import kotlin.math.abs
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.layout.layout
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.gestures.Orientation
import android.text.format.DateUtils
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.phosphor.icons.PhIcons
import com.phosphor.icons.regular.CaretLeft
import com.phosphor.icons.regular.CaretRight
import kotlin.math.roundToInt
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.LocalDate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.draw.alpha
import androidx.compose.runtime.key
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.selection.selectable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.snap
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.jvbabi.clockme.R
import es.jvbabi.clockme.core.ui.LocalAppStartState
import es.jvbabi.clockme.core.ui.ObserveAsEvents
import es.jvbabi.clockme.core.ui.RollingText
import es.jvbabi.clockme.core.ui.chart.DonutChart
import es.jvbabi.clockme.core.ui.chart.DonutGroup
import es.jvbabi.clockme.core.ui.chart.DonutSlice
import es.jvbabi.clockme.core.ui.formatDuration
import es.jvbabi.clockme.core.ui.rememberTimeFormatter
import es.jvbabi.clockme.navigation.LocalBottomNavBarHeight
import es.jvbabi.clockme.ui.theme.ClockMeTheme
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

@Composable
fun HomeScreen(
    onNavigateToOnboarding: () -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            HomeEvent.NavigateToOnboarding -> onNavigateToOnboarding()
        }
    }
    val appStart = LocalAppStartState.current
    LaunchedEffect(uiState.isReady) {
        if (uiState.isReady) appStart.onContentReady()
    }
    HomeContent(
        uiState = uiState,
        onCalendarSelect = viewModel::onCalendarSelect,
        onGroupSelect = viewModel::onGroupSelect,
        onClearSelection = viewModel::onClearSelection,
        onShortcutSelect = viewModel::onShortcutSelect,
        onPageChange = viewModel::onPageChange,
    )
}

object HomeTestTags {
    const val RING = "home_ring"
    const val SHORTCUTS = "home_shortcuts"
    const val PAGER = "home_pager"
    const val PAGE_LABEL = "home_page_label"
}

@Composable
internal fun HomeContent(
    uiState: HomeUiState,
    onCalendarSelect: (Long?) -> Unit,
    onGroupSelect: (Long) -> Unit,
    onClearSelection: () -> Unit,
    onShortcutSelect: (RangeShortcut) -> Unit,
    onPageChange: (Int) -> Unit,
) {
    // 0 while no back gesture is running, up to 1 while the user swipes back
    var backProgress by remember { mutableFloatStateOf(0f) }
    // Back only clears the selection; without one it is left to the navigation
    PredictiveBackHandler(enabled = uiState.hasSelection) { events ->
        try {
            events.collect { backProgress = it.progress }
            onClearSelection()
        } finally {
            backProgress = 0f
        }
    }

    Scaffold { innerPadding ->
        Box(Modifier.fillMaxSize()) {
            when (uiState.hasActiveCalendars) {
                null -> Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }

                false -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = innerPadding.calculateTopPadding(), bottom = LocalBottomNavBarHeight.current)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stringResource(R.string.home_no_active_calendars), textAlign = TextAlign.Center)
                }

                true -> HomePager(
                    uiState = uiState,
                    topInset = innerPadding.calculateTopPadding(),
                    selectionStrength = 1f - backProgress,
                    onCalendarSelect = onCalendarSelect,
                    onGroupSelect = onGroupSelect,
                    onClearSelection = onClearSelection,
                    onShortcutSelect = onShortcutSelect,
                    onPageChange = onPageChange,
                )
            }
            LastSyncHint(
                lastSync = uiState.lastEventSync,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = LocalBottomNavBarHeight.current + 8.dp),
            )
        }
    }
}

/** Pages far enough into the past; the last one is the current unit. */
private const val PAGE_COUNT = 100_000
private const val CURRENT_PAGE = PAGE_COUNT - 1

/**
 * The whole screen pages horizontally through the units of the selected shortcut. On top stay the
 * shortcuts and a single card with the ring and the page navigator, which always shows the current
 * page: its ring morphs from page to page. The card shrinks as the page is scrolled and then floats.
 */
@Composable
private fun HomePager(
    uiState: HomeUiState,
    topInset: Dp,
    selectionStrength: Float,
    onCalendarSelect: (Long?) -> Unit,
    onGroupSelect: (Long) -> Unit,
    onClearSelection: () -> Unit,
    onShortcutSelect: (RangeShortcut) -> Unit,
    onPageChange: (Int) -> Unit,
) {
    val pagerState = rememberPagerState(initialPage = CURRENT_PAGE + uiState.pageOffset) { PAGE_COUNT }

    // Follow the state: jump when the mode changes (the ring unfolds there), slide otherwise
    var shownShortcut by remember { mutableStateOf(uiState.selectedShortcut) }
    // The page jumped to on a mode change, which gets no haptic tick
    val jumpTarget = remember { intArrayOf(-1) }
    LaunchedEffect(uiState.selectedShortcut, uiState.pageOffset) {
        val target = CURRENT_PAGE + uiState.pageOffset
        if (uiState.selectedShortcut != shownShortcut) {
            shownShortcut = uiState.selectedShortcut
            jumpTarget[0] = if (target != pagerState.currentPage) target else -1
            pagerState.scrollToPage(target)
        } else if (pagerState.currentPage != target) {
            pagerState.animateScrollToPage(target)
        }
    }
    val currentOnPageChange by rememberUpdatedState(onPageChange)
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { currentOnPageChange(it - CURRENT_PAGE) }
    }
    // A tick whenever another page takes over the card, half way through a swipe; not on the
    // jump to a new mode
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.drop(1).collect { page ->
            if (page == jumpTarget[0]) {
                jumpTarget[0] = -1
            } else {
                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
            }
        }
    }

    val density = LocalDensity.current
    val collapseRangePx = with(density) { (RING_SIZE - RING_COLLAPSED_SIZE).toPx() }
    // Scroll state of each composed page, so the card knows how far the current one is scrolled
    val listStates = remember { mutableStateMapOf<Int, LazyListState>() }
    fun collapseOf(page: Int): Float {
        val state = listStates[page] ?: return 0f
        if (state.firstVisibleItemIndex > 0) return 1f
        return (state.firstVisibleItemScrollOffset / collapseRangePx).coerceIn(0f, 1f)
    }
    // Blends between the current page and the one being swiped to
    val collapse by remember {
        derivedStateOf {
            val fraction = pagerState.currentPageOffsetFraction
            val other = if (fraction < 0f) pagerState.currentPage - 1 else pagerState.currentPage + 1
            lerp(collapseOf(pagerState.currentPage), collapseOf(other), abs(fraction))
        }
    }
    val scrolled by remember { derivedStateOf { collapse > 0f } }

    // The card shows the current page; a page not loaded yet keeps the last one
    val currentPage = uiState.pages[pagerState.currentPage - CURRENT_PAGE]
    var lastPage by remember { mutableStateOf<HomePage?>(null) }
    SideEffect { if (currentPage != null) lastPage = currentPage }
    val shownPage = currentPage ?: lastPage

    // Pulling down at the very top grows the ring to nearly the screen's width and moves the
    // events away; past the threshold it snaps into that view on a spring
    val windowWidth = with(density) { LocalWindowInfo.current.containerSize.width.toDp() }
    val hugeRingSize = maxOf(RING_SIZE, windowWidth - 24.dp)
    val pullRangePx = with(density) { PULL_RANGE.toPx() }
    val pull = remember { PullState() }
    val scope = rememberCoroutineScope()
    val pullConnection = remember(pullRangePx) {
        object : NestedScrollConnection {
            fun atTop() = listStates[pagerState.currentPage]?.canScrollBackward == false

            /**
             * Only the first stretch follows the finger, and heavily; at its end the view springs
             * the rest of the way on its own, and the rest of the gesture is ignored.
             */
            fun drag(delta: Float) {
                pull.animation?.cancel()
                val rest = if (pull.expanded) 1f else 0f
                val before = pull.expansion
                // How far into the stretch, 0 at rest and 1 at the threshold
                val progress = abs(before - rest) / PULL_FOLLOW
                val weight = PULL_WEIGHT * (1f - 0.5f * progress.coerceAtMost(1f))
                // Pulling further into the full view only gives a little
                val beyond = pull.expanded && delta > 0f
                val step = delta / pullRangePx * if (beyond) PULL_WEIGHT * 0.15f else weight
                pull.expansion = (before + step).coerceIn(0f, 1f + PULL_FOLLOW / 2f)
                if (abs(pull.expansion - rest) >= PULL_FOLLOW && !beyond) {
                    haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                    pull.latched = true
                    // Leaves with some speed of its own, towards the other side
                    pull.settle(scope, 1f - rest, if (rest == 0f) SNAP_VELOCITY else -SNAP_VELOCITY)
                }
            }

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput) return Offset.Zero
                // Once it snapped, the gesture is spent
                if (pull.latched) return available.copy(x = 0f)
                // Pushing up first takes the view back before anything scrolls
                if (available.y >= 0f || pull.expansion <= 0f) return Offset.Zero
                drag(available.y)
                return Offset(0f, available.y)
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput || available.y <= 0f || !atTop()) return Offset.Zero
                drag(available.y)
                return Offset(0f, available.y)
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (pull.latched) {
                    pull.latched = false
                    return available
                }
                val rest = if (pull.expanded) 1f else 0f
                if (pull.expansion == rest) return Velocity.Zero
                // Short of the threshold it springs back, unless flung
                val velocity = available.y / pullRangePx
                val target = when {
                    !pull.expanded && velocity > PULL_FLING -> 1f
                    pull.expanded && velocity < -PULL_FLING -> 0f
                    else -> rest
                }
                pull.settle(scope, target, velocity)
                return available
            }
        }
    }
    BackHandler(enabled = pull.expanded) { pull.settle(scope, 0f, 0f) }
    val expansion = pull.expansion.coerceAtLeast(0f)
    val shownRingSize = if (expansion > 0f) lerp(RING_SIZE, hugeRingSize, expansion) else ringSize(collapse)

    var headerHeight by remember { mutableStateOf(0.dp) }
    // Height of the card fully expanded; only its ring changes size, the rest stays
    var cardExpandedHeight by remember { mutableStateOf(0.dp) }

    // In the full view the card reaches down to just above the navigation bar: the ring centered
    // in it, the navigator at its bottom. The space goes above and below the ring in equal parts
    var screenHeight by remember { mutableStateOf(0.dp) }
    val fullCardHeight = screenHeight - topInset - headerHeight - LocalBottomNavBarHeight.current - 8.dp
    val fullViewGap = ((fullCardHeight - (cardExpandedHeight - RING_SIZE) - hugeRingSize) / 2).coerceAtLeast(0.dp)
    val ringGap = fullViewGap * expansion
    val fullViewGrowPx = with(density) { (hugeRingSize - RING_SIZE + fullViewGap * 2).toPx() }

    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { screenHeight = with(density) { it.height.toDp() } }
            .nestedScroll(pullConnection),
    ) {
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = uiState.unit != null,
            modifier = Modifier.fillMaxSize().testTag(HomeTestTags.PAGER),
        ) { index ->
            val listState = rememberLazyListState()
            DisposableEffect(index, listState) {
                listStates[index] = listState
                onDispose { listStates.remove(index) }
            }
            HomePageContent(
                uiState = uiState,
                page = uiState.pages[index - CURRENT_PAGE],
                listState = listState,
                topPadding = topInset + headerHeight + cardExpandedHeight,
                // The events move down with the growing ring and fade away
                modifier = Modifier.graphicsLayer {
                    val e = pull.expansion.coerceAtLeast(0f)
                    translationY = fullViewGrowPx * e
                    alpha = (1f - 1.5f * e).coerceIn(0f, 1f)
                },
            )
        }
        Column(Modifier.padding(top = topInset)) {
            FloatingHeader(
                floating = scrolled,
                modifier = Modifier.onSizeChanged { headerHeight = with(density) { it.height.toDp() } },
            ) { progress ->
                ShortcutSelector(
                    shortcuts = uiState.shortcuts,
                    selected = uiState.selectedShortcut,
                    onSelect = onShortcutSelect,
                    compactness = progress,
                )
            }
            shownPage?.let { page ->
                RingCard(
                    uiState = uiState,
                    page = page,
                    ringSize = shownRingSize,
                    ringGap = ringGap,
                    floating = scrolled,
                    selectionStrength = selectionStrength,
                    onCalendarSelect = onCalendarSelect,
                    onGroupSelect = onGroupSelect,
                    onClearSelection = onClearSelection,
                    onPrevious = { onPageChange(uiState.pageOffset - 1) },
                    onNext = { onPageChange(uiState.pageOffset + 1) },
                    modifier = Modifier
                        .onSizeChanged {
                            val height = with(density) { it.height.toDp() }
                            val expanded = height + (RING_SIZE - shownRingSize) - ringGap * 2
                            // Rounding the shrunk ring wobbles by a pixel; that must not move the pages
                            if ((collapse == 0f && expansion == 0f) || abs((expanded - cardExpandedHeight).value) > 2f) {
                                cardExpandedHeight = expanded
                            }
                        }
                        // Swipes on the card page and scroll like on the page below it
                        .scrollable(
                            state = pagerState,
                            orientation = Orientation.Horizontal,
                            enabled = uiState.unit != null,
                            reverseDirection = true,
                            flingBehavior = PagerDefaults.flingBehavior(pagerState),
                        )
                        .then(
                            listStates[pagerState.currentPage]?.let { list ->
                                Modifier.scrollable(list, Orientation.Vertical, reverseDirection = true)
                            } ?: Modifier,
                        ),
                )
            }
        }
    }
}

private val RING_SIZE = 260.dp
private val RING_COLLAPSED_SIZE = 150.dp

private fun ringSize(collapse: Float) = lerp(RING_SIZE, RING_COLLAPSED_SIZE, collapse)

/** Finger travel that would make the whole way into the full view of the ring without weight. */
private val PULL_RANGE = 240.dp

/** Share of the way into (or out of) the full view that follows the finger; then it snaps. */
private const val PULL_FOLLOW = 0.2f

/** Makes the pull heavy: the view moves this much per finger travel, less towards the threshold. */
private const val PULL_WEIGHT = 0.45f

/** Speed (full views per second) the view snaps away with at the threshold. */
private const val SNAP_VELOCITY = 2f

/** Pull ranges per second that snap on their own, whatever the distance. */
private const val PULL_FLING = 1.5f

/** The pull into the full view of the ring: 0 normal, 1 full view, a bit more while overpulled. */
@Stable
private class PullState {
    var expansion by mutableFloatStateOf(0f)

    /** Whether it rests (or springs towards) the full view. */
    var expanded by mutableStateOf(false)
        private set

    var animation: Job? = null

    /** It snapped during the current gesture, which then does nothing more. */
    var latched = false

    fun settle(scope: CoroutineScope, target: Float, velocity: Float) {
        expanded = target == 1f
        animation?.cancel()
        animation = scope.launch {
            animate(
                initialValue = expansion,
                targetValue = target,
                initialVelocity = velocity,
                animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow),
            ) { value, _ -> expansion = value }
        }
    }
}

/**
 * The ring of [page] with the page navigator below it. Lies flat on the page at first; with
 * scrolling the ring shrinks to [ringSize] (or grows when pulled, with [ringGap] above and below
 * it), and once [floating] the card lifts off like the header.
 */
@Composable
private fun RingCard(
    uiState: HomeUiState,
    page: HomePage,
    ringSize: Dp,
    ringGap: Dp,
    floating: Boolean,
    selectionStrength: Float,
    onCalendarSelect: (Long?) -> Unit,
    onGroupSelect: (Long) -> Unit,
    onClearSelection: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress by animateFloatAsState(if (floating) 1f else 0f, label = "card float")
    val colors = MaterialTheme.colorScheme
    // Constant outer padding, so only the ring changes the card's height
    Box(modifier.fillMaxWidth().padding(top = 4.dp)) {
        Surface(
            shape = RoundedCornerShape(32.dp * progress),
            color = colors.background,
            shadowElevation = 8.dp * progress,
            border = BorderStroke(Dp.Hairline, colors.outlineVariant.copy(alpha = 0.6f * progress)),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp * progress),
        ) {
            Column(Modifier.padding(top = 8.dp, bottom = 4.dp)) {
                Spacer(Modifier.height(ringGap))
                CalendarRing(
                    page = page,
                    groups = uiState.groups,
                    size = ringSize,
                    selectedCalendar = page.calendar(uiState.selectedCalendarId),
                    selectedGroup = uiState.selectedGroup,
                    selectionStrength = selectionStrength,
                    onCalendarSelect = onCalendarSelect,
                    onGroupSelect = onGroupSelect,
                    onClearSelection = onClearSelection,
                    introKey = uiState.selectedShortcut,
                )
                Spacer(Modifier.height(ringGap))
                val unit = uiState.unit
                val today = uiState.today
                if (unit != null && today != null && page.firstDay != null) {
                    PageNavigator(
                        label = rememberPageLabel(unit, page.firstDay, today),
                        canGoNext = page.offset < 0,
                        onPrevious = onPrevious,
                        onNext = onNext,
                    )
                }
            }
        }
    }
}

/**
 * Lies flat on the page until [floating]; then it lifts off as a pill in the background color, held
 * by a soft shadow and a hairline. [content] gets how far it has lifted, to tighten up inside.
 * Its own size never changes, so the content below stays put.
 */
@Composable
private fun FloatingHeader(
    floating: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable (progress: Float) -> Unit,
) {
    val progress by animateFloatAsState(if (floating) 1f else 0f, label = "header float")
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(percent = (50 * progress).roundToInt())
    Box(modifier.fillMaxWidth().height(HEADER_HEIGHT)) {
        Surface(
            shape = shape,
            color = colors.background,
            shadowElevation = 8.dp * progress,
            border = BorderStroke(Dp.Hairline, colors.outlineVariant.copy(alpha = 0.6f * progress)),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp * progress, vertical = 6.dp * progress),
        ) {
            Box(contentAlignment = Alignment.CenterStart) { content(progress) }
        }
    }
}

private val HEADER_HEIGHT = 56.dp

@Composable
private fun HomePageContent(
    uiState: HomeUiState,
    page: HomePage?,
    listState: LazyListState,
    topPadding: Dp,
    modifier: Modifier = Modifier,
) {
    // A page further away than the loaded neighbours stays empty until it is loaded
    if (page == null) {
        Box(Modifier.fillMaxSize())
        return
    }
    val selectedCalendar = page.calendar(uiState.selectedCalendarId)
    val selectedGroup = uiState.selectedGroup
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = LocalBottomNavBarHeight.current + 16.dp),
    ) {
        // Room for the header and the card as an item, not as padding: its scroll offset is what
        // collapses the card, and it is taller than the whole collapse
        item(key = "top space") { Spacer(Modifier.height(topPadding)) }
        item(key = "events title") {
            val selectedName = selectedCalendar?.name ?: selectedGroup?.name
            Text(
                text = selectedName
                    ?.let { stringResource(R.string.home_events_of_calendar, it) }
                    ?: stringResource(R.string.home_events),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
            )
        }
        if (page.events.isEmpty()) {
            item(key = "no events") {
                Text(
                    text = stringResource(R.string.home_no_events),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
        val eventsByDay = page.events.groupBy { it.date }
        eventsByDay.forEach { (date, events) ->
            if (eventsByDay.size > 1) {
                item(key = "day-$date") { DayHeader(date, Modifier.animateItem()) }
            }
            items(events, key = { "${it.eventId}-${it.start.toEpochMilliseconds()}" }) { event ->
                EventRow(event, Modifier.animateItem())
            }
        }
    }
}

/** A quiet bar naming the page, with buttons to the previous and next one. */
@Composable
private fun PageNavigator(
    label: PageLabel,
    canGoNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
    ) {
        IconButton(onClick = onPrevious) {
            Icon(
                imageVector = PhIcons.Regular.CaretLeft,
                contentDescription = stringResource(R.string.home_page_previous),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .weight(1f)
                .testTag(HomeTestTags.PAGE_LABEL)
                .semantics(mergeDescendants = true) {},
        ) {
            Text(
                text = label.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
            )
            label.subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
        IconButton(onClick = onNext, enabled = canGoNext) {
            Icon(
                imageVector = PhIcons.Regular.CaretRight,
                contentDescription = stringResource(R.string.home_page_next),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (canGoNext) 1f else 0.38f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * When the events were last synced, shown above the bottom bar for a moment once home is
 * visible and again after every sync.
 */
@Composable
private fun LastSyncHint(lastSync: Instant?, modifier: Modifier = Modifier) {
    val splashDismissed by LocalAppStartState.current.splashDismissed.collectAsStateWithLifecycle()
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(lastSync, splashDismissed) {
        if (lastSync == null || !splashDismissed) return@LaunchedEffect
        visible = true
        delay(LAST_SYNC_HINT_MILLIS)
        visible = false
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = modifier,
    ) {
        lastSync?.let {
            Text(
                text = stringResource(
                    R.string.home_last_event_sync,
                    DateUtils.getRelativeTimeSpanString(it.toEpochMilliseconds()),
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private const val LAST_SYNC_HINT_MILLIS = 3_000L

/** The shortcuts side by side, scrollable, on one baseline. */
@Composable
private fun ShortcutSelector(
    shortcuts: List<RangeShortcut>,
    selected: RangeShortcut?,
    onSelect: (RangeShortcut) -> Unit,
    compactness: Float = 0f,
) {
    // In the pill the labels shrink a little and keep away from its rounded ends. The row is
    // scaled as a whole, so the text is not laid out anew in every frame
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(HomeTestTags.SHORTCUTS)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp + 4.dp * compactness)
            .scaledTo(1f - 0.2f * compactness, fromStart = true),
    ) {
        shortcuts.forEach { shortcut ->
            key(shortcut) {
                ShortcutText(
                    label = stringResource(shortcut.label),
                    selected = shortcut == selected,
                    onClick = { onSelect(shortcut) },
                    modifier = Modifier.alignByBaseline(),
                )
            }
        }
    }
}

/** Just the text: the selected one bold, larger and in the content color, the others thinner, smaller and in outline color. */
@Composable
private fun ShortcutText(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
        label = "color",
    )
    val scale by animateFloatAsState(if (selected) 1f else 0.75f, label = "scale")
    val baseStyle = MaterialTheme.typography.headlineSmall
    val style = baseStyle.copy(fontSize = baseStyle.fontSize * scale)

    Box(
        modifier.selectable(
            selected = selected,
            role = Role.Tab,
            interactionSource = null,
            indication = null,
            onClick = onClick,
        ),
    ) {
        // Invisible bold copy: reserves the bold width, so the weight never shifts the layout
        Text(
            text = label,
            style = style,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .alpha(0f)
                .clearAndSetSemantics {},
        )
        Text(
            text = label,
            color = color,
            style = style,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

private val RangeShortcut.label: Int
    get() = when (this) {
        RangeShortcut.TODAY -> R.string.home_shortcut_today
        RangeShortcut.THIS_WEEK -> R.string.home_shortcut_this_week
        RangeShortcut.LAST_WEEK -> R.string.home_shortcut_last_week
        RangeShortcut.THIS_MONTH -> R.string.home_shortcut_this_month
        RangeShortcut.LAST_MONTH -> R.string.home_shortcut_last_month
        RangeShortcut.ALL -> R.string.home_shortcut_all
    }

/** What the center of the ring shows: the selected calendar or group, or the total. */
private data class CenterLabel(val key: String, val name: String?, val color: Color?, val duration: Duration)

@Composable
private fun CalendarRing(
    page: HomePage,
    groups: List<GroupInfo>,
    size: Dp,
    selectedCalendar: CalendarTime?,
    selectedGroup: GroupInfo?,
    selectionStrength: Float,
    onCalendarSelect: (Long?) -> Unit,
    onGroupSelect: (Long) -> Unit,
    onClearSelection: () -> Unit,
    introKey: Any?,
) {
    val groupColor = { group: GroupInfo -> group.color?.let(::Color) }
    val outline = MaterialTheme.colorScheme.outline
    val hasSelection = selectedCalendar != null || selectedGroup != null
    val label = when {
        selectedCalendar != null -> CenterLabel(
            key = "calendar-${selectedCalendar.calendarId}",
            name = selectedCalendar.name,
            color = Color(selectedCalendar.color),
            duration = selectedCalendar.duration,
        )
        selectedGroup != null -> CenterLabel(
            key = "group-${selectedGroup.id}",
            name = selectedGroup.name,
            color = groupColor(selectedGroup) ?: MaterialTheme.colorScheme.onSurface,
            duration = page.groupDuration(selectedGroup.id),
        )
        else -> CenterLabel(key = "total", name = null, color = null, duration = page.totalDuration)
    }
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        DonutChart(
            slices = page.calendars.map {
                DonutSlice(
                    id = it.calendarId,
                    value = it.duration.inWholeSeconds.toFloat(),
                    color = Color(it.color),
                    groupId = it.groupId,
                )
            },
            groups = groups.map { group -> DonutGroup(group.id, groupColor(group) ?: outline) },
            selectedId = selectedCalendar?.calendarId,
            selectedGroupId = selectedGroup?.id,
            onGroupClick = onGroupSelect,
            selectionStrength = selectionStrength,
            // Unfold once the splash screen is gone, not hidden behind it
            playIntro = LocalAppStartState.current.splashDismissed.collectAsStateWithLifecycle().value,
            introKey = introKey,
            // A tap beside the ring clears the selection
            onSliceClick = { id -> if (id != null) onCalendarSelect(id) else if (hasSelection) onClearSelection() },
            // Laid out at full size and scaled, so the ring and its center shrink as one
            modifier = Modifier
                .scaledTo(size / RING_SIZE)
                .size(RING_SIZE)
                .testTag(HomeTestTags.RING),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(160.dp),
            ) {
                // Fixed width, so names of different lengths don't make it jump sideways
                AnimatedContent(
                    targetState = label,
                    transitionSpec = {
                        (slideInVertically { it / 2 } + fadeIn()) togetherWith
                            (slideOutVertically { -it / 2 } + fadeOut()) using
                            SizeTransform(clip = false) { _, _ -> snap() }
                    },
                    contentKey = { it.key },
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth(),
                    label = "center label",
                ) { label ->
                    Text(
                        text = label.name ?: stringResource(R.string.home_total),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.labelLarge,
                        color = label.color ?: MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                RollingText(
                    text = formatDuration(label.duration),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

/**
 * One event like in a day view of a calendar: the times on the left, a block in the calendar's
 * color on the right that gets taller the longer the event is.
 */
@Composable
private fun EventRow(event: EventItem, modifier: Modifier = Modifier) {
    val formatTime = rememberTimeFormatter()
    val timeZone = TimeZone.currentSystemDefault()
    fun Instant.time() = formatTime(toLocalDateTime(timeZone).time)
    val color = Color(event.color)
    val height = (event.duration.inWholeMinutes * EVENT_DP_PER_MINUTE)
        .coerceIn(EVENT_MIN_HEIGHT_DP, EVENT_MAX_HEIGHT_DP).dp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Wide enough for 12 hour times like "10:00 AM"
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.width(72.dp),
        ) {
            Text(
                text = event.start.time(),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                softWrap = false,
            )
            Text(
                text = event.end.time(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
            )
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .height(height)
                .clip(RoundedCornerShape(8.dp))
                .background(color.copy(alpha = 0.16f)),
        ) {
            Box(
                Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(color),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    text = event.title.ifBlank { stringResource(R.string.home_event_untitled) },
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = formatDuration(event.duration),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (event.isOngoing) {
                        Box(Modifier.size(6.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                        Text(
                            text = stringResource(R.string.home_event_ongoing),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

/** Heading of a day, shown when the range covers several days. */
@Composable
private fun DayHeader(date: LocalDate, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val text = remember(date) {
        val millis = date.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        DateUtils.formatDateTime(
            context,
            millis,
            DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_ALL,
        )
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
    )
}

private const val EVENT_DP_PER_MINUTE = 0.9f
private const val EVENT_MIN_HEIGHT_DP = 52f
private const val EVENT_MAX_HEIGHT_DP = 160f

@Preview(showBackground = true)
@Composable
private fun HomeContentPreview() {
    val now = Instant.parse("2026-11-12T15:00:00Z")
    val today = LocalDate(2026, 11, 12)
    ClockMeTheme(dynamicColor = false) {
        HomeContent(
            uiState = HomeUiState(
                shortcuts = RangeShortcut.available(today),
                today = today,
                unit = PageUnit.DAY,
                hasActiveCalendars = true,
                pages = mapOf(
                    0 to HomePage(
                        offset = 0,
                        firstDay = today,
                        range = ResolvedTimeRange(now - 900.minutes, now),
                        calendars = listOf(
                            CalendarTime(1, "Work", 0xFF4285F4.toInt(), 150.minutes),
                            CalendarTime(2, "Personal", 0xFF0B8043.toInt(), 45.minutes),
                        ),
                        events = listOf(
                            EventItem(1, 1, today, "Standup", 0xFF4285F4.toInt(), now - 60.minutes, now + 30.minutes, 60.minutes, true),
                            EventItem(2, 2, today, "Gym", 0xFF0B8043.toInt(), now - 300.minutes, now - 255.minutes, 45.minutes, false),
                        ),
                    ),
                ),
            ),
            onCalendarSelect = {},
            onGroupSelect = {},
            onClearSelection = {},
            onShortcutSelect = {},
            onPageChange = {},
        )
    }
}

/**
 * Measures the content as usual but takes up and draws it at [scale]; centered, or vertically
 * centered on its start edge if [fromStart].
 */
private fun Modifier.scaledTo(scale: Float, fromStart: Boolean = false) = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(maxWidth = Constraints.Infinity, maxHeight = Constraints.Infinity))
    val width = (placeable.width * scale).roundToInt()
    val height = (placeable.height * scale).roundToInt()
    layout(width, height) {
        val x = if (fromStart) 0 else (width - placeable.width) / 2
        placeable.placeWithLayer(x, (height - placeable.height) / 2) {
            scaleX = scale
            scaleY = scale
            if (fromStart) transformOrigin = TransformOrigin(0f, 0.5f)
        }
    }
}
