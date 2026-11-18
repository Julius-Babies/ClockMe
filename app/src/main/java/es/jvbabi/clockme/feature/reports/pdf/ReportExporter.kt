package es.jvbabi.clockme.feature.reports.pdf

import android.content.Context
import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.core.content.FileProvider
import es.jvbabi.clockme.R
import es.jvbabi.clockme.core.ui.formatDuration
import es.jvbabi.clockme.core.ui.timeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import java.io.File
import kotlin.time.Duration

/** Turns a report into a file another app can open. */
fun interface ReportExporter {
    /** Returns the content URI of the written report. */
    suspend fun export(document: ReportDocument): String
}

/**
 * Writes the report as a PDF into the cache, where only the latest one is kept, and shares it
 * through a [FileProvider].
 */
class PdfReportExporter(private val context: Context) : ReportExporter {

    override suspend fun export(document: ReportDocument): String = withContext(Dispatchers.IO) {
        val directory = File(context.cacheDir, REPORTS_DIRECTORY).apply { mkdirs() }
        directory.listFiles()?.forEach { it.delete() }
        val file = File(directory, "Circles-Report_${document.from.date}_${document.to.date}.pdf")
        file.outputStream().use { ReportPdfRenderer(AndroidReportLabels(context)).render(document, it) }
        FileProvider.getUriForFile(context, "${context.packageName}.reports", file).toString()
    }

    private companion object {
        /** Must match res/xml/report_paths.xml. */
        const val REPORTS_DIRECTORY = "reports"
    }
}

private class AndroidReportLabels(private val context: Context) : ReportLabels {
    private val resources = context.resources
    private val formatTime = timeFormatter(DateFormat.is24HourFormat(context))

    override val title = resources.getString(R.string.reports_pdf_title)
    override val total = resources.getString(R.string.home_total)
    override val events = resources.getString(R.string.home_events)
    override val noEvents = resources.getString(R.string.reports_pdf_no_events)
    override val noTime = resources.getString(R.string.reports_pdf_no_time)
    override val untitled = resources.getString(R.string.home_event_untitled)

    override fun created(at: String) = resources.getString(R.string.reports_pdf_created, at)
    override fun page(number: Int, count: Int) = resources.getString(R.string.reports_pdf_page, number, count)
    override fun time(time: LocalTime) = formatTime(time)
    override fun duration(duration: Duration) = resources.formatDuration(duration)

    override fun day(date: LocalDate): String {
        val millis = date.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        return DateUtils.formatDateTime(
            context,
            millis,
            DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR,
        )
    }
}
