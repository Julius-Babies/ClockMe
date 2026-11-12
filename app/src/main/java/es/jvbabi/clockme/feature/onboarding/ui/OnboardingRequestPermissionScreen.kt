package es.jvbabi.clockme.feature.onboarding.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.jvbabi.clockme.R
import es.jvbabi.clockme.core.ui.ObserveAsEvents
import es.jvbabi.clockme.data.permission.PermissionStatus
import es.jvbabi.clockme.ui.theme.ClockMeTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun OnboardingRequestPermissionScreen(
    onNavigateToHome: () -> Unit,
    viewModel: OnboardingRequestPermissionViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            OnboardingRequestPermissionEvent.NavigateToHome -> onNavigateToHome()
            OnboardingRequestPermissionEvent.ShowAppSettingsHint -> Toast.makeText(
                context,
                R.string.onboarding_calendar_permission_settings_hint,
                Toast.LENGTH_LONG,
            ).show()
        }
    }
    OnboardingRequestPermissionContent(
        uiState = uiState,
        onGrantPermissionClick = viewModel::onGrantPermissionClick,
    )
}

@Composable
private fun OnboardingRequestPermissionContent(
    uiState: OnboardingRequestPermissionUiState,
    onGrantPermissionClick: () -> Unit,
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
                text = stringResource(R.string.onboarding_calendar_permission_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(
                    R.string.onboarding_calendar_permission_description,
                    stringResource(R.string.app_name),
                ),
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = onGrantPermissionClick,
                enabled = uiState.calendarPermission != null,
            ) {
                Text(
                    stringResource(
                        if (uiState.calendarPermission == PermissionStatus.PermanentlyDenied) {
                            R.string.permission_action_open_settings
                        } else {
                            R.string.permission_action_request
                        },
                    ),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingRequestPermissionContentPreview() {
    ClockMeTheme {
        OnboardingRequestPermissionContent(
            uiState = OnboardingRequestPermissionUiState(PermissionStatus.NotRequested),
            onGrantPermissionClick = {},
        )
    }
}
