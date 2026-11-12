package es.jvbabi.clockme.feature.home.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.jvbabi.clockme.R
import es.jvbabi.clockme.core.ui.ObserveAsEvents
import es.jvbabi.clockme.navigation.LocalBottomNavBarHeight
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding(), bottom = LocalBottomNavBarHeight.current)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
            )
            when (val count = uiState.activeCalendarCount) {
                null -> CircularProgressIndicator()
                0 -> Text(stringResource(R.string.home_no_active_calendars))
                else -> Text(pluralStringResource(R.plurals.home_active_calendars, count, count))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeContentPreview() {
    ClockMeTheme {
        HomeContent(uiState = HomeUiState(activeCalendarCount = 2))
    }
}
