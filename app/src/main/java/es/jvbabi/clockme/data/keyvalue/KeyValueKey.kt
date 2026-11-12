package es.jvbabi.clockme.data.keyvalue

/**
 * All keys of the key-value store. [key] is what is stored in the database, so it must
 * never change, even if the constant is renamed.
 */
enum class KeyValueKey(val key: String) {
    /** When the calendar events were last synced, as epoch milliseconds. */
    LAST_EVENT_SYNC("last_event_sync"),
}
