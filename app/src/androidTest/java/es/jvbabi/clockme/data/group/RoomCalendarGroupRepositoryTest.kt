package es.jvbabi.clockme.data.group

import androidx.room3.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import es.jvbabi.clockme.data.database.ClockMeDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomCalendarGroupRepositoryTest {

    private val database = Room.inMemoryDatabaseBuilder<ClockMeDatabase>(
        InstrumentationRegistry.getInstrumentation().targetContext,
    ).build()
    private val repository = RoomCalendarGroupRepository(database.calendarGroupDao())

    @After
    fun tearDown() = database.close()

    private suspend fun groups() = repository.observeGroups().first()

    @Test
    fun createsGroupsSortedByName() = runBlocking {
        val sports = repository.createGroup("sports", 0xFF00FF00.toInt(), setOf(3))
        val uni = repository.createGroup(" University ", null, setOf(1, 2))

        assertEquals(
            listOf(
                CalendarGroup(sports, "sports", 0xFF00FF00.toInt(), setOf(3)),
                CalendarGroup(uni, "University", null, setOf(1, 2)),
            ),
            groups(),
        )
    }

    @Test
    fun updatesNameColorAndCalendars() = runBlocking {
        val id = repository.createGroup("Uni", null, setOf(1, 2))

        repository.updateGroup(id, "University", 0xFFFF0000.toInt(), setOf(2, 3))

        assertEquals(listOf(CalendarGroup(id, "University", 0xFFFF0000.toInt(), setOf(2, 3))), groups())
    }

    @Test
    fun addsAndRemovesSingleCalendars() = runBlocking {
        val id = repository.createGroup("Uni", null, setOf(1))

        repository.setCalendarInGroup(id, 2, inGroup = true)
        repository.setCalendarInGroup(id, 2, inGroup = true)
        repository.setCalendarInGroup(id, 1, inGroup = false)

        assertEquals(setOf(2L), groups().single().calendarIds)
    }

    @Test
    fun calendarIsInOneGroupOnly() = runBlocking {
        val a = repository.createGroup("A", null, setOf(1, 2))
        val b = repository.createGroup("B", null, setOf(1))
        assertEquals(listOf(setOf(2L), setOf(1L)), groups().map { it.calendarIds })

        repository.setCalendarInGroup(a, 1, inGroup = true)
        assertEquals(listOf(setOf(1L, 2L), emptySet<Long>()), groups().map { it.calendarIds })

        repository.updateGroup(b, "B", null, setOf(2))
        assertEquals(listOf(setOf(1L), setOf(2L)), groups().map { it.calendarIds })
    }

    @Test
    fun deletingGroupRemovesItsMembers() = runBlocking {
        val id = repository.createGroup("Uni", null, setOf(1, 2))
        val other = repository.createGroup("Other", null, setOf(3))

        repository.deleteGroup(id)

        assertEquals(listOf(CalendarGroup(other, "Other", null, setOf(3))), groups())
    }

    @Test
    fun removesCalendarsThatNoLongerExist() = runBlocking {
        val id = repository.createGroup("Uni", null, setOf(1, 2, 3))

        repository.removeCalendarsNotIn(setOf(2))

        assertEquals(listOf(CalendarGroup(id, "Uni", null, setOf(2))), groups())
    }
}
