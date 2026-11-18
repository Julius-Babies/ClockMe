package es.jvbabi.clockme.feature.reports.pdf

import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@RunWith(AndroidJUnit4::class)
class ReportPdfRendererTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val labels = object : ReportLabels {
        override val title = "Circles Report"
        override val total = "Total"
        override val events = "Events"
        override val noEvents = "No events"
        override val noTime = "No time"
        override val untitled = "(Untitled)"
        override fun created(at: String) = "Created: $at"
        override fun page(number: Int, count: Int) = "Page $number of $count"
        override fun day(date: LocalDate) = date.toString()
        override fun time(time: LocalTime) = time.toString()
        override fun duration(duration: Duration) = duration.toString()
    }

    private val calendars = listOf(
        ReportCalendar(1, "Lectures", 0xFF4285F4.toInt(), 14.hours, groupId = 10),
        ReportCalendar(2, "Study", 0xFF0B8043.toInt(), 6.hours, groupId = 10),
        ReportCalendar(3, "Gym", 0xFFF4511E.toInt(), 3.hours),
        ReportCalendar(4, "Reading", 0xFF8E24AA.toInt(), 45.minutes),
    )

    private fun document(events: List<ReportEvent>?) = ReportDocument(
        from = LocalDateTime.parse("2026-11-01T00:00"),
        to = LocalDateTime.parse("2026-11-18T14:37"),
        createdAt = LocalDateTime.parse("2026-11-18T14:37:52"),
        calendars = calendars,
        groups = listOf(ReportCalendarGroup(10, "University", 0xFF3949AB.toInt())),
        events = events,
    )

    private fun events(count: Int) = List(count) { index ->
        val date = LocalDate(2026, 11, 1 + index / 8)
        val start = LocalDateTime(date, LocalTime(8 + index % 8, 0))
        val calendar = calendars[index % calendars.size]
        ReportEvent(date, start, LocalDateTime(date, LocalTime(8 + index % 8, 45)), "Event $index", calendar.color, calendar.name, 45.minutes)
    }

    private fun pageCountOf(document: ReportDocument, name: String): Int {
        val file = File(context.cacheDir, name)
        file.outputStream().use { ReportPdfRenderer(labels).render(document, it) }
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
            PdfRenderer(descriptor).use { return it.pageCount }
        }
    }

    @Test
    fun withoutEventsTheReportIsOnePage() {
        assertEquals(1, pageCountOf(document(events = null), "report-without-events.pdf"))
    }

    @Test
    fun fewEventsFitOnTheFirstPage() {
        assertEquals(1, pageCountOf(document(events(8)), "report-few-events.pdf"))
    }

    @Test
    fun manyEventsRunOverSeveralPages() {
        assertTrue(pageCountOf(document(events(130)), "report-many-events.pdf") > 1)
    }

    @Test
    fun aSpanWithoutTimeStillMakesAReport() {
        val empty = document(emptyList()).copy(calendars = calendars.map { it.copy(duration = Duration.ZERO) })
        assertEquals(1, pageCountOf(empty, "report-empty.pdf"))
    }

    @Test
    fun manyCalendarsKeepTheFirstPage() {
        val colors = listOf(0xFF4285F4, 0xFF0B8043, 0xFFF4511E, 0xFF8E24AA, 0xFFF6BF26, 0xFF039BE5, 0xFF616161, 0xFFE67C73, 0xFF33B679)
        val many = colors.mapIndexed { index, color ->
            ReportCalendar(index.toLong(), "Calendar $index", color.toInt(), (index % 4 + 1).hours, groupId = if (index < 3) 10 else null)
        }
        assertEquals(1, pageCountOf(document(events(8)).copy(calendars = many), "report-many-calendars.pdf"))
    }
}
