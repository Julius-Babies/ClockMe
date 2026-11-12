package es.jvbabi.clockme.data.calendar

import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import android.provider.CalendarContract.Attendees
import android.provider.CalendarContract.Calendars
import android.provider.CalendarContract.Events
import android.provider.CalendarContract.Instances
import androidx.core.content.ContextCompat
import es.jvbabi.clockme.data.calendar.local.ActiveCalendarDao
import es.jvbabi.clockme.data.calendar.local.ActiveCalendarEntity
import es.jvbabi.clockme.data.calendar.local.CalendarAccountEntity
import es.jvbabi.clockme.data.calendar.local.CalendarEventDao
import es.jvbabi.clockme.data.calendar.local.DailyCalendarMinutesEntity
import es.jvbabi.clockme.data.calendar.local.EventSegmentEntity
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
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

class AndroidCalendarRepository(
    private val context: Context,
    private val permissionRepository: PermissionRepository,
    private val activeCalendarDao: ActiveCalendarDao,
    private val calendarEventDao: CalendarEventDao,
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
        if (!hasCalendarPermission()) return

        // null if the provider couldn't be queried, don't delete anything in that case
        val accounts = withContext(Dispatchers.IO) { queryAccounts() } ?: return
        val existing = accounts.flatMap { account ->
            account.calendars.map { calendar ->
                calendar.id to (account.toEntity() to calendar.toEntity(account))
            }
        }.toMap()
        activeCalendarDao.sync(existing)
    }

    override suspend fun getActiveCalendarIds(): List<Long> = activeCalendarDao.getCalendarIds()

    override suspend fun getEventInstances(
        calendarId: Long,
        from: Instant,
        to: Instant,
    ): List<EventInstance>? = withContext(Dispatchers.IO) {
        if (!hasCalendarPermission()) return@withContext null

        val uri = Instances.CONTENT_URI.buildUpon().also {
            ContentUris.appendId(it, from.toEpochMilliseconds())
            ContentUris.appendId(it, to.toEpochMilliseconds())
        }.build()
        val selection = buildString {
            append("${Instances.CALENDAR_ID} = ?")
            append(" AND ${Instances.ALL_DAY} = 0")
            append(" AND (${Instances.STATUS} IS NULL OR ${Instances.STATUS} != ${Events.STATUS_CANCELED})")
            append(" AND (${Instances.SELF_ATTENDEE_STATUS} IS NULL")
            append(" OR ${Instances.SELF_ATTENDEE_STATUS} != ${Attendees.ATTENDEE_STATUS_DECLINED})")
        }
        val cursor = try {
            context.contentResolver.query(uri, INSTANCE_PROJECTION, selection, arrayOf(calendarId.toString()), null)
        } catch (_: SecurityException) {
            null
        } ?: return@withContext null

        cursor.use {
            buildList {
                while (it.moveToNext()) {
                    add(
                        EventInstance(
                            eventId = it.getLong(INSTANCE_INDEX_EVENT_ID),
                            calendarId = it.getLong(INSTANCE_INDEX_CALENDAR_ID),
                            start = Instant.fromEpochMilliseconds(it.getLong(INSTANCE_INDEX_BEGIN)),
                            end = Instant.fromEpochMilliseconds(it.getLong(INSTANCE_INDEX_END)),
                        ),
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeCalendarProviderChanges(): Flow<Unit> =
        permissionRepository.observe(AppPermission.READ_CALENDAR)
            .map { it == PermissionStatus.Granted }
            .distinctUntilChanged()
            .flatMapLatest { granted -> if (granted) observeChanges(CalendarContract.CONTENT_URI) else emptyFlow() }

    override suspend fun replaceEventData(
        calendarId: Long,
        from: LocalDate,
        to: LocalDate,
        segments: List<EventSegment>,
        dailyMinutes: List<DailyCalendarMinutes>,
    ) {
        calendarEventDao.replace(
            calendarId = calendarId,
            from = from,
            to = to,
            segments = segments.map {
                EventSegmentEntity(
                    calendarId = it.calendarId,
                    eventId = it.eventId,
                    date = it.date,
                    start = it.start,
                    end = it.end,
                )
            },
            dailyMinutes = dailyMinutes.map {
                DailyCalendarMinutesEntity(calendarId = it.calendarId, date = it.date, minutes = it.minutes)
            },
        )
    }

    override fun observeEventSegments(): Flow<List<EventSegment>> =
        calendarEventDao.observeSegments().map { segments ->
            segments.map {
                EventSegment(
                    calendarId = it.calendarId,
                    eventId = it.eventId,
                    date = it.date,
                    start = it.start,
                    end = it.end,
                )
            }
        }

    override fun observeDailyMinutes(): Flow<List<DailyCalendarMinutes>> =
        calendarEventDao.observeDailyMinutes().map { minutes ->
            minutes.map { DailyCalendarMinutes(calendarId = it.calendarId, date = it.date, minutes = it.minutes) }
        }

    private fun hasCalendarPermission() = ContextCompat.checkSelfPermission(
        context,
        AppPermission.READ_CALENDAR.manifestName,
    ) == PackageManager.PERMISSION_GRANTED

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
        val INSTANCE_PROJECTION = arrayOf(
            Instances.EVENT_ID,
            Instances.CALENDAR_ID,
            Instances.BEGIN,
            Instances.END,
        )
        const val INSTANCE_INDEX_EVENT_ID = 0
        const val INSTANCE_INDEX_CALENDAR_ID = 1
        const val INSTANCE_INDEX_BEGIN = 2
        const val INSTANCE_INDEX_END = 3

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
