package es.jvbabi.clockme.data.calendar

import android.os.ParcelFileDescriptor
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import es.jvbabi.clockme.MainActivity
import es.jvbabi.clockme.data.permission.AppPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

/**
 * Tests [AndroidCalendarRepository] against the real calendar provider.
 *
 * Test calendars are created via the shell (as a sync adapter), so the app itself
 * doesn't need WRITE_CALENDAR. Only calendars of the test accounts are asserted,
 * the device may have other accounts.
 */
@RunWith(AndroidJUnit4::class)
class AndroidCalendarRepositoryTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    private val repository: CalendarRepository by lazy { GlobalContext.get().get() }

    private lateinit var scenario: ActivityScenario<MainActivity>
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Before
    fun setUp() {
        deleteTestCalendars()
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun tearDown() {
        scope.cancel()
        scenario.close()
        deleteTestCalendars()
    }

    @Test
    fun emitsEmptyListWithoutPermission() = runBlocking {
        insertCalendar(ACCOUNT_A, "Work", RED)

        val accounts = withTimeout(TIMEOUT_MS) { repository.observeAccounts().first() }

        assertEquals(emptyList<CalendarAccount>(), accounts)
    }

    @Test
    fun groupsCalendarsByAccount() = runBlocking {
        grantPermission()
        val workId = insertCalendar(ACCOUNT_A, "Work", RED)
        val personalId = insertCalendar(ACCOUNT_A, "Personal", BLUE)
        val holidaysId = insertCalendar(ACCOUNT_B, "Holidays", GREEN)

        val accounts = testAccounts(withTimeout(TIMEOUT_MS) { repository.observeAccounts().first() })

        assertEquals(
            listOf(
                CalendarAccount(
                    id = "$ACCOUNT_TYPE/$ACCOUNT_A",
                    name = ACCOUNT_A,
                    // sorted by name
                    calendars = listOf(
                        Calendar(id = personalId, name = "Personal", color = BLUE),
                        Calendar(id = workId, name = "Work", color = RED),
                    ),
                ),
                CalendarAccount(
                    id = "$ACCOUNT_TYPE/$ACCOUNT_B",
                    name = ACCOUNT_B,
                    calendars = listOf(Calendar(id = holidaysId, name = "Holidays", color = GREEN)),
                ),
            ),
            accounts,
        )
    }

    @Test
    fun emitsUpdateWhenCalendarIsAdded() = runBlocking {
        grantPermission()
        insertCalendar(ACCOUNT_A, "Work", RED)
        val accounts = repository.observeAccounts().stateIn(scope, SharingStarted.Eagerly, null)
        awaitTestCalendarCount(accounts, 1)

        insertCalendar(ACCOUNT_A, "Personal", BLUE)

        awaitTestCalendarCount(accounts, 2)
    }

    @Test
    fun emitsCalendarsOncePermissionIsGranted() = runBlocking {
        insertCalendar(ACCOUNT_A, "Work", RED)
        val accounts = repository.observeAccounts().stateIn(scope, SharingStarted.Eagerly, null)
        awaitTestCalendarCount(accounts, 0)

        // Simulates granting the permission in the app settings and returning to the app
        grantPermission()
        scenario.moveToState(Lifecycle.State.STARTED)
        scenario.moveToState(Lifecycle.State.RESUMED)

        awaitTestCalendarCount(accounts, 1)
    }

    private fun testAccounts(accounts: List<CalendarAccount>?) =
        accounts.orEmpty().filter { it.name in TEST_ACCOUNTS }

    private suspend fun awaitTestCalendarCount(flow: StateFlow<List<CalendarAccount>?>, expected: Int) {
        fun count() = flow.value?.let { testAccounts(it).sumOf { account -> account.calendars.size } }
        withTimeoutOrNull(TIMEOUT_MS) { flow.first { count() == expected } }
        assertEquals(expected, count())
    }

    private fun grantPermission() {
        instrumentation.uiAutomation.grantRuntimePermission(
            context.packageName,
            AppPermission.READ_CALENDAR.manifestName,
        )
    }

    /** Inserts a calendar as sync adapter via the shell and returns its id. */
    private fun insertCalendar(account: String, name: String, color: Int): Long {
        shell(
            "content", "insert", "--uri", syncAdapterUri(account),
            "--bind", "account_name:s:$account", "--bind", "account_type:s:$ACCOUNT_TYPE",
            "--bind", "name:s:$name", "--bind", "calendar_displayName:s:$name",
            "--bind", "calendar_color:i:$color", "--bind", "calendar_access_level:i:700",
            "--bind", "ownerAccount:s:$account", "--bind", "visible:i:1", "--bind", "sync_events:i:1",
        )
        val output = shell(
            "content", "query", "--uri", CALENDARS_URI,
            "--projection", "_id:account_name:name",
        )
        return Regex("_id=(\\d+), account_name=$account, name=$name$", RegexOption.MULTILINE)
            .find(output)
            ?.groupValues?.get(1)?.toLong()
            ?: error("Calendar $name of $account not found after insert:\n$output")
    }

    private fun deleteTestCalendars() {
        TEST_ACCOUNTS.forEach { account ->
            shell("content", "delete", "--uri", syncAdapterUri(account), "--where", "account_name='$account'")
        }
    }

    private fun syncAdapterUri(account: String) =
        "$CALENDARS_URI?caller_is_syncadapter=true&account_name=$account&account_type=$ACCOUNT_TYPE"

    /**
     * Runs a shell command. There is no real shell involved: the command is split at spaces
     * and no quoting is applied, so no argument may contain a space.
     */
    private fun shell(vararg args: String): String {
        require(args.none { ' ' in it }) { "Shell arguments must not contain spaces: ${args.toList()}" }
        val fd = instrumentation.uiAutomation.executeShellCommand(args.joinToString(" "))
        return ParcelFileDescriptor.AutoCloseInputStream(fd).bufferedReader().use { it.readText() }
    }

    private companion object {
        const val TIMEOUT_MS = 5_000L
        const val CALENDARS_URI = "content://com.android.calendar/calendars"
        const val ACCOUNT_TYPE = "LOCAL"
        const val ACCOUNT_A = "clockme-test-a"
        const val ACCOUNT_B = "clockme-test-b"
        val TEST_ACCOUNTS = listOf(ACCOUNT_A, ACCOUNT_B)

        const val RED = 0xFFFF0000.toInt()
        const val GREEN = 0xFF00FF00.toInt()
        const val BLUE = 0xFF0000FF.toInt()
    }
}
