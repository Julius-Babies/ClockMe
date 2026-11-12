package es.jvbabi.clockme.data.calendar

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import es.jvbabi.clockme.MainActivity
import es.jvbabi.clockme.testing.CalendarProviderFixture
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

    private val fixture = CalendarProviderFixture()
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

    @Test
    fun activatingStoresCalendarWithItsAccount() = runBlocking {
        val calendar = Calendar(id = 1, name = "Work", color = RED)
        val account = CalendarAccount(id = "$ACCOUNT_TYPE/$ACCOUNT_A", name = ACCOUNT_A, calendars = listOf(calendar))

        repository.setCalendarActive(account, calendar, active = true)

        assertEquals(listOf(account), repository.observeActiveCalendars().first())
    }

    @Test
    fun deactivatingLastCalendarRemovesAccount() = runBlocking {
        val work = Calendar(id = 1, name = "Work", color = RED)
        val personal = Calendar(id = 2, name = "Personal", color = BLUE)
        val account = CalendarAccount(id = "$ACCOUNT_TYPE/$ACCOUNT_A", name = ACCOUNT_A, calendars = listOf(personal, work))
        repository.setCalendarActive(account, work, active = true)
        repository.setCalendarActive(account, personal, active = true)

        repository.setCalendarActive(account, work, active = false)
        assertEquals(
            listOf(account.copy(calendars = listOf(personal))),
            repository.observeActiveCalendars().first(),
        )

        repository.setCalendarActive(account, personal, active = false)
        assertEquals(emptyList<CalendarAccount>(), repository.observeActiveCalendars().first())
    }

    @Test
    fun syncRemovesDeletedCalendarsAndEmptyAccounts() = runBlocking {
        grantPermission()
        val workId = insertCalendar(ACCOUNT_A, "Work", RED)
        val personalId = insertCalendar(ACCOUNT_A, "Personal", BLUE)
        val holidaysId = insertCalendar(ACCOUNT_B, "Holidays", GREEN)
        val accounts = testAccounts(withTimeout(TIMEOUT_MS) { repository.observeAccounts().first() })
        accounts.forEach { account ->
            account.calendars.forEach { repository.setCalendarActive(account, it, active = true) }
        }

        deleteCalendar(ACCOUNT_A, personalId)
        deleteCalendar(ACCOUNT_B, holidaysId)
        repository.syncActiveCalendars()

        assertEquals(
            listOf(
                CalendarAccount(
                    id = "$ACCOUNT_TYPE/$ACCOUNT_A",
                    name = ACCOUNT_A,
                    calendars = listOf(Calendar(id = workId, name = "Work", color = RED)),
                ),
            ),
            testAccounts(repository.observeActiveCalendars().first()),
        )
    }

    @Test
    fun syncUpdatesNameAndColor() = runBlocking {
        grantPermission()
        val workId = insertCalendar(ACCOUNT_A, "Work", RED)
        val account = CalendarAccount(id = "$ACCOUNT_TYPE/$ACCOUNT_A", name = ACCOUNT_A, calendars = emptyList())
        // Stored with outdated name and color
        repository.setCalendarActive(account, Calendar(id = workId, name = "Old", color = BLUE), active = true)

        repository.syncActiveCalendars()

        assertEquals(
            listOf(account.copy(calendars = listOf(Calendar(id = workId, name = "Work", color = RED)))),
            testAccounts(repository.observeActiveCalendars().first()),
        )
    }

    @Test
    fun syncDoesNothingWithoutPermission() = runBlocking {
        val calendar = Calendar(id = 123_456, name = "Work", color = RED)
        val account = CalendarAccount(id = "$ACCOUNT_TYPE/$ACCOUNT_A", name = ACCOUNT_A, calendars = listOf(calendar))
        repository.setCalendarActive(account, calendar, active = true)

        repository.syncActiveCalendars()

        assertEquals(listOf(account), repository.observeActiveCalendars().first())
    }

    private fun testAccounts(accounts: List<CalendarAccount>?) =
        accounts.orEmpty().filter { it.name in TEST_ACCOUNTS }

    private suspend fun awaitTestCalendarCount(flow: StateFlow<List<CalendarAccount>?>, expected: Int) {
        fun count() = flow.value?.let { testAccounts(it).sumOf { account -> account.calendars.size } }
        withTimeoutOrNull(TIMEOUT_MS) { flow.first { count() == expected } }
        assertEquals(expected, count())
    }

    private fun grantPermission() = fixture.grantPermission()

    private fun insertCalendar(account: String, name: String, color: Int) = fixture.insertCalendar(account, name, color)

    private fun deleteCalendar(account: String, id: Long) = fixture.deleteCalendar(account, id)

    private fun deleteTestCalendars() = fixture.deleteAll()

    private companion object {
        const val TIMEOUT_MS = 5_000L
        const val ACCOUNT_TYPE = CalendarProviderFixture.ACCOUNT_TYPE
        const val ACCOUNT_A = CalendarProviderFixture.ACCOUNT_A
        const val ACCOUNT_B = CalendarProviderFixture.ACCOUNT_B
        val TEST_ACCOUNTS = CalendarProviderFixture.TEST_ACCOUNTS

        const val RED = 0xFFFF0000.toInt()
        const val GREEN = 0xFF00FF00.toInt()
        const val BLUE = 0xFF0000FF.toInt()
    }
}
