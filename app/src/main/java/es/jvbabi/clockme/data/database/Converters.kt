package es.jvbabi.clockme.data.database

import androidx.room3.ColumnTypeConverter
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

class Converters {

    @ColumnTypeConverter
    fun localDateToEpochDays(date: LocalDate): Long = date.toEpochDays()

    @ColumnTypeConverter
    fun epochDaysToLocalDate(epochDays: Long): LocalDate = LocalDate.fromEpochDays(epochDays)

    @ColumnTypeConverter
    fun instantToEpochMillis(instant: Instant): Long = instant.toEpochMilliseconds()

    @ColumnTypeConverter
    fun epochMillisToInstant(epochMillis: Long): Instant = Instant.fromEpochMilliseconds(epochMillis)
}
