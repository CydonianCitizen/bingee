package com.cydoniancitizen.bingee.domain.policy

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class ViewingDatePolicyTest {
    private val rome = ZoneId.of("Europe/Rome")

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
