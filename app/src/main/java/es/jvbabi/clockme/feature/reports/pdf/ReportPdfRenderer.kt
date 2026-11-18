package es.jvbabi.clockme.feature.reports.pdf

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.TextPaint
import android.text.TextUtils
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import java.io.OutputStream
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.time.Duration

/** The texts of a report, in the user's language and formats. */
interface ReportLabels {
    val title: String
    val total: String
    val events: String
    val noEvents: String
    val noTime: String
    val untitled: String
    fun created(at: String): String
    fun page(number: Int, count: Int): String
    fun day(date: LocalDate): String
    fun time(time: LocalTime): String
    fun duration(duration: Duration): String
}

/**
 * Draws a [ReportDocument] as an A4 PDF: a header with the span, the ring with a legend whose
 * lines reach it horizontally, and, if the report has them, the events day by day over as many
 * pages as they need.
 */
class ReportPdfRenderer(private val labels: ReportLabels) {

    fun render(document: ReportDocument, out: OutputStream) {
        val pages = layOut(document)
        val pdf = PdfDocument()
        try {
            pages.forEachIndexed { index, operations ->
                val page = pdf.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, index + 1).create())
                val canvas = page.canvas
                operations.forEach { it(canvas) }
                drawFooter(canvas, index + 1, pages.size)
                pdf.finishPage(page)
            }
            pdf.writeTo(out)
        } finally {
            pdf.close()
        }
    }

    /** What to draw on each page; laid out first so the footer knows how many pages there are. */
    private fun layOut(document: ReportDocument): List<List<(Canvas) -> Unit>> {
        val pages = mutableListOf(mutableListOf<(Canvas) -> Unit>())
        var y = MARGIN.toFloat()

        // Header
        y += 22f
        val headerTop = y
        pages.last() += { it.drawText(labels.title, MARGIN.toFloat(), headerTop, titlePaint) }
        y += 22f
        val rangeY = y
        val fromText = formatTimestamp(document.from)
        val toText = formatTimestamp(document.to)
        pages.last() += { canvas ->
            val fromWidth = bodyPaint.measureText(fromText)
            canvas.drawText(fromText, MARGIN.toFloat(), rangeY, bodyPaint)
            val arrowX = MARGIN + fromWidth + ARROW_SPACING
            drawArrow(canvas, arrowX, rangeY - 3.8f)
            canvas.drawText(toText, arrowX + ARROW_LENGTH + ARROW_SPACING, rangeY, bodyPaint)
        }
        y += 17f
        val createdY = y
        val created = labels.created(formatTimestamp(document.createdAt))
        pages.last() += { it.drawText(created, MARGIN.toFloat(), createdY, secondaryPaint) }

        // Ring and legend
        y += 32f
        val ringBlockBottom = layOutRing(document, top = y, operations = pages.last())
        y = ringBlockBottom

        // Events
        val events = document.events ?: return pages
        y += 40f
        val sectionY = y
        pages.last() += { canvas ->
            canvas.drawText(labels.events, MARGIN.toFloat(), sectionY, sectionPaint)
            canvas.drawLine(MARGIN.toFloat(), sectionY + 7f, (PAGE_WIDTH - MARGIN).toFloat(), sectionY + 7f, rulePaint)
        }
        y += 14f
        if (events.isEmpty()) {
            y += 18f
            val emptyY = y
            pages.last() += { it.drawText(labels.noEvents, MARGIN.toFloat(), emptyY, secondaryPaint) }
            return pages
        }

        val bottom = (PAGE_HEIGHT - MARGIN - FOOTER_SPACE).toFloat()
        fun newPage() {
            pages += mutableListOf<(Canvas) -> Unit>()
            y = MARGIN.toFloat()
        }
        fun dayHeader(date: LocalDate) {
            y += DAY_HEADER_HEIGHT
            val headerY = y
            val text = labels.day(date)
            pages.last() += { it.drawText(text, MARGIN.toFloat(), headerY, dayPaint) }
            y += 4f
        }

        events.groupBy { it.date }.forEach { (date, dayEvents) ->
            // A day's header never stands alone at the bottom of a page
            if (y + DAY_HEADER_HEIGHT + 4f + ROW_HEIGHT > bottom) newPage()
            dayHeader(date)
            dayEvents.forEach { event ->
                if (y + ROW_HEIGHT > bottom) {
                    newPage()
                    dayHeader(date)
                }
                y += ROW_HEIGHT
                val rowY = y
                pages.last() += { drawEventRow(it, event, rowY) }
            }
        }
        return pages
    }

    /**
     * Lays out the ring at the left and the legend beside it; returns where the block ends.
     *
     * Each calendar's line leaves the middle of its slice straight up (upper half) or down (lower
     * half) and then bends once, running level to its entry. Lines from the left half rise above
     * or sink below the ring before they bend, so they pass it; and the further left a line starts,
     * the further out its entry lies, so no two lines cross.
     */
    private fun layOutRing(document: ReportDocument, top: Float, operations: MutableList<(Canvas) -> Unit>): Float {
        val cx = MARGIN + BAND_OUTSET + RING_RADIUS
        val total = document.total
        val calendars = document.calendars.filter { it.duration > Duration.ZERO }
        val ringExtent = RING_RADIUS + BAND_OUTSET

        // Laid out around a ring centered at 0 first, then moved so the whole block starts at top
        val sweeps = sweepsOf(calendars, total)
        val anchors = sweeps.map { (start, sweep) ->
            val angle = Math.toRadians((start + sweep / 2).toDouble())
            RING_RADIUS * cos(angle).toFloat() to RING_RADIUS * sin(angle).toFloat()
        }
        val labelYs = FloatArray(calendars.size)
        val clearance = ringExtent + PASS_CLEARANCE
        val (upper, lower) = calendars.indices.partition { anchors[it].second < 0 }

        // Upper half: the rightmost line is lowest; each further left one lies a row above it
        var limit = Float.MAX_VALUE
        upper.sortedByDescending { anchors[it].first }.forEach { index ->
            val (x, y) = anchors[index]
            val cap = if (x < 0) -clearance else min(y, y - GRAZE_CLEARANCE * -y / RING_RADIUS)
            labelYs[index] = min(cap, limit)
            limit = labelYs[index] - LEGEND_ROW
        }
        // Lower half, mirrored, and below every entry of the upper half
        var floor = upper.maxOfOrNull { labelYs[it] + LEGEND_ROW } ?: -Float.MAX_VALUE
        lower.sortedByDescending { anchors[it].first }.forEach { index ->
            val (x, y) = anchors[index]
            val base = if (x < 0) clearance else max(y, y + GRAZE_CLEARANCE * y / RING_RADIUS)
            labelYs[index] = max(base, floor)
            floor = labelYs[index] + LEGEND_ROW
        }

        val blockTop = min(-ringExtent, (labelYs.minOrNull() ?: 0f) - LEGEND_ROW / 2)
        val cy = top - blockTop
        val ringBottom = cy + ringExtent

        operations += { canvas -> drawRing(canvas, cx, cy, calendars, document.groups, total) }
        if (calendars.isEmpty()) {
            operations += { it.drawText(labels.noTime, LEGEND_X, cy + 4f, secondaryPaint) }
            return ringBottom
        }
        calendars.forEachIndexed { index, calendar ->
            val edgeX = cx + anchors[index].first
            val edgeY = cy + anchors[index].second
            val labelY = cy + labelYs[index]
            operations += { canvas ->
                val path = Path().apply {
                    moveTo(edgeX, edgeY)
                    lineTo(edgeX, labelY)
                    lineTo(LEGEND_X - 6f, labelY)
                }
                canvas.drawPath(path, leaderPaint)
                canvas.drawCircle(edgeX, edgeY, 2.6f, haloPaint)
                canvas.drawCircle(edgeX, edgeY, 1.6f, leaderDotPaint)
                drawLegendEntry(canvas, calendar, total, labelY)
            }
        }
        return max(ringBottom, cy + labelYs.max() + LEGEND_ROW / 2)
    }

    private fun drawRing(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        calendars: List<ReportCalendar>,
        groups: List<ReportCalendarGroup>,
        total: Duration,
    ) {
        val ringRadius = RING_RADIUS - RING_STROKE / 2
        val ringRect = RectF(cx - ringRadius, cy - ringRadius, cx + ringRadius, cy + ringRadius)
        if (calendars.isEmpty()) {
            canvas.drawArc(ringRect, 0f, 360f, false, ringPaint(TRACK_COLOR))
        } else {
            val sweeps = sweepsOf(calendars, total)
            val gap = if (calendars.size > 1) GAP_DEGREES else 0f
            calendars.forEachIndexed { index, calendar ->
                val (start, sweep) = sweeps[index]
                canvas.drawArc(ringRect, start + gap / 2, max(sweep - gap, 0.3f), false, ringPaint(calendar.color))
            }
            // A band just outside the ring around each group's slices
            val bandRadius = RING_RADIUS + BAND_GAP + BAND_WIDTH / 2
            val bandRect = RectF(cx - bandRadius, cy - bandRadius, cx + bandRadius, cy + bandRadius)
            groups.forEach { group ->
                val indices = calendars.indices.filter { calendars[it].groupId == group.id }
                if (indices.isEmpty()) return@forEach
                val start = sweeps[indices.first()].first
                val end = sweeps[indices.last()].let { it.first + it.second }
                val paint = ringPaint(group.color ?: GROUP_FALLBACK_COLOR, BAND_WIDTH)
                canvas.drawArc(bandRect, start + gap / 2, max(end - start - gap, 0.3f), false, paint)
            }
        }
        canvas.drawText(labels.duration(total), cx, cy + 3f, ringTotalPaint)
        canvas.drawText(labels.total.uppercase(), cx, cy + 16f, ringCaptionPaint)
    }

    /** A right arrow like the app's icons: a shaft and an open head with round ends. */
    private fun drawArrow(canvas: Canvas, x: Float, centerY: Float) {
        val path = Path().apply {
            moveTo(x, centerY)
            lineTo(x + ARROW_LENGTH, centerY)
            moveTo(x + ARROW_LENGTH - ARROW_HEAD, centerY - ARROW_HEAD)
            lineTo(x + ARROW_LENGTH, centerY)
            lineTo(x + ARROW_LENGTH - ARROW_HEAD, centerY + ARROW_HEAD)
        }
        canvas.drawPath(path, arrowPaint)
    }

    private fun drawLegendEntry(canvas: Canvas, calendar: ReportCalendar, total: Duration, y: Float) {
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = calendar.color }
        canvas.drawCircle(LEGEND_X + 4f, y, 4f, dotPaint)
        val percent = if (total > Duration.ZERO) (calendar.duration / total * 100).roundToInt() else 0
        val value = "${labels.duration(calendar.duration)}  ·  $percent %"
        val right = (PAGE_WIDTH - MARGIN).toFloat()
        canvas.drawText(value, right, y + 3.5f, legendValuePaint)
        val nameWidth = right - legendValuePaint.measureText(value) - 12f - (LEGEND_X + 14f)
        val name = TextUtils.ellipsize(calendar.name, legendNamePaint, nameWidth, TextUtils.TruncateAt.END)
        canvas.drawText(name.toString(), LEGEND_X + 14f, y + 3.5f, legendNamePaint)
    }

    private fun drawEventRow(canvas: Canvas, event: ReportEvent, y: Float) {
        val x = MARGIN.toFloat()
        val right = (PAGE_WIDTH - MARGIN).toFloat()
        val time = "${labels.time(event.start.time)} – ${labels.time(event.end.time)}"
        canvas.drawText(time, x, y, rowTimePaint)
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = event.color }
        canvas.drawCircle(x + EVENT_DOT_X, y - 3.2f, 3.2f, dotPaint)

        val duration = labels.duration(event.duration)
        canvas.drawText(duration, right, y, rowDurationPaint)
        val calendarX = right - EVENT_DURATION_WIDTH - EVENT_CALENDAR_WIDTH
        val calendarName = TextUtils.ellipsize(event.calendarName, rowSecondaryPaint, EVENT_CALENDAR_WIDTH - 8f, TextUtils.TruncateAt.END)
        canvas.drawText(calendarName.toString(), calendarX, y, rowSecondaryPaint)
        val titleX = x + EVENT_TITLE_X
        val title = TextUtils.ellipsize(event.title.ifBlank { labels.untitled }, rowTitlePaint, calendarX - 10f - titleX, TextUtils.TruncateAt.END)
        canvas.drawText(title.toString(), titleX, y, rowTitlePaint)
    }

    private fun drawFooter(canvas: Canvas, number: Int, count: Int) {
        val text = "${labels.title}  ·  ${labels.page(number, count)}"
        canvas.drawText(text, PAGE_WIDTH / 2f, (PAGE_HEIGHT - MARGIN / 2).toFloat(), footerPaint)
    }

    private companion object {
        // A4 in points
        const val PAGE_WIDTH = 595
        const val PAGE_HEIGHT = 842
        const val MARGIN = 48
        const val FOOTER_SPACE = 12

        const val RING_RADIUS = 84f
        const val RING_STROKE = 24f
        const val GAP_DEGREES = 1.6f
        const val BAND_GAP = 3f
        const val BAND_WIDTH = 3f
        const val BAND_OUTSET = BAND_GAP + BAND_WIDTH

        /** How far above or below the ring a line from its left half passes it. */
        const val PASS_CLEARANCE = 8f

        /**
         * How far a line from the very top or bottom of the ring first moves away before it runs
         * level, so it does not graze the neighboring slice; nothing for a line from the middle.
         */
        const val GRAZE_CLEARANCE = 7f
        const val LEGEND_X = MARGIN + 2 * (RING_RADIUS + BAND_OUTSET) + 64f
        const val LEGEND_ROW = 22f

        const val ARROW_LENGTH = 14f
        const val ARROW_HEAD = 4.5f
        const val ARROW_SPACING = 10f

        const val DAY_HEADER_HEIGHT = 22f
        const val ROW_HEIGHT = 16f
        const val EVENT_DOT_X = 86f
        const val EVENT_TITLE_X = 96f
        const val EVENT_CALENDAR_WIDTH = 110f
        const val EVENT_DURATION_WIDTH = 52f

        val TEXT_COLOR = Color.rgb(0x1E, 0x1E, 0x1E)
        val SECONDARY_COLOR = Color.rgb(0x6B, 0x6B, 0x6B)
        val TRACK_COLOR = Color.rgb(0xE6, 0xE6, 0xE6)
        val GROUP_FALLBACK_COLOR = Color.rgb(0xB0, 0xB0, 0xB0)

        fun textPaint(size: Float, color: Int = TEXT_COLOR, bold: Boolean = false, align: Paint.Align = Paint.Align.LEFT) =
            TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = size
                this.color = color
                typeface = if (bold) Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) else Typeface.SANS_SERIF
                textAlign = align
            }

        val titlePaint = textPaint(22f, bold = true)
        val bodyPaint = textPaint(11f)
        val secondaryPaint = textPaint(10f, SECONDARY_COLOR)
        val sectionPaint = textPaint(13f, bold = true)
        val dayPaint = textPaint(10f, bold = true)
        val ringTotalPaint = textPaint(15f, bold = true, align = Paint.Align.CENTER)
        val ringCaptionPaint = textPaint(7f, SECONDARY_COLOR, align = Paint.Align.CENTER)
        val legendNamePaint = textPaint(10f)
        val legendValuePaint = textPaint(10f, SECONDARY_COLOR, align = Paint.Align.RIGHT)
        val rowTimePaint = textPaint(9.5f)
        val rowTitlePaint = textPaint(9.5f)
        val rowSecondaryPaint = textPaint(9f, SECONDARY_COLOR)
        val rowDurationPaint = textPaint(9.5f, align = Paint.Align.RIGHT)
        val footerPaint = textPaint(8f, SECONDARY_COLOR, align = Paint.Align.CENTER)

        val rulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = TRACK_COLOR; strokeWidth = 1f }
        val leaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = SECONDARY_COLOR
            style = Paint.Style.STROKE
            strokeWidth = 0.75f
            strokeJoin = Paint.Join.ROUND
        }
        val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = SECONDARY_COLOR
            style = Paint.Style.STROKE
            strokeWidth = 1.4f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val leaderDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = TEXT_COLOR }
        val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }

        fun ringPaint(color: Int, width: Float = RING_STROKE) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = width
        }

        /** Start and sweep of each slice in degrees, clockwise from the top. */
        fun sweepsOf(calendars: List<ReportCalendar>, total: Duration): List<Pair<Float, Float>> {
            var start = -90f
            return calendars.map { calendar ->
                val sweep = (calendar.duration / total).toFloat() * 360f
                (start to sweep).also { start += sweep }
            }
        }

        /** Like "2026-11-01 00:00:00". */
        fun formatTimestamp(value: LocalDateTime): String {
            fun Int.two() = toString().padStart(2, '0')
            return "${value.date} ${value.hour.two()}:${value.minute.two()}:${value.second.two()}"
        }
    }
}
