package es.jvbabi.clockme.domain.calendar

import es.jvbabi.clockme.data.calendar.CalendarRepository
import es.jvbabi.clockme.data.keyvalue.KeyValueKey
import es.jvbabi.clockme.data.keyvalue.KeyValueRepository
import es.jvbabi.clockme.data.keyvalue.setInstant
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock

/**
 * Reads the events of every active calendar from the device and stores them split into days,
 * together with the minutes per calendar and day.
 *
 * Only a window of [PAST_DAYS] before and [FUTURE_DAYS] after today is synced, older data stays
 * as it is. Safe to call from several places at once, runs are serialized.
 */
class SyncCalendarEventsUseCase(
    private val calendarRepository: CalendarRepository,
    private val keyValueRepository: KeyValueRepository,
    private val clock: Clock = Clock.System,
    private val timeZone: () -> TimeZone = { TimeZone.currentSystemDefault() },
) {
    private val mutex = Mutex()

    /** @return `false` if the calendars couldn't be read (e.g. missing permission), nothing is changed then. */
    suspend operator fun invoke(): Boolean = mutex.withLock {
        val timeZone = timeZone()
        val today = clock.todayIn(timeZone)
        val from = today.minus(PAST_DAYS, DateTimeUnit.DAY)
        val to = today.plus(FUTURE_DAYS, DateTimeUnit.DAY)
        val windowStart = from.atStartOfDayIn(timeZone)
        val windowEnd = to.plus(1, DateTimeUnit.DAY).atStartOfDayIn(timeZone)

        for (calendarId in calendarRepository.getActiveCalendarIds()) {
            val instances = calendarRepository.getEventInstances(calendarId, windowStart, windowEnd)
                ?: return false
            val segments = instances
                .flatMap { it.splitIntoDays(timeZone) }
                .filter { it.date in from..to }
            calendarRepository.replaceEventData(
                calendarId = calendarId,
                from = from,
                to = to,
                segments = segments,
                dailyMinutes = segments.minutesPerCalendarAndDay(),
            )
        }
        keyValueRepository.setInstant(KeyValueKey.LAST_EVENT_SYNC, clock.now())
        true
    }

    companion object {
        const val PAST_DAYS = 365
        const val FUTURE_DAYS = 90
    }
}
