package es.jvbabi.clockme.feature.reports.ui

import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.jvbabi.clockme.R
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
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    RangeEndRow(
                        label = R.string.reports_from,
                        value = uiState.from,
                        onDateClick = { openPicker = RangePicker.FROM_DATE },
                        onTimeClick = { openPicker = RangePicker.FROM_TIME },
                        dateTag = ReportsTestTags.FROM_DATE,
                        timeTag = ReportsTestTags.FROM_TIME,
                    )
                    RangeEndRow(
                        label = R.string.reports_to,
                        value = uiState.to,
                        onDateClick = { openPicker = RangePicker.TO_DATE },
                        onTimeClick = { openPicker = RangePicker.TO_TIME },
                        dateTag = ReportsTestTags.TO_DATE,
                        timeTag = ReportsTestTags.TO_TIME,
                    )
                    if (!uiState.isRangeValid) {
                        Text(
                            text = stringResource(R.string.reports_range_invalid),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
            uiState.calendars?.let { tree ->
                Text(
                    text = stringResource(R.string.reports_calendars),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
                CalendarTreeCard(uiState, tree, onAction)
            }
        }
    }

    val dismiss = { openPicker = null }
    when (openPicker) {
        RangePicker.FROM_DATE -> DatePickerDialog(uiState.from.date, dismiss) { onAction(ReportsAction.FromDateChange(it)) }
        RangePicker.FROM_TIME -> TimePickerDialog(uiState.from.time, dismiss) { onAction(ReportsAction.FromTimeChange(it)) }
        RangePicker.TO_DATE -> DatePickerDialog(uiState.to.date, dismiss) { onAction(ReportsAction.ToDateChange(it)) }
        RangePicker.TO_TIME -> TimePickerDialog(uiState.to.time, dismiss) { onAction(ReportsAction.ToTimeChange(it)) }
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

/** One end of the range: its label, then the date and the time, each tappable to change it. */
@Composable
private fun RangeEndRow(
    @StringRes label: Int,
    value: LocalDateTime,
    onDateClick: () -> Unit,
    onTimeClick: () -> Unit,
    dateTag: String,
    timeTag: String,
) {
    val formatTime = rememberTimeFormatter()
    val labelText = stringResource(label)
    val dateText = rememberDateText(value.date)
    val timeText = formatTime(value.time)
    val pickDate = stringResource(R.string.reports_pick_date)
    val pickTime = stringResource(R.string.reports_pick_time)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = labelText,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.width(40.dp),
        )
        FilledTonalButton(
            onClick = onDateClick,
            modifier = Modifier
                .weight(1f)
                .testTag(dateTag)
                .semantics { contentDescription = "$labelText: $dateText, $pickDate" },
        ) { Text(dateText, maxLines = 1) }
        FilledTonalButton(
            onClick = onTimeClick,
            modifier = Modifier
                .testTag(timeTag)
                .semantics { contentDescription = "$labelText: $timeText, $pickTime" },
        ) { Text(timeText, maxLines = 1) }
    }
}

/** The date in the system's short form with weekday, like "Wed, Nov 18, 2026". */
@Composable
private fun rememberDateText(date: LocalDate): String {
    val context = LocalContext.current
    return remember(date) {
        val millis = date.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        DateUtils.formatDateTime(
            context,
            millis,
            DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_ABBREV_ALL,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialog(
    initial: LocalDate,
    onDismiss: () -> Unit,
    onPick: (LocalDate) -> Unit,
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
                        onPick(Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date)
                    }
                    onDismiss()
                },
                enabled = state.selectedDateMillis != null,
            ) { Text(stringResource(R.string.dialog_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
        },
    ) {
        DatePicker(state = state)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initial: LocalTime,
    onDismiss: () -> Unit,
    onPick: (LocalTime) -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute,
        is24Hour = DateFormat.is24HourFormat(LocalContext.current),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onPick(LocalTime(state.hour, state.minute))
                    onDismiss()
                },
            ) { Text(stringResource(R.string.dialog_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
        },
        text = { TimePicker(state = state) },
    )
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
            ),
            onAction = {},
        )
    }
}
