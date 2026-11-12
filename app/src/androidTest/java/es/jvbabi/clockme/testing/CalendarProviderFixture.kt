package es.jvbabi.clockme.testing

import android.os.ParcelFileDescriptor
import androidx.test.platform.app.InstrumentationRegistry
import es.jvbabi.clockme.data.permission.AppPermission
import kotlin.time.Instant

/**
 * Creates calendars and events in the real calendar provider via the shell, acting as sync
 * adapter, so the app itself doesn't need WRITE_CALENDAR. Everything is created under the
 * [TEST_ACCOUNTS], [deleteAll] removes it again (events are deleted with their calendar).
 */
class CalendarProviderFixture {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    fun grantPermission() {
        instrumentation.uiAutomation.grantRuntimePermission(
            context.packageName,
            AppPermission.READ_CALENDAR.manifestName,
        )
    }

    /** Inserts a calendar and returns its id. */
    fun insertCalendar(account: String, name: String, color: Int): Long {
        shell(
            "content", "insert", "--uri", syncAdapterUri(CALENDARS_URI, account),
            "--bind", "account_name:s:$account", "--bind", "account_type:s:$ACCOUNT_TYPE",
            "--bind", "name:s:$name", "--bind", "calendar_displayName:s:$name",
            "--bind", "calendar_color:i:$color", "--bind", "calendar_access_level:i:700",
            "--bind", "ownerAccount:s:$account", "--bind", "visible:i:1", "--bind", "sync_events:i:1",
        )
        val output = shell("content", "query", "--uri", CALENDARS_URI, "--projection", "_id:account_name:name")
        return Regex("_id=(\\d+), account_name=$account, name=$name$", RegexOption.MULTILINE)
            .find(output)
            ?.groupValues?.get(1)?.toLong()
            ?: error("Calendar $name of $account not found after insert:\n$output")
    }

    fun deleteCalendar(account: String, id: Long) {
        shell("content", "delete", "--uri", syncAdapterUri(CALENDARS_URI, account), "--where", "_id=$id")
    }

    /**
     * Inserts an event and returns its id. [title] must be unique within the calendar.
     * A [recurrenceRule] (e.g. `FREQ=DAILY;COUNT=3`) repeats the event, then [end] only
     * defines the duration of each occurrence.
     */
    fun insertEvent(
        account: String,
        calendarId: Long,
        title: String,
        start: Instant,
        end: Instant,
        allDay: Boolean = false,
        canceled: Boolean = false,
        recurrenceRule: String? = null,
    ): Long {
        val timing = if (recurrenceRule == null) {
            arrayOf("--bind", "dtend:l:${end.toEpochMilliseconds()}")
        } else {
            arrayOf(
                "--bind", "rrule:s:$recurrenceRule",
                "--bind", "duration:s:PT${(end - start).inWholeMinutes}M",
            )
        }
        shell(
            "content", "insert", "--uri", syncAdapterUri(EVENTS_URI, account),
            "--bind", "calendar_id:l:$calendarId",
            "--bind", "title:s:$title",
            "--bind", "dtstart:l:${start.toEpochMilliseconds()}",
            *timing,
            "--bind", "eventTimezone:s:${if (allDay) "UTC" else "Europe/Berlin"}",
            "--bind", "allDay:i:${if (allDay) 1 else 0}",
            "--bind", "eventStatus:i:${if (canceled) 2 else 1}",
        )
        val output = shell("content", "query", "--uri", EVENTS_URI, "--projection", "_id:calendar_id:title")
        return Regex("_id=(\\d+), calendar_id=$calendarId, title=$title$", RegexOption.MULTILINE)
            .find(output)
            ?.groupValues?.get(1)?.toLong()
            ?: error("Event $title not found after insert:\n$output")
    }

    fun deleteAll() {
        TEST_ACCOUNTS.forEach { account ->
            shell("content", "delete", "--uri", syncAdapterUri(CALENDARS_URI, account), "--where", "account_name='$account'")
        }
    }

    private fun syncAdapterUri(uri: String, account: String) =
        "$uri?caller_is_syncadapter=true&account_name=$account&account_type=$ACCOUNT_TYPE"

    /**
     * Runs a shell command. There is no real shell involved: the command is split at spaces
     * and no quoting is applied, so no argument may contain a space.
     */
    private fun shell(vararg args: String): String {
        require(args.none { ' ' in it }) { "Shell arguments must not contain spaces: ${args.toList()}" }
        val fd = instrumentation.uiAutomation.executeShellCommand(args.joinToString(" "))
        return ParcelFileDescriptor.AutoCloseInputStream(fd).bufferedReader().use { it.readText() }
    }

    companion object {
        const val ACCOUNT_TYPE = "LOCAL"
        const val ACCOUNT_A = "clockme-test-a"
        const val ACCOUNT_B = "clockme-test-b"
        val TEST_ACCOUNTS = listOf(ACCOUNT_A, ACCOUNT_B)

        private const val CALENDARS_URI = "content://com.android.calendar/calendars"
        private const val EVENTS_URI = "content://com.android.calendar/events"
    }
}
