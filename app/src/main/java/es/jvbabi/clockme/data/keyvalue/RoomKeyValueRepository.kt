package es.jvbabi.clockme.data.keyvalue

import es.jvbabi.clockme.data.keyvalue.local.KeyValueDao
import es.jvbabi.clockme.data.keyvalue.local.KeyValueEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

class RoomKeyValueRepository(
    private val dao: KeyValueDao,
) : KeyValueRepository {

    override fun observe(key: KeyValueKey): Flow<String?> = dao.observe(key.key).distinctUntilChanged()

    override suspend fun get(key: KeyValueKey): String? = dao.get(key.key)

    override suspend fun set(key: KeyValueKey, value: String?) {
        if (value == null) dao.delete(key.key) else dao.upsert(KeyValueEntity(key.key, value))
    }
}
