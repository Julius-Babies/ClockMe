package es.jvbabi.clockme.data.calendar

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract.Calendars
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

class AndroidCalendarRepository(
    private val context: Context,
    private val permissionRepository: PermissionRepository,
) : CalendarRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeAccounts(): Flow<List<CalendarAccount>> =
        permissionRepository.observe(AppPermission.READ_CALENDAR)
            .map { it == PermissionStatus.Granted }
            .distinctUntilChanged()
            .flatMapLatest { granted ->
                if (granted) {
                    observeChanges(Calendars.CONTENT_URI)
                        .map { queryAccounts() }
                        .flowOn(Dispatchers.IO)
                } else {
                    flowOf(emptyList())
                }
            }
            .distinctUntilChanged()

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

    private fun queryAccounts(): List<CalendarAccount> {
        val cursor = context.contentResolver.query(
            Calendars.CONTENT_URI,
            PROJECTION,
            null,
            null,
            "${Calendars.ACCOUNT_NAME} ASC, ${Calendars.CALENDAR_DISPLAY_NAME} ASC",
        ) ?: return emptyList()

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
