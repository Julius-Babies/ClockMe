package es.jvbabi.clockme.feature.reports.ui

import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.annotation.StringRes
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
            ),
            onAction = {},
        )
    }
}
