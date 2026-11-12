package es.jvbabi.clockme.data.keyvalue.local

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "key_value")
data class KeyValueEntity(
    @PrimaryKey val key: String,
    val value: String,
)
