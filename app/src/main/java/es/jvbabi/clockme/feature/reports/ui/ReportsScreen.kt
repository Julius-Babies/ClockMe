package es.jvbabi.clockme.feature.reports.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import es.jvbabi.clockme.R
import es.jvbabi.clockme.ui.theme.ClockMeTheme

/** Empty for now; reports on the tracked time will go here. */
@Composable
fun ReportsScreen() {
    Scaffold { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            Text(
                text = stringResource(R.string.reports_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ReportsScreenPreview() {
    ClockMeTheme(dynamicColor = false) {
        ReportsScreen()
    }
}
