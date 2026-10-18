package com.cydoniancitizen.bingee.data.library.local

import androidx.room.TypeConverter
import com.cydoniancitizen.bingee.core.model.MediaSource
import com.cydoniancitizen.bingee.core.model.MediaType
import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

internal class RoomConverters {
    @TypeConverter
    fun mediaSourceToString(value: MediaSource): String = value.name

    @TypeConverter
    fun stringToMediaSource(value: String): MediaSource = MediaSource.valueOf(value)

    @TypeConverter
    fun mediaTypeToString(value: MediaType): String = value.name

    @TypeConverter
    fun stringToMediaType(value: String): MediaType = MediaType.valueOf(value)

    @TypeConverter
    fun localDateToString(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun stringToLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun instantToString(value: Instant): String = value.toString()

    @TypeConverter
    fun stringToInstant(value: String): Instant {
        // Room writes Instant.toString(): parse its common UTC shape without the generic formatter.
        val fractionDigits = when (value.length) {
            20 -> 0
            24 -> 3
            27 -> 6
            30 -> 9
            else -> return Instant.parse(value)
        }
        if (value[4] != '-' || value[7] != '-' || value[10] != 'T' ||
            value[13] != ':' || value[16] != ':' || value.last() != 'Z' ||
            (fractionDigits != 0 && value[19] != '.')
        ) {
            return Instant.parse(value)
        }

        val year = value.decimal(0, 4) ?: return Instant.parse(value)
        val month = value.decimal(5, 7) ?: return Instant.parse(value)
        val day = value.decimal(8, 10) ?: return Instant.parse(value)
        val hour = value.decimal(11, 13) ?: return Instant.parse(value)
        val minute = value.decimal(14, 16) ?: return Instant.parse(value)
        val second = value.decimal(17, 19) ?: return Instant.parse(value)
        val nanos = if (fractionDigits == 0) {
            0
        } else {
            val fraction = value.decimal(20, value.lastIndex) ?: return Instant.parse(value)
            fraction * when (fractionDigits) {
                3 -> 1_000_000
                6 -> 1_000
                else -> 1
            }
        }
        return try {
            LocalDate.of(year, month, day).atTime(hour, minute, second, nanos).toInstant(ZoneOffset.UTC)
        } catch (_: DateTimeException) {
            // Preserve ISO parsing of legacy forms, leap seconds, midnight 24:00 and invalid input.
            Instant.parse(value)
        }
    }

    private fun String.decimal(start: Int, end: Int): Int? {
        var result = 0
        for (index in start until end) {
            val digit = this[index] - '0'
            if (digit !in 0..9) return null
            result = result * 10 + digit
        }
        return result
    }
}
