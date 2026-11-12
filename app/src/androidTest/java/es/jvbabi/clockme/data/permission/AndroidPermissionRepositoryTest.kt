package es.jvbabi.clockme.data.permission

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import es.jvbabi.clockme.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
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
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import java.util.regex.Pattern

/**
 * Tests [AndroidPermissionRepository] against the real permission system.
 *
 * Relies on the Test Orchestrator with `clearPackageData`, so every test starts
 * with the permission revoked and no request history.
 */
@RunWith(AndroidJUnit4::class)
class AndroidPermissionRepositoryTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val device = UiDevice.getInstance(instrumentation)
    private val permission = AppPermission.READ_CALENDAR

    // The real instance from the app's Koin graph (started by ClockMeApp)
    private val repository: PermissionRepository by lazy { GlobalContext.get().get() }

    private lateinit var scenario: ActivityScenario<MainActivity>
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Before
    fun setUp() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun tearDown() {
        scope.cancel()
        scenario.close()
    }

    @Test
    fun initialStatusIsNotRequested() = runBlocking {
        assertEquals(PermissionStatus.NotRequested, currentStatus())
    }

    @Test
    fun grantedPermissionIsReportedAsGranted() = runBlocking {
        grantViaShell()
        assertEquals(PermissionStatus.Granted, currentStatus())
    }

    @Test
    fun observeEmitsUpdateWhenActivityIsResumed() = runBlocking {
        val status = repository.observe(permission).stateIn(scope, SharingStarted.Eagerly, null)
        awaitStatus(status, PermissionStatus.NotRequested)

        // Simulates granting the permission in the app settings and returning to the app
        grantViaShell()
        scenario.moveToState(Lifecycle.State.STARTED)
        scenario.moveToState(Lifecycle.State.RESUMED)

        awaitStatus(status, PermissionStatus.Granted)
    }

    @Test
    fun allowingInDialogReturnsGranted() = runBlocking {
        val result = scope.async { repository.requestViaDialog(permission) }
        clickDialogButton(ALLOW_BUTTON)

        assertEquals(PermissionStatus.Granted, withTimeout(TIMEOUT_MS) { result.await() })
        assertEquals(PermissionStatus.Granted, currentStatus())
    }

    @Test
    fun denyingOnceReturnsDenied() = runBlocking {
        val result = scope.async { repository.requestViaDialog(permission) }
        clickDialogButton(DENY_BUTTON)

        assertEquals(PermissionStatus.Denied, withTimeout(TIMEOUT_MS) { result.await() })
        assertEquals(PermissionStatus.Denied, currentStatus())
    }

    @Test
    fun denyingTwiceReturnsPermanentlyDenied() = runBlocking {
        repeat(2) {
            val result = scope.async { repository.requestViaDialog(permission) }
            clickDialogButton(DENY_BUTTON)
            withTimeout(TIMEOUT_MS) { result.await() }
        }

        assertEquals(PermissionStatus.PermanentlyDenied, currentStatus())
    }

    @Test
    fun observedFlowFollowsDialogResult() = runBlocking {
        val status = repository.observe(permission).stateIn(scope, SharingStarted.Eagerly, null)
        awaitStatus(status, PermissionStatus.NotRequested)

        val result = scope.async { repository.requestViaDialog(permission) }
        clickDialogButton(ALLOW_BUTTON)
        withTimeout(TIMEOUT_MS) { result.await() }

        awaitStatus(status, PermissionStatus.Granted)
    }

    private suspend fun currentStatus() =
        withTimeout(TIMEOUT_MS) { repository.observe(permission).first() }

    /** Waits until [flow] emits [expected]; on timeout fails with the last emitted value. */
    private suspend fun awaitStatus(flow: StateFlow<PermissionStatus?>, expected: PermissionStatus) {
        withTimeoutOrNull(TIMEOUT_MS) { flow.first { it == expected } }
        assertEquals(expected, flow.value)
    }

    private fun grantViaShell() {
        instrumentation.uiAutomation.grantRuntimePermission(
            context.packageName,
            permission.manifestName,
        )
    }

    private fun clickDialogButton(resourceIdPattern: Pattern) {
        val button = device.wait(Until.findObject(By.res(resourceIdPattern)), TIMEOUT_MS)
        assertNotNull("Permission dialog button $resourceIdPattern not found", button)
        button.click()
        device.wait(Until.gone(By.res(resourceIdPattern)), TIMEOUT_MS)
    }

    private companion object {
        const val TIMEOUT_MS = 5_000L

        // Package differs between AOSP and Google builds of the permission controller
        val ALLOW_BUTTON: Pattern = Pattern.compile(".*:id/permission_allow_button")
        val DENY_BUTTON: Pattern =
            Pattern.compile(".*:id/permission_deny(_and_dont_ask_again)?_button")
    }
}
