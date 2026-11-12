package es.jvbabi.clockme.testing

import es.jvbabi.clockme.core.time.TimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.TimeZone
import kotlin.time.Duration
import kotlin.time.Instant

/** A clock that only moves when the test sets [now]; every change is a tick. */
class FakeTimeProvider(
    now: Instant,
    private val timeZone: TimeZone = TimeZone.of("Europe/Berlin"),
) : TimeProvider() {

    private val current = MutableStateFlow(now)

    var now: Instant
        get() = current.value
        set(value) {
            current.value = value
        }

    override fun now(): Instant = current.value

    override fun timeZone(): TimeZone = timeZone

    override fun ticks(period: Duration): Flow<Instant> = current
}
