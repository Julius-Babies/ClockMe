package es.jvbabi.clockme.testing

import es.jvbabi.clockme.data.keyvalue.KeyValueKey
import es.jvbabi.clockme.data.keyvalue.KeyValueRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeKeyValueRepository : KeyValueRepository {

    val values = MutableStateFlow<Map<KeyValueKey, String>>(emptyMap())

    override fun observe(key: KeyValueKey): Flow<String?> = values.map { it[key] }

    override suspend fun get(key: KeyValueKey): String? = values.value[key]

    override suspend fun set(key: KeyValueKey, value: String?) {
        values.update { if (value == null) it - key else it + (key to value) }
    }
}
