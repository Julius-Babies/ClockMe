package es.jvbabi.clockme.data.keyvalue

import androidx.room3.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import es.jvbabi.clockme.data.database.ClockMeDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Instant

@RunWith(AndroidJUnit4::class)
class RoomKeyValueRepositoryTest {

    private val database = Room.inMemoryDatabaseBuilder<ClockMeDatabase>(
        InstrumentationRegistry.getInstrumentation().targetContext,
    ).build()
    private val repository = RoomKeyValueRepository(database.keyValueDao())

    @After
    fun tearDown() = database.close()

    @Test
    fun missingKeyIsNull() = runBlocking {
        assertNull(repository.get(KeyValueKey.LAST_EVENT_SYNC))
        assertNull(repository.observe(KeyValueKey.LAST_EVENT_SYNC).first())
    }

    @Test
    fun storesOverwritesAndRemovesValues() = runBlocking {
        repository.set(KeyValueKey.LAST_EVENT_SYNC, "a")
        assertEquals("a", repository.get(KeyValueKey.LAST_EVENT_SYNC))

        repository.set(KeyValueKey.LAST_EVENT_SYNC, "b")
        assertEquals("b", repository.observe(KeyValueKey.LAST_EVENT_SYNC).first())

        repository.set(KeyValueKey.LAST_EVENT_SYNC, null)
        assertNull(repository.get(KeyValueKey.LAST_EVENT_SYNC))
    }

    @Test
    fun storesInstants() = runBlocking {
        val instant = Instant.fromEpochMilliseconds(1_700_000_000_123)

        repository.setInstant(KeyValueKey.LAST_EVENT_SYNC, instant)

        assertEquals(instant, repository.getInstant(KeyValueKey.LAST_EVENT_SYNC))
        assertEquals(instant, repository.observeInstant(KeyValueKey.LAST_EVENT_SYNC).first())
    }
}
