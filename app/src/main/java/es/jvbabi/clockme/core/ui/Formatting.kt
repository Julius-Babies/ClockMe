package es.jvbabi.clockme.core.ui

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import es.jvbabi.clockme.R
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlin.time.Duration

/** Compact: "2h05", "2h" for full hours, or "45min" below an hour. */
@Composable
fun formatDuration(duration: Duration): String {
    val totalMinutes = duration.inWholeMinutes
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 && minutes == 0L -> stringResource(R.string.duration_hours, hours)
        hours > 0 -> stringResource(R.string.duration_hours_minutes, hours, minutes)
        else -> stringResource(R.string.duration_minutes, minutes)
    }
}

/** Formats a time of day in the 12 or 24 hour format the user picked in the system settings. */
@Composable
fun rememberTimeFormatter(): (LocalTime) -> String {
    val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    return remember(is24Hour) {
        val format = LocalTime.Format {
            if (is24Hour) {
                hour()
                char(':')
                minute()
            } else {
                amPmHour(Padding.NONE)
                char(':')
                minute()
                char(' ')
                amPmMarker("AM", "PM")
            }
        }
        val formatter: (LocalTime) -> String = { time -> time.format(format) }
        formatter
    }
}
