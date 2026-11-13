package es.jvbabi.clockme.feature.home.ui

import android.text.format.DateUtils
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
        onClearSelection = viewModel::onClearSelection,
        onShortcutSelect = viewModel::onShortcutSelect,
    )
}

object HomeTestTags {
    const val RING = "home_ring"
    const val SHORTCUTS = "home_shortcuts"
}

@Composable
internal fun HomeContent(
    uiState: HomeUiState,
    onCalendarSelect: (Long?) -> Unit,
    onClearSelection: () -> Unit,
    onShortcutSelect: (RangeShortcut) -> Unit,
) {
    // 0 while no back gesture is running, up to 1 while the user swipes back
    var backProgress by remember { mutableFloatStateOf(0f) }
    // Back only clears the selection; without one it is left to the navigation
    PredictiveBackHandler(enabled = uiState.selectedCalendarId != null) { events ->
        try {
            events.collect { backProgress = it.progress }
            onClearSelection()
        } finally {
            backProgress = 0f
        }
    }

    Scaffold { innerPadding ->
        val calendars = uiState.calendars
        Box(Modifier.fillMaxSize()) {
            when {
                calendars == null -> Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }

                calendars.isEmpty() -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = innerPadding.calculateTopPadding(), bottom = LocalBottomNavBarHeight.current)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stringResource(R.string.home_no_active_calendars), textAlign = TextAlign.Center)
                }

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = innerPadding.calculateTopPadding() + 16.dp,
                        bottom = LocalBottomNavBarHeight.current + 16.dp,
                    ),
                ) {
                    item { Header(uiState, onShortcutSelect) }
                    item {
                        CalendarRing(
                            calendars = calendars,
                            groups = uiState.groups,
                            selected = uiState.selectedCalendar,
                            totalDuration = uiState.totalDuration,
                            selectionStrength = 1f - backProgress,
                            onCalendarSelect = onCalendarSelect,
                        )
                    }
                    item {
                        Text(
                            text = uiState.selectedCalendar
                                ?.let { stringResource(R.string.home_events_of_calendar, it.name) }
                                ?: stringResource(R.string.home_events),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
                        )
                    }
                    if (uiState.events.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.home_no_events),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }
                    }
                    val eventsByDay = uiState.events.groupBy { it.date }
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
            LastSyncHint(
                lastSync = uiState.lastEventSync,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = LocalBottomNavBarHeight.current + 8.dp),
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

@Composable
private fun Header(uiState: HomeUiState, onShortcutSelect: (RangeShortcut) -> Unit) {
    ShortcutSelector(
        shortcuts = uiState.shortcuts,
        selected = uiState.selectedShortcut,
        onSelect = onShortcutSelect,
    )
}

/** The shortcuts side by side, scrollable, on one baseline. */
@Composable
private fun ShortcutSelector(
    shortcuts: List<RangeShortcut>,
    selected: RangeShortcut?,
    onSelect: (RangeShortcut) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(HomeTestTags.SHORTCUTS)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
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
        RangeShortcut.YESTERDAY -> R.string.home_shortcut_yesterday
        RangeShortcut.THIS_WEEK -> R.string.home_shortcut_this_week
        RangeShortcut.THIS_MONTH -> R.string.home_shortcut_this_month
        RangeShortcut.ALL -> R.string.home_shortcut_all
    }

@Composable
private fun CalendarRing(
    calendars: List<CalendarTime>,
    groups: List<GroupInfo>,
    selected: CalendarTime?,
    totalDuration: Duration,
    selectionStrength: Float,
    onCalendarSelect: (Long?) -> Unit,
) {
    Box(Modifier.fillMaxWidth().padding(top = 16.dp), contentAlignment = Alignment.Center) {
        DonutChart(
            slices = calendars.map {
                DonutSlice(
                    id = it.calendarId,
                    value = it.duration.inWholeSeconds.toFloat(),
                    color = Color(it.color),
                    groupId = it.groupId,
                )
            },
            groups = groups.map { group ->
                DonutGroup(group.id, group.color?.let(::Color) ?: MaterialTheme.colorScheme.outline)
            },
            selectedId = selected?.calendarId,
            selectionStrength = selectionStrength,
            // Unfold once the splash screen is gone, not hidden behind it
            playIntro = LocalAppStartState.current.splashDismissed.collectAsStateWithLifecycle().value,
            onSliceClick = { id -> onCalendarSelect(id ?: selected?.calendarId) },
            modifier = Modifier.size(260.dp).testTag(HomeTestTags.RING),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(160.dp),
            ) {
                // Fixed width, so names of different lengths don't make it jump sideways
                AnimatedContent(
                    targetState = selected,
                    transitionSpec = {
                        (slideInVertically { it / 2 } + fadeIn()) togetherWith
                            (slideOutVertically { -it / 2 } + fadeOut()) using
                            SizeTransform(clip = false) { _, _ -> snap() }
                    },
                    contentKey = { it?.calendarId },
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth(),
                    label = "center label",
                ) { calendar ->
                    Text(
                        text = calendar?.name ?: stringResource(R.string.home_total),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.labelLarge,
                        color = calendar?.let { Color(it.color) } ?: MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                RollingText(
                    text = formatDuration(selected?.duration ?: totalDuration),
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
    ClockMeTheme(dynamicColor = false) {
        HomeContent(
            uiState = HomeUiState(
                shortcuts = RangeShortcut.entries,
                calendars = listOf(
                    CalendarTime(1, "Work", 0xFF4285F4.toInt(), 150.minutes),
                    CalendarTime(2, "Personal", 0xFF0B8043.toInt(), 45.minutes),
                ),
                selectedCalendarId = null,
                events = listOf(
                    EventItem(1, 1, LocalDate(2026, 11, 12), "Standup", 0xFF4285F4.toInt(), now - 60.minutes, now + 30.minutes, 60.minutes, true),
                    EventItem(2, 2, LocalDate(2026, 11, 12), "Gym", 0xFF0B8043.toInt(), now - 300.minutes, now - 255.minutes, 45.minutes, false),
                ),
            ),
            onCalendarSelect = {},
            onClearSelection = {},
            onShortcutSelect = {},
        )
    }
}
