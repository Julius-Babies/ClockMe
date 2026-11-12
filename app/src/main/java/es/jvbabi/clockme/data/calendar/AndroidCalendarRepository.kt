package es.jvbabi.clockme.data.calendar

import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract.Calendars
import androidx.core.content.ContextCompat
import es.jvbabi.clockme.data.calendar.local.ActiveCalendarDao
import es.jvbabi.clockme.data.calendar.local.ActiveCalendarEntity
import es.jvbabi.clockme.data.calendar.local.CalendarAccountEntity
import es.jvbabi.clockme.data.permission.AppPermission
import es.jvbabi.clockme.data.permission.PermissionRepository
import es.jvbabi.clockme.data.permission.PermissionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class AndroidCalendarRepository(
    private val context: Context,
    private val permissionRepository: PermissionRepository,
    private val activeCalendarDao: ActiveCalendarDao,
) : CalendarRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeAccounts(): Flow<List<CalendarAccount>> =
        permissionRepository.observe(AppPermission.READ_CALENDAR)
            .map { it == PermissionStatus.Granted }
            .distinctUntilChanged()
            .flatMapLatest { granted ->
                if (granted) {
                    observeChanges(Calendars.CONTENT_URI)
                        .map { queryAccounts().orEmpty() }
                        .flowOn(Dispatchers.IO)
                } else {
                    flowOf(emptyList())
                }
            }
            .distinctUntilChanged()

    override fun observeActiveCalendars(): Flow<List<CalendarAccount>> =
        activeCalendarDao.observeAccountsWithCalendars().map { accounts ->
            accounts.map { (account, calendars) ->
                CalendarAccount(
                    id = account.id,
                    name = account.name,
                    calendars = calendars
                        .sortedBy { it.name }
                        .map { Calendar(id = it.id, name = it.name, color = it.color) },
                )
            }
        }

    override suspend fun setCalendarActive(
        account: CalendarAccount,
        calendar: Calendar,
        active: Boolean,
    ) {
        if (active) {
            activeCalendarDao.activate(account.toEntity(), calendar.toEntity(account))
        } else {
            activeCalendarDao.deactivate(calendar.id)
        }
    }

    override suspend fun syncActiveCalendars() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            AppPermission.READ_CALENDAR.manifestName,
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) return

        // null if the provider couldn't be queried, don't delete anything in that case
        val accounts = withContext(Dispatchers.IO) { queryAccounts() } ?: return
        val existing = accounts.flatMap { account ->
            account.calendars.map { calendar ->
                calendar.id to (account.toEntity() to calendar.toEntity(account))
            }
        }.toMap()
        activeCalendarDao.sync(existing)
    }

    /** Emits once immediately and again on every change of [uri]. */
    private fun observeChanges(uri: Uri): Flow<Unit> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
        }
        context.contentResolver.registerContentObserver(uri, true, observer)
        trySend(Unit)
        awaitClose { context.contentResolver.unregisterContentObserver(observer) }
    }.buffer(Channel.CONFLATED)

    /** Returns `null` if the calendar provider couldn't be queried. */
    private fun queryAccounts(): List<CalendarAccount>? {
        val cursor = context.contentResolver.query(
            Calendars.CONTENT_URI,
            PROJECTION,
            null,
            null,
            "${Calendars.ACCOUNT_NAME} ASC, ${Calendars.CALENDAR_DISPLAY_NAME} ASC",
        ) ?: return null

        data class Row(val accountType: String, val accountName: String, val calendar: Calendar)

        val rows = cursor.use {
            buildList {
                while (it.moveToNext()) {
                    add(
                        Row(
                            accountType = it.getString(INDEX_ACCOUNT_TYPE).orEmpty(),
                            accountName = it.getString(INDEX_ACCOUNT_NAME).orEmpty(),
                            calendar = Calendar(
                                id = it.getLong(INDEX_ID),
                                name = it.getString(INDEX_DISPLAY_NAME).orEmpty(),
                                color = it.getInt(INDEX_COLOR),
                            ),
                        ),
                    )
                }
            }
        }

        return rows
            .groupBy { it.accountType to it.accountName }
            .map { (account, accountRows) ->
                val (type, name) = account
                CalendarAccount(
                    id = "$type/$name",
                    name = name,
                    calendars = accountRows.map { it.calendar },
                )
            }
    }

    private fun CalendarAccount.toEntity() = CalendarAccountEntity(id = id, name = name)

    private fun Calendar.toEntity(account: CalendarAccount) = ActiveCalendarEntity(
        id = id,
        accountId = account.id,
        name = name,
        color = color,
    )

    private companion object {
        val PROJECTION = arrayOf(
            Calendars._ID,
            Calendars.CALENDAR_DISPLAY_NAME,
            Calendars.CALENDAR_COLOR,
            Calendars.ACCOUNT_NAME,
            Calendars.ACCOUNT_TYPE,
        )
        const val INDEX_ID = 0
        const val INDEX_DISPLAY_NAME = 1
        const val INDEX_COLOR = 2
        const val INDEX_ACCOUNT_NAME = 3
        const val INDEX_ACCOUNT_TYPE = 4
    }
}
