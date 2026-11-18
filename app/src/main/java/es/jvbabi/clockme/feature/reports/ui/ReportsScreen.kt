package es.jvbabi.clockme.feature.reports.ui

import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.TimePickerDialogDefaults
import androidx.compose.material3.TimePickerDisplayMode
import com.phosphor.icons.regular.ArrowRight
import com.phosphor.icons.regular.Clock
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.key
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.Dp
import com.phosphor.icons.PhIcons
import com.phosphor.icons.regular.CaretDown
import es.jvbabi.clockme.data.calendar.Calendar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.jvbabi.clockme.R
import es.jvbabi.clockme.core.ui.chart.DonutChart
import es.jvbabi.clockme.core.ui.chart.DonutGroup
import es.jvbabi.clockme.core.ui.chart.DonutSlice
import es.jvbabi.clockme.core.ui.formatDuration
import es.jvbabi.clockme.core.ui.rememberTimeFormatter
import es.jvbabi.clockme.navigation.LocalBottomNavBarHeight
import es.jvbabi.clockme.ui.theme.CirclesTheme
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.koin.androidx.compose.koinViewModel
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

@Composable
fun ReportsScreen(viewModel: ReportsViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ReportsContent(uiState = uiState, onAction = viewModel::onAction)
}

object ReportsTestTags {
    const val FROM_DATE = "reports_from_date"
    const val FROM_TIME = "reports_from_time"
    const val TO_DATE = "reports_to_date"
    const val TO_TIME = "reports_to_time"
    const val PREVIEW_RING = "reports_preview_ring"
    const val PREVIEW_TOTAL = "reports_preview_total"
    const val INCLUDE_EVENTS = "reports_include_events"
    fun group(id: Long) = "reports_group_$id"
    fun groupCheckbox(id: Long) = "reports_group_checkbox_$id"
    fun calendar(id: Long) = "reports_calendar_$id"
}

/** Which part of the range a picker dialog is open for. */
private enum class RangePicker { FROM_DATE, FROM_TIME, TO_DATE, TO_TIME }

@Composable
internal fun ReportsContent(
    uiState: ReportsUiState,
    onAction: (ReportsAction) -> Unit,
) {
    var openPicker by rememberSaveable { mutableStateOf<RangePicker?>(null) }

    Scaffold { innerPadding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = innerPadding.calculateTopPadding(), bottom = LocalBottomNavBarHeight.current)
                .padding(16.dp),
        ) {
            Text(
                text = stringResource(R.string.reports_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            ) {
                RangeEndTile(
                    label = R.string.reports_from,
                    value = uiState.from,
                    isError = !uiState.isRangeValid,
                    onClick = { openPicker = RangePicker.FROM_DATE },
                    onTimeClick = { openPicker = RangePicker.FROM_TIME },
                    tag = ReportsTestTags.FROM_DATE,
                    timeTag = ReportsTestTags.FROM_TIME,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
                Icon(
                    imageVector = PhIcons.Regular.ArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp).size(20.dp),
                )
                RangeEndTile(
                    label = R.string.reports_to,
                    value = uiState.to,
                    isError = !uiState.isRangeValid,
                    onClick = { openPicker = RangePicker.TO_DATE },
                    onTimeClick = { openPicker = RangePicker.TO_TIME },
                    tag = ReportsTestTags.TO_DATE,
                    timeTag = ReportsTestTags.TO_TIME,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
            AnimatedVisibility(visible = !uiState.isRangeValid) {
                Text(
                    text = stringResource(R.string.reports_range_invalid),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
            uiState.calendars?.let { tree ->
                Text(
                    text = stringResource(R.string.reports_calendars),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
                CalendarTreeCard(uiState, tree, onAction)
            }
            Text(
                text = stringResource(R.string.reports_preview),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            PreviewCard(uiState, onAction)
        }
    }

    // A picked date leads straight on to its time; cancelling that keeps the new date
    val dismiss = { openPicker = null }
    when (openPicker) {
        RangePicker.FROM_DATE -> DatePickerDialog(uiState.from.date, R.string.reports_from, dismiss) {
            onAction(ReportsAction.FromDateChange(it))
            openPicker = RangePicker.FROM_TIME
        }
        RangePicker.FROM_TIME -> TimePickerDialog(uiState.from.time, R.string.reports_from, dismiss) {
            onAction(ReportsAction.FromTimeChange(it))
            openPicker = null
        }
        RangePicker.TO_DATE -> DatePickerDialog(uiState.to.date, R.string.reports_to, dismiss) {
            onAction(ReportsAction.ToDateChange(it))
            openPicker = RangePicker.TO_TIME
        }
        RangePicker.TO_TIME -> TimePickerDialog(uiState.to.time, R.string.reports_to, dismiss) {
            onAction(ReportsAction.ToTimeChange(it))
            openPicker = null
        }
        null -> Unit
    }
}

/**
 * The groups with their calendars folded away beneath them, then the calendars in no group. A
 * group's box takes in or leaves out all its calendars at once and shows when only some are in.
 */
@Composable
private fun CalendarTreeCard(
    uiState: ReportsUiState,
    tree: ReportCalendarTree,
    onAction: (ReportsAction) -> Unit,
) {
    var expandedGroups by rememberSaveable { mutableStateOf(emptySet<Long>()) }
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(vertical = 8.dp)) {
            if (tree.isEmpty) {
                Text(
                    text = stringResource(R.string.reports_no_calendars),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            tree.groups.forEach { group ->
                key("group-${group.id}") {
                    val expanded = group.id in expandedGroups
                    GroupRow(
                        group = group,
                        selection = uiState.selectionOf(group),
                        selectedCount = group.calendars.count(uiState::isSelected),
                        expanded = expanded,
                        onExpandToggle = {
                            expandedGroups = if (expanded) expandedGroups - group.id else expandedGroups + group.id
                        },
                        onToggle = { onAction(ReportsAction.GroupToggle(group.id)) },
                    )
                    AnimatedVisibility(
                        visible = expanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                    ) {
                        Column {
                            group.calendars.forEach { calendar ->
                                CalendarRow(
                                    calendar = calendar,
                                    checked = uiState.isSelected(calendar),
                                    onToggle = { onAction(ReportsAction.CalendarToggle(calendar.id)) },
                                    indent = TREE_INDENT,
                                )
                            }
                        }
                    }
                }
            }
            tree.ungrouped.forEach { calendar ->
                key("calendar-${calendar.id}") {
                    CalendarRow(
                        calendar = calendar,
                        checked = uiState.isSelected(calendar),
                        onToggle = { onAction(ReportsAction.CalendarToggle(calendar.id)) },
                    )
                }
            }
        }
    }
}

/** A small ring of the selected calendars' time with the total beside it, and the report's options. */
@Composable
private fun PreviewCard(
    uiState: ReportsUiState,
    onAction: (ReportsAction) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val preview = uiState.preview
    Surface(
        shape = MaterialTheme.shapes.large,
        color = colors.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp),
            ) {
                DonutChart(
                    slices = preview?.calendars.orEmpty().map {
                        DonutSlice(
                            id = it.calendarId,
                            value = it.duration.inWholeSeconds.toFloat(),
                            color = Color(it.color),
                            groupId = it.groupId,
                        )
                    },
                    groups = uiState.calendars?.groups.orEmpty().map { group ->
                        DonutGroup(group.id, group.color?.let(::Color) ?: colors.outline)
                    },
                    selectedId = null,
                    onSliceClick = {},
                    strokeWidth = 14.dp,
                    selectedExtraWidth = 0.dp,
                    groupBandWidth = 3.dp,
                    groupBandGap = 2.dp,
                    groupTouchPadding = 0.dp,
                    playIntro = preview != null,
                    modifier = Modifier.size(PREVIEW_RING_SIZE).testTag(ReportsTestTags.PREVIEW_RING),
                )
                Column(Modifier.weight(1f).padding(start = 20.dp)) {
                    Text(
                        text = stringResource(R.string.home_total).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.primary,
                    )
                    Text(
                        text = formatDuration(preview?.total ?: Duration.ZERO),
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.testTag(ReportsTestTags.PREVIEW_TOTAL),
                    )
                    if (preview != null) {
                        val events = pluralStringResource(R.plurals.reports_event_count, preview.eventCount, preview.eventCount)
                        val calendarCount = preview.calendars.size
                        val calendars = pluralStringResource(R.plurals.reports_calendar_count, calendarCount, calendarCount)
                        Text(
                            text = "$events $calendars",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                        )
                    }
                }
            }
            HorizontalDivider(color = colors.outlineVariant, modifier = Modifier.padding(horizontal = 16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = uiState.includeEvents,
                        role = Role.Checkbox,
                        onValueChange = { onAction(ReportsAction.IncludeEventsToggle) },
                    )
                    .testTag(ReportsTestTags.INCLUDE_EVENTS)
                    .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.reports_include_events), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = stringResource(R.string.reports_include_events_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
                // The row handles the click
                Checkbox(checked = uiState.includeEvents, onCheckedChange = null, modifier = Modifier.padding(12.dp))
            }
        }
    }
}

private val PREVIEW_RING_SIZE = 112.dp

/** How far a group's calendars sit in under it: past the caret, so their dots line up with the group's. */
private val TREE_INDENT = 40.dp

@Composable
private fun GroupRow(
    group: ReportGroup,
    selection: GroupSelection,
    selectedCount: Int,
    expanded: Boolean,
    onExpandToggle: () -> Unit,
    onToggle: () -> Unit,
) {
    val caretRotation by animateFloatAsState(if (expanded) 0f else -90f, label = "caret")
    val expandLabel = stringResource(if (expanded) R.string.reports_collapse_group else R.string.reports_expand_group)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = expandLabel, onClick = onExpandToggle)
            .testTag(ReportsTestTags.group(group.id))
            .padding(start = 8.dp, end = 4.dp),
    ) {
        Icon(
            imageVector = PhIcons.Regular.CaretDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(32.dp).padding(6.dp).rotate(caretRotation),
        )
        ColorDot(group.color, Modifier.padding(start = 8.dp))
        Column(Modifier.weight(1f).padding(start = 16.dp)) {
            Text(text = group.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = stringResource(R.string.reports_group_count, selectedCount, group.calendars.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TriStateCheckbox(
            state = when (selection) {
                GroupSelection.ALL -> ToggleableState.On
                GroupSelection.SOME -> ToggleableState.Indeterminate
                GroupSelection.NONE -> ToggleableState.Off
            },
            onClick = onToggle,
            modifier = Modifier.testTag(ReportsTestTags.groupCheckbox(group.id)),
        )
    }
}

@Composable
private fun CalendarRow(
    calendar: Calendar,
    checked: Boolean,
    onToggle: () -> Unit,
    indent: Dp = 0.dp,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggle() })
            .testTag(ReportsTestTags.calendar(calendar.id))
            .padding(start = 16.dp + indent, end = 4.dp, top = 4.dp, bottom = 4.dp),
    ) {
        ColorDot(calendar.color)
        Text(
            text = calendar.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f).padding(start = 16.dp),
        )
        // The row handles the click
        Checkbox(checked = checked, onCheckedChange = null, modifier = Modifier.padding(12.dp))
    }
}

@Composable
private fun ColorDot(color: Int?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(16.dp)
            .then(
                if (color != null) {
                    Modifier.background(Color(color), CircleShape)
                } else {
                    Modifier.border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                },
            ),
    )
}

/**
 * One end of the range as a tile: its label, the date large with the weekday beneath, and the
 * time as a chip. The tile opens the date and then the time; the chip only the time.
 */
@Composable
private fun RangeEndTile(
    @StringRes label: Int,
    value: LocalDateTime,
    isError: Boolean,
    onClick: () -> Unit,
    onTimeClick: () -> Unit,
    tag: String,
    timeTag: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val formatTime = rememberTimeFormatter()
    val labelText = stringResource(label)
    val dateText = rememberDateText(value.date, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_ABBREV_MONTH)
    val weekdayText = rememberDateText(value.date, DateUtils.FORMAT_SHOW_WEEKDAY)
    val timeText = formatTime(value.time)
    val pickDate = stringResource(R.string.reports_pick_date)
    val pickTime = stringResource(R.string.reports_pick_time)
    val borderColor by animateColorAsState(if (isError) colors.error else Color.Transparent, label = "tile border")
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        color = colors.surfaceContainerHigh,
        border = BorderStroke(1.5.dp, borderColor),
        modifier = modifier
            .testTag(tag)
            .semantics { contentDescription = "$labelText: $weekdayText, $dateText, $timeText. $pickDate" },
    ) {
        Column(Modifier.padding(start = 16.dp, end = 12.dp, top = 14.dp, bottom = 12.dp)) {
            Text(
                text = labelText.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = if (isError) colors.error else colors.primary,
            )
            Text(
                text = dateText,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(top = 6.dp).basicMarquee(),
            )
            Text(
                text = weekdayText,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                maxLines = 1,
            )
            Surface(
                onClick = onTimeClick,
                shape = CircleShape,
                color = colors.secondaryContainer,
                contentColor = colors.onSecondaryContainer,
                modifier = Modifier
                    .padding(top = 12.dp)
                    .testTag(timeTag)
                    .semantics { contentDescription = "$labelText: $timeText. $pickTime" },
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(start = 10.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
                ) {
                    Icon(PhIcons.Regular.Clock, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(timeText, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                }
            }
        }
    }
}

/** The date as the system writes it, with the parts asked for by [flags] of [DateUtils]. */
@Composable
private fun rememberDateText(date: LocalDate, flags: Int): String {
    val context = LocalContext.current
    return remember(date, flags) {
        val millis = date.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        DateUtils.formatDateTime(context, millis, flags)
    }
}

/** [onConfirm] gets the picked date; the caller decides what follows, so the dialog does not close itself. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialog(
    initial: LocalDate,
    @StringRes title: Int,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    // The date picker speaks in UTC midnights
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis?.let { millis ->
                        onConfirm(Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date)
                    }
                },
                enabled = state.selectedDateMillis != null,
            ) { Text(stringResource(R.string.dialog_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
        },
    ) {
        DatePicker(
            state = state,
            title = {
                Text(
                    text = stringResource(title),
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp),
                )
            },
        )
    }
}

/** Material's time dialog, with the switch between the clock and typing the time in. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initial: LocalTime,
    @StringRes title: Int,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute,
        is24Hour = DateFormat.is24HourFormat(LocalContext.current),
    )
    var displayMode by remember { mutableStateOf(TimePickerDisplayMode.Picker) }
    TimePickerDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(bottom = 20.dp),
            )
        },
        modeToggleButton = {
            TimePickerDialogDefaults.DisplayModeToggle(
                onDisplayModeChange = {
                    displayMode = if (displayMode == TimePickerDisplayMode.Picker) {
                        TimePickerDisplayMode.Input
                    } else {
                        TimePickerDisplayMode.Picker
                    }
                },
                displayMode = displayMode,
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime(state.hour, state.minute)) }) {
                Text(stringResource(R.string.dialog_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
        },
    ) {
        if (displayMode == TimePickerDisplayMode.Picker) TimePicker(state = state) else TimeInput(state = state)
    }
}

@Preview(showBackground = true)
@Composable
private fun ReportsContentPreview() {
    CirclesTheme(dynamicColor = false) {
        ReportsContent(
            uiState = ReportsUiState(
                from = LocalDateTime.parse("2026-11-18T00:00"),
                to = LocalDateTime.parse("2026-11-18T14:30"),
                calendars = ReportCalendarTree(
                    groups = listOf(
                        ReportGroup(
                            id = 1,
                            name = "University",
                            color = 0xFF8E24AA.toInt(),
                            calendars = listOf(
                                Calendar(1, "Lectures", 0xFF4285F4.toInt()),
                                Calendar(2, "Study", 0xFF0B8043.toInt()),
                            ),
                        ),
                    ),
                    ungrouped = listOf(Calendar(3, "Gym", 0xFFF4511E.toInt())),
                ),
                deselectedCalendarIds = setOf(2),
                preview = ReportPreview(
                    calendars = listOf(
                        PreviewCalendar(1, 0xFF4285F4.toInt(), 14.hours, groupId = 1),
                        PreviewCalendar(3, 0xFFF4511E.toInt(), 5.hours),
                    ),
                    eventCount = 23,
                ),
            ),
            onAction = {},
        )
    }
}
