package es.jvbabi.clockme.data.database

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.ColumnTypeConverters
import es.jvbabi.clockme.data.calendar.local.CalendarEventDao
import es.jvbabi.clockme.data.calendar.local.DailyCalendarMinutesEntity
import es.jvbabi.clockme.data.calendar.local.EventSegmentEntity
import es.jvbabi.clockme.data.calendar.local.ActiveCalendarDao
import es.jvbabi.clockme.data.calendar.local.ActiveCalendarEntity
import es.jvbabi.clockme.data.calendar.local.CalendarAccountEntity
import es.jvbabi.clockme.data.keyvalue.local.KeyValueDao
import es.jvbabi.clockme.data.keyvalue.local.KeyValueEntity

/**
 * There are no migrations: on a schema change, bump [version] and the database is rebuilt
 * from scratch (all stored data is lost).
 */
@Database(
    entities = [
        CalendarAccountEntity::class,
        ActiveCalendarEntity::class,
        KeyValueEntity::class,
        EventSegmentEntity::class,
        DailyCalendarMinutesEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
@ColumnTypeConverters(Converters::class)
abstract class ClockMeDatabase : RoomDatabase() {

    abstract fun activeCalendarDao(): ActiveCalendarDao

    abstract fun keyValueDao(): KeyValueDao

    abstract fun calendarEventDao(): CalendarEventDao

    companion object {
        private const val NAME = "clockme.db"

        fun create(context: Context): ClockMeDatabase =
            Room.databaseBuilder<ClockMeDatabase>(context, NAME)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
