package es.jvbabi.clockme.feature.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.jvbabi.clockme.R
import es.jvbabi.clockme.core.ui.ObserveAsEvents
import es.jvbabi.clockme.data.calendar.Calendar
import es.jvbabi.clockme.data.calendar.CalendarAccount
import es.jvbabi.clockme.ui.theme.ClockMeTheme
import org.koin.androidx.compose.koinViewModel

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
    HomeContent(uiState = uiState)
}

@Composable
private fun HomeContent(uiState: HomeUiState) {
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
            ) { Text(stringResource(R.string.home_no_calendars)) }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = innerPadding,
            ) {
                item {
                    Text(
                        text = stringResource(R.string.home_calendars_title),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                    )
                }
                accounts.forEach { account ->
                    item(key = account.id) { AccountHeader(account) }
                    items(account.calendars, key = { it.id }) { CalendarRow(it) }
                }
            }
        }
    }
}

@Composable
private fun AccountHeader(account: CalendarAccount) {
    Text(
        text = account.name,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun CalendarRow(calendar: Calendar) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(Color(calendar.color), CircleShape),
        )
        Text(calendar.name, style = MaterialTheme.typography.bodyLarge)
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeContentPreview() {
    ClockMeTheme {
        HomeContent(
            uiState = HomeUiState(
                accounts = listOf(
                    CalendarAccount(
                        id = "com.google/jane@example.com",
                        name = "jane@example.com",
                        calendars = listOf(
                            Calendar(id = 1, name = "Personal", color = 0xFF4285F4.toInt()),
                            Calendar(id = 2, name = "Work", color = 0xFF0B8043.toInt()),
                        ),
                    ),
                    CalendarAccount(
                        id = "LOCAL/Device",
                        name = "Device",
                        calendars = listOf(
                            Calendar(id = 3, name = "Birthdays", color = 0xFFD50000.toInt()),
                        ),
                    ),
                ),
            ),
        )
    }
}
