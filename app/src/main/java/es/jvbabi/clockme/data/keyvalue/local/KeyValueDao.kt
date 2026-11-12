package es.jvbabi.clockme.data.keyvalue.local

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface KeyValueDao {

    @Query("SELECT value FROM key_value WHERE `key` = :key")
    fun observe(key: String): Flow<String?>

    @Query("SELECT value FROM key_value WHERE `key` = :key")
    suspend fun get(key: String): String?

    @Upsert
    suspend fun upsert(entity: KeyValueEntity)

    @Query("DELETE FROM key_value WHERE `key` = :key")
    suspend fun delete(key: String)
}
