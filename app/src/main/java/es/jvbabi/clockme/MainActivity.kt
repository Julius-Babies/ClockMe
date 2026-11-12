package es.jvbabi.clockme

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import es.jvbabi.clockme.navigation.AppNavigation
import es.jvbabi.clockme.ui.theme.ClockMeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ClockMeTheme {
                AppNavigation()
            }
        }
    }
}
