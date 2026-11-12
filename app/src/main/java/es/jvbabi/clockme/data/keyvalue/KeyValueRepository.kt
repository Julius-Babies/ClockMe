package es.jvbabi.clockme.data.keyvalue

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Instant

/** Small persistent settings and state, addressed by a [KeyValueKey]. */
interface KeyValueRepository {

    fun observe(key: KeyValueKey): Flow<String?>

    suspend fun get(key: KeyValueKey): String?

    /** Stores [value], or removes the entry if it is `null`. */
    suspend fun set(key: KeyValueKey, value: String?)
}

fun KeyValueRepository.observeInstant(key: KeyValueKey): Flow<Instant?> =
    observe(key).map { it?.toLongOrNull()?.let(Instant::fromEpochMilliseconds) }

suspend fun KeyValueRepository.getInstant(key: KeyValueKey): Instant? =
    get(key)?.toLongOrNull()?.let(Instant::fromEpochMilliseconds)

suspend fun KeyValueRepository.setInstant(key: KeyValueKey, value: Instant?) =
    set(key, value?.toEpochMilliseconds()?.toString())
