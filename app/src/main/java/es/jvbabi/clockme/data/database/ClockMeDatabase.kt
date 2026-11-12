package es.jvbabi.clockme.data.database

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import es.jvbabi.clockme.data.calendar.local.ActiveCalendarDao
import es.jvbabi.clockme.data.calendar.local.ActiveCalendarEntity
import es.jvbabi.clockme.data.calendar.local.CalendarAccountEntity

/**
 * There are no migrations: on a schema change, bump [version] and the database is rebuilt
 * from scratch (all stored data is lost).
 */
@Database(
    entities = [
        CalendarAccountEntity::class,
        ActiveCalendarEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class ClockMeDatabase : RoomDatabase() {

    abstract fun activeCalendarDao(): ActiveCalendarDao

    companion object {
        private const val NAME = "clockme.db"

        fun create(context: Context): ClockMeDatabase =
            Room.databaseBuilder<ClockMeDatabase>(context, NAME)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
