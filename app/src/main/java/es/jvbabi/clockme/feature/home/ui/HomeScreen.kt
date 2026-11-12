package es.jvbabi.clockme.feature.home.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.jvbabi.clockme.R
import es.jvbabi.clockme.data.permission.PermissionStatus
import es.jvbabi.clockme.ui.theme.ClockMeTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeScreen(viewModel: HomeViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeContent(
        uiState = uiState,
        onRequestPermission = viewModel::onRequestCalendarPermission,
        onOpenAppSettings = viewModel::onOpenAppSettings,
    )
}

@Composable
private fun HomeContent(
    uiState: HomeUiState,
    onRequestPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.home_calendar_permission_title),
                style = MaterialTheme.typography.titleLarge,
            )
            when (uiState.calendarPermission) {
                null -> CircularProgressIndicator()
                PermissionStatus.Granted ->
                    Text(stringResource(R.string.permission_status_granted))
                PermissionStatus.NotRequested -> {
                    Text(stringResource(R.string.permission_status_not_requested))
                    Button(onClick = onRequestPermission) {
                        Text(stringResource(R.string.permission_action_request))
                    }
                }
                PermissionStatus.Denied -> {
                    Text(stringResource(R.string.permission_status_denied))
                    Button(onClick = onRequestPermission) {
                        Text(stringResource(R.string.permission_action_request))
                    }
                }
                PermissionStatus.PermanentlyDenied -> {
                    Text(stringResource(R.string.permission_status_permanently_denied))
                    Button(onClick = onOpenAppSettings) {
                        Text(stringResource(R.string.permission_action_open_settings))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeContentPreview() {
    ClockMeTheme {
        HomeContent(
            uiState = HomeUiState(calendarPermission = PermissionStatus.Denied),
            onRequestPermission = {},
            onOpenAppSettings = {},
        )
    }
}
