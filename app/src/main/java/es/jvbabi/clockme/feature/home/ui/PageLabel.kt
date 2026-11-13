package es.jvbabi.clockme.feature.home.ui

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import es.jvbabi.clockme.R
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.toJavaLocalDate
import java.time.format.DateTimeFormatter

/** Describes a page: e.g. "Thu, Nov 12" for a day, "Week 46" and "Nov 9 – Nov 15" for a week. */
data class PageLabel(val title: String, val subtitle: String? = null)

/** The year is only shown if the page lies outside the current year. */
@Composable
fun rememberPageLabel(unit: PageUnit, firstDay: LocalDate, today: LocalDate): PageLabel {
    val locale = LocalConfiguration.current.locales[0]
    val weekTitle = stringResource(R.string.home_page_week, firstDay.isoWeekNumber)
    val rangeFormat = stringResource(R.string.home_page_range)
    return remember(unit, firstDay, today, locale, weekTitle, rangeFormat) {
        fun LocalDate.format(skeleton: String) = DateTimeFormatter
            .ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale)
            .format(toJavaLocalDate())

        val lastDay = unit.endExclusive(firstDay).minus(1, DateTimeUnit.DAY)
        val otherYear = firstDay.year != today.year || lastDay.year != today.year
        val year = if (otherYear) "yy" else ""
        when (unit) {
            PageUnit.DAY -> PageLabel(firstDay.format("EEEdMMM$year"))
            PageUnit.WEEK -> PageLabel(
                title = weekTitle,
                subtitle = rangeFormat.format(firstDay.format("dM$year"), lastDay.format("dM$year")),
            )
            PageUnit.MONTH -> PageLabel(
                title = firstDay.format("LLLL"),
                subtitle = if (otherYear) firstDay.format("yyyy") else null,
            )
        }
    }
}
