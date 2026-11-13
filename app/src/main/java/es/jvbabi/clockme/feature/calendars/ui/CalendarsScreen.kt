package es.jvbabi.clockme.feature.calendars.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phosphor.icons.PhIcons
import com.phosphor.icons.regular.Check
import com.phosphor.icons.regular.Plus
import com.phosphor.icons.regular.Prohibit
import es.jvbabi.clockme.R
import es.jvbabi.clockme.data.calendar.Calendar
import es.jvbabi.clockme.data.calendar.CalendarAccount
import es.jvbabi.clockme.data.group.CalendarGroup
import es.jvbabi.clockme.navigation.LocalBottomNavBarHeight
import es.jvbabi.clockme.ui.theme.ClockMeTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun CalendarsScreen(viewModel: CalendarsViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CalendarsContent(uiState = uiState, onAction = viewModel::onAction)
}

@Composable
internal fun CalendarsContent(
    uiState: CalendarsUiState,
    onAction: (CalendarsAction) -> Unit,
) {
    Scaffold { innerPadding ->
        val accounts = uiState.accounts
        when {
            accounts == null -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            accounts.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) { Text(stringResource(R.string.calendars_empty)) }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    // The bottom bar floats over the list
                    bottom = LocalBottomNavBarHeight.current + 16.dp,
                ),
            ) {
                item {
                    Text(
                        text = stringResource(R.string.calendars_title),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                    )
                }
                accounts.forEach { account ->
                    item(key = account.id) { AccountHeader(account.name) }
                    items(account.calendars, key = { it.id }) { calendar ->
                        CalendarRow(
                            calendar = calendar,
                            checked = calendar.id in uiState.activeCalendarIds,
                            onClick = { onAction(CalendarsAction.CalendarClick(account, calendar)) },
                        )
                    }
                }
                item(key = "groups-header") {
                    GroupsHeader(onCreate = { onAction(CalendarsAction.CreateGroup) })
                }
                if (uiState.groups.isEmpty()) {
                    item(key = "groups-empty") {
                        Text(
                            text = stringResource(R.string.groups_empty),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    }
                }
                items(uiState.groups, key = { "group-${it.id}" }) { group ->
                    GroupCard(
                        group = group,
                        calendars = group.calendarIds.mapNotNull { uiState.activeCalendarsById[it] }.sortedBy { it.name },
                        onClick = { onAction(CalendarsAction.EditGroup(group)) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }

    uiState.groupEditor?.let { editor ->
        GroupEditorSheet(
            editor = editor,
            accounts = uiState.activeAccounts,
            groupOfCalendar = uiState.groupOfCalendar,
            onAction = onAction,
        )
    }
}

@Composable
private fun AccountHeader(name: String) {
    Text(
        text = name,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun CalendarRow(
    calendar: Calendar,
    checked: Boolean,
    onClick: () -> Unit,
    supportingText: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onClick() })
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ColorDot(calendar.color)
        Column(Modifier.weight(1f)) {
            Text(text = calendar.name, style = MaterialTheme.typography.bodyLarge)
            if (supportingText != null) {
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        // Click is handled by the row
        Checkbox(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun ColorDot(color: Int?, modifier: Modifier = Modifier) {
    val shape = CircleShape
    Box(
        modifier = modifier
            .size(16.dp)
            .then(
                if (color != null) {
                    Modifier.background(Color(color), shape)
                } else {
                    Modifier.border(1.5.dp, MaterialTheme.colorScheme.outline, shape)
                },
            ),
    )
}

@Composable
private fun GroupsHeader(onCreate: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 32.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.groups_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onCreate) {
            Icon(PhIcons.Regular.Plus, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(6.dp))
            Text(stringResource(R.string.groups_create))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GroupCard(
    group: CalendarGroup,
    calendars: List<Calendar>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ColorDot(group.color)
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            if (calendars.isEmpty()) {
                Text(
                    text = stringResource(R.string.groups_no_calendars),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    calendars.forEach { calendar ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .background(Color(calendar.color).copy(alpha = 0.16f), CircleShape)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Box(Modifier.size(8.dp).background(Color(calendar.color), CircleShape))
                            Text(calendar.name, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun GroupEditorSheet(
    editor: GroupEditorState,
    accounts: List<CalendarAccount>,
    groupOfCalendar: Map<Long, CalendarGroup>,
    onAction: (CalendarsAction) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = { onAction(CalendarsAction.EditorDismiss) },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            Text(
                text = stringResource(if (editor.isNew) R.string.groups_new else R.string.groups_edit),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            OutlinedTextField(
                value = editor.name,
                onValueChange = { onAction(CalendarsAction.EditorNameChange(it)) },
                label = { Text(stringResource(R.string.groups_name)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            )

            Text(
                text = stringResource(R.string.groups_color),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            ) {
                ColorOption(color = null, selected = editor.color == null, onClick = { onAction(CalendarsAction.EditorColorChange(null)) })
                GROUP_COLORS.forEach { color ->
                    ColorOption(color = color, selected = editor.color == color, onClick = { onAction(CalendarsAction.EditorColorChange(color)) })
                }
            }

            Text(
                text = stringResource(R.string.groups_calendars),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp),
            )
            if (accounts.isEmpty()) {
                Text(
                    text = stringResource(R.string.groups_no_active_calendars),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
            }
            accounts.forEach { account ->
                AccountHeader(account.name)
                account.calendars.forEach { calendar ->
                    // A calendar is in one group only, selecting it here moves it
                    val otherGroup = groupOfCalendar[calendar.id]?.takeIf { it.id != editor.groupId }
                    CalendarRow(
                        calendar = calendar,
                        checked = calendar.id in editor.calendarIds,
                        onClick = { onAction(CalendarsAction.EditorCalendarToggle(calendar.id)) },
                        supportingText = otherGroup
                            ?.takeIf { calendar.id !in editor.calendarIds }
                            ?.let { stringResource(R.string.groups_calendar_in_other_group, it.name) },
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!editor.isNew) {
                    TextButton(onClick = { onAction(CalendarsAction.EditorDelete) }) {
                        Text(stringResource(R.string.groups_delete), color = MaterialTheme.colorScheme.error)
                    }
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = { onAction(CalendarsAction.EditorSave) },
                    enabled = editor.canSave,
                ) {
                    Text(stringResource(R.string.groups_save))
                }
            }
        }
    }
}

/** A color to pick, or "no color" if [color] is `null`. */
@Composable
private fun ColorOption(color: Int?, selected: Boolean, onClick: () -> Unit) {
    val description = if (color == null) stringResource(R.string.groups_no_color) else null
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .then(
                if (color != null) {
                    Modifier.background(Color(color))
                } else {
                    Modifier.border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                },
            )
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { if (description != null) contentDescription = description },
    ) {
        when {
            selected -> Icon(
                PhIcons.Regular.Check,
                contentDescription = null,
                tint = if (color != null) Color.White else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp),
            )
            color == null -> Icon(
                PhIcons.Regular.Prohibit,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CalendarsContentPreview() {
    val work = Calendar(id = 1, name = "Work", color = 0xFF4285F4.toInt())
    val personal = Calendar(id = 2, name = "Personal", color = 0xFF0B8043.toInt())
    ClockMeTheme {
        CalendarsContent(
            uiState = CalendarsUiState(
                accounts = listOf(
                    CalendarAccount(id = "com.google/jane@example.com", name = "jane@example.com", calendars = listOf(personal, work)),
                ),
                activeCalendarIds = setOf(1),
                groups = listOf(CalendarGroup(id = 1, name = "University", color = GROUP_COLORS[5], calendarIds = setOf(1, 2))),
            ),
            onAction = {},
        )
    }
}
