package es.jvbabi.clockme.testing

import es.jvbabi.clockme.data.calendar.Calendar
import es.jvbabi.clockme.data.calendar.CalendarAccount
import es.jvbabi.clockme.data.calendar.CalendarRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeCalendarRepository(
    initialAccounts: List<CalendarAccount> = emptyList(),
) : CalendarRepository {

    val accounts = MutableStateFlow(initialAccounts)
    val activeCalendars = MutableStateFlow<List<CalendarAccount>>(emptyList())

    var syncCount = 0
        private set

    override fun observeAccounts(): Flow<List<CalendarAccount>> = accounts

    override fun observeActiveCalendars(): Flow<List<CalendarAccount>> = activeCalendars

    override suspend fun setCalendarActive(account: CalendarAccount, calendar: Calendar, active: Boolean) {
        activeCalendars.update { current ->
            val others = current.filter { it.id != account.id }
            val calendars = current.find { it.id == account.id }?.calendars.orEmpty()
                .filter { it.id != calendar.id }
                .let { if (active) it + calendar else it }
            if (calendars.isEmpty()) others else others + account.copy(calendars = calendars)
        }
    }

    override suspend fun syncActiveCalendars() {
        syncCount++
    }
}
