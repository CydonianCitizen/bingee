package com.cydoniancitizen.bingee.domain.policy

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object ViewingDatePolicy {
    fun effectiveMovieDate(watchedAt: Instant, watchedDate: LocalDate?, zoneId: ZoneId): LocalDate =
        watchedDate ?: effectiveEpisodeDate(watchedAt, zoneId)

    fun effectiveEpisodeDate(watchedAt: Instant, zoneId: ZoneId): LocalDate = watchedAt.atZone(zoneId).toLocalDate()

    fun dayWindow(day: LocalDate, zoneId: ZoneId): DayWindow = DayWindow(
        day.atStartOfDay(zoneId).toInstant(),
        day.plusDays(1).atStartOfDay(zoneId).toInstant()
    )
}

data class DayWindow(val start: Instant, val endExclusive: Instant)
