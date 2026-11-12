package es.jvbabi.clockme.core.time

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.TimeZone
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

/** The current time and time zone, replaceable in tests. */
open class TimeProvider(
    private val clock: Clock = Clock.System,
) {
    open fun now(): Instant = clock.now()

    open fun timeZone(): TimeZone = TimeZone.currentSystemDefault()

    /** Emits the current time right away and then every [period]. */
    open fun ticks(period: Duration): Flow<Instant> = flow {
        while (true) {
            emit(now())
            delay(period)
        }
    }
}
