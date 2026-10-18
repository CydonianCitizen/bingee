package com.cydoniancitizen.bingee.data.library

import com.cydoniancitizen.bingee.core.model.MediaSource
import com.cydoniancitizen.bingee.core.model.MediaType
import com.cydoniancitizen.bingee.data.library.local.RoomConverters
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeParseException
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RoomConvertersTest {
    private val converters = RoomConverters()

    @Test
    fun enumsPersistByStableNameInsteadOfOrdinal() {
        assertEquals("TMDB", converters.mediaSourceToString(MediaSource.TMDB))
        assertEquals(MediaSource.IMDB, converters.stringToMediaSource("IMDB"))
        assertEquals("MOVIE", converters.mediaTypeToString(MediaType.MOVIE))
        assertEquals(MediaType.SERIES, converters.stringToMediaType("SERIES"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun malformedEnumFailsInsteadOfSilentlyChangingMeaning() {
        converters.stringToMediaSource("UNKNOWN")
    }

    @Test
    fun datesAndInstantsUseIsoUtcText() {
        val date = LocalDate.of(2026, 8, 1)
        val instant = Instant.parse("2026-08-01T12:34:56Z")

        assertEquals("2026-08-01", converters.localDateToString(date))
        assertEquals(date, converters.stringToLocalDate("2026-08-01"))
        assertNull(converters.localDateToString(null))
        assertNull(converters.stringToLocalDate(null))
        assertEquals("2026-08-01T12:34:56Z", converters.instantToString(instant))
        assertEquals(instant, converters.stringToInstant("2026-08-01T12:34:56Z"))
    }

    @Test
    fun persistedInstantsPreserveEveryNanosecondAcrossTheCanonicalYearRange() {
        val random = Random(1245)
        repeat(2_000) {
            val instant = Instant.ofEpochSecond(
                random.nextLong(-62_167_219_200L, 253_402_300_800L),
                random.nextInt(1_000_000_000).toLong()
            )
            assertEquals(instant, converters.stringToInstant(converters.instantToString(instant)))
        }
        listOf(0L, 1L, 1_000L, 1_000_000L, 123_456_789L, 999_999_999L).forEach { nanos ->
            val instant = Instant.ofEpochSecond(-1L, nanos)
            assertEquals(instant, converters.stringToInstant(instant.toString()))
        }
    }

    @Test
    fun alternateIsoSpellingsKeepTheExistingInstantParseContract() {
        listOf(
            "2026-08-01T12:34:56.1Z",
            "2026-08-01T12:34:56.12Z",
            "2026-08-01T12:34:56.1234Z",
            "2026-08-01T12:34:56.12345Z",
            "2026-08-01T12:34:56.1234567Z",
            "2026-08-01T12:34:56.12345678Z",
            "2026-08-01t12:34:56z",
            "2026-08-01T14:34:56.123456789+02:00",
            "2016-12-31T23:59:60Z",
            "2026-08-01T24:00:00.000Z",
            "0000-02-29T00:00:00Z",
            "-0001-12-31T23:59:59Z",
            "+10000-01-01T00:00:00Z"
        ).forEach { text ->
            assertEquals(text, Instant.parse(text), converters.stringToInstant(text))
        }
    }

    @Test
    fun malformedInstantsStillFailWithParseErrorsInsteadOfNormalizingOrTruncating() {
        listOf(
            "2026-02-29T00:00:00Z",
            "2026-04-31T00:00:00Z",
            "2026-13-01T00:00:00Z",
            "2026-08-01T25:00:00Z",
            "2026-08-01T12:60:00Z",
            "2026-08-01T12:34:61Z",
            "2026-08-01T12:34:56.1234567890Z",
            "2026-08-01T12:34:56,123Z",
            "2026-08-01T12:34:56.12xZ",
            "2026-08-01T12:34:56",
            "2026-08-01T12:34:56Z ",
            "２０２６-08-01T12:34:56Z"
        ).forEach { text ->
            org.junit.Assert.assertThrows(text, DateTimeParseException::class.java) {
                converters.stringToInstant(text)
            }
        }
    }
}
