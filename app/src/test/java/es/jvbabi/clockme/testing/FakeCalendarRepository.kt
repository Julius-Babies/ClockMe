package es.jvbabi.clockme.testing

import es.jvbabi.clockme.data.calendar.CalendarAccount
import es.jvbabi.clockme.data.calendar.CalendarRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeCalendarRepository(
    initialAccounts: List<CalendarAccount> = emptyList(),
) : CalendarRepository {

    val accounts = MutableStateFlow(initialAccounts)

    override fun observeAccounts(): Flow<List<CalendarAccount>> = accounts
}
