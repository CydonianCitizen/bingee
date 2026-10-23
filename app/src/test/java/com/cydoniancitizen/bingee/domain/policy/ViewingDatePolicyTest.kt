package com.cydoniancitizen.bingee.domain.policy

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class ViewingDatePolicyTest {
    private val rome = ZoneId.of("Europe/Rome")

    @Test fun episodeDateEditPreservesLocalTimeAndNanosecondsAcrossDst() {
        assertEquals(
            Instant.parse("2026-03-28T12:15:30.123456789Z"),
            ViewingDatePolicy.withEpisodeDate(
                Instant.parse("2026-08-03T11:15:30.123456789Z"),
                LocalDate.of(2026, 3, 28),
                rome
            )
        )
        assertEquals(
            Instant.parse("2026-03-29T01:30:00Z"),
            ViewingDatePolicy.withEpisodeDate(
                Instant.parse("2026-03-28T01:30:00Z"),
                LocalDate.of(2026, 3, 29),
                rome
            )
        )
    }

    @Test fun episodeDateEditRejectsSkippedCalendarDayAndKeepsOverlapOffset() {
        assertEquals(
            null,
            ViewingDatePolicy.withEpisodeDate(
                Instant.parse("2011-12-29T22:00:00Z"),
                LocalDate.of(2011, 12, 30),
                ZoneId.of("Pacific/Apia")
            )
        )
        val overlap = Instant.parse("2026-10-25T01:30:00Z")
        assertEquals(overlap, ViewingDatePolicy.withEpisodeDate(overlap, LocalDate.of(2026, 10, 25), rome))
    }

    @Test fun manualDateDoesNotChangeWithZone() {
        val date = LocalDate.of(2024, 2, 29)
        val instant = Instant.parse("2026-01-01T00:30:00Z")
        listOf(rome, ZoneId.of("America/New_York")).forEach {
            assertEquals(date, ViewingDatePolicy.effectiveMovieDate(instant, date, it))
        }
        assertEquals(
            LocalDate.of(2025, 12, 31),
            ViewingDatePolicy.effectiveEpisodeDate(instant, ZoneId.of("America/New_York"))
        )
    }

    @Test fun dayWindowsRespectDstLeapDayAndYearBoundaries() {
        listOf(
            "2026-03-29" to 23L,
            "2026-10-25" to 25L,
            "2024-02-29" to 24L,
            "2026-12-31" to 24L
        ).forEach { (date, hours) ->
            val day = LocalDate.parse(date)
            val window = ViewingDatePolicy.dayWindow(day, rome)
            assertEquals(hours, Duration.between(window.start, window.endExclusive).toHours())
            assertEquals(day, ViewingDatePolicy.effectiveEpisodeDate(window.start, rome))
            assertEquals(day, ViewingDatePolicy.effectiveEpisodeDate(window.endExclusive.minusNanos(1), rome))
            assertEquals(day.plusDays(1), ViewingDatePolicy.effectiveEpisodeDate(window.endExclusive, rome))
        }
    }
}
