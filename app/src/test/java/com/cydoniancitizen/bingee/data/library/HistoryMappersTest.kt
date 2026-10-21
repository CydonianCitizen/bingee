package com.cydoniancitizen.bingee.data.library

import com.cydoniancitizen.bingee.core.model.HistoryContext
import com.cydoniancitizen.bingee.core.model.HistoryCursor
import com.cydoniancitizen.bingee.core.model.HistoryOverviewQuery
import com.cydoniancitizen.bingee.core.model.HistoryPageQuery
import com.cydoniancitizen.bingee.data.library.local.HistoryOverviewRow
import com.cydoniancitizen.bingee.data.library.local.HistoryRow
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryMappersTest {
    private val day = LocalDate.of(2026, 10, 2)
    private val context = HistoryContext(day, ZoneId.of("America/New_York"))

    @Test fun sentinelIsNotRetainedAndReverseOrderProducesCanonicalPage() {
        val rows = (1L..31L).map { HistoryRow(day, 0, it, "Movie", null, it.toString(), 0) }
        val query = HistoryPageQuery(YearMonth.from(day), context)
        val page = rows.toHistoryPage(query)
        assertEquals(30, page.items.size)
        assertTrue(page.hasNext)
        assertFalse(page.hasPrevious)
        val reverse = rows.reversed().toHistoryPage(query.copy(backwards = true, cursor = HistoryCursor(day, 0, 32)))
        assertEquals((2L..31L).toList(), reverse.items.map { it.mediaId })
        assertTrue(reverse.hasPrevious)
        assertTrue(reverse.hasNext)
    }

    @Test fun invalidNavigationIdentityRemainsReadable() {
        val page = listOf(
            HistoryRow(day, 0, 1, "Movie", null, "invalid", 0)
        ).toHistoryPage(HistoryPageQuery(YearMonth.from(day), context))
        assertEquals("Movie", page.items.single().title)
        assertNull(page.items.single().detailsRef)
    }

    @Test fun overviewUsesManualDatesWithoutZoneConversionAndTimestampDatesWithIt() {
        val overview = HistoryOverviewRow(
            "2026-09-01",
            "2026-09-01",
            "2026-10-01T00:30:00",
            "2026-10-01T00:30:00",
            null,
            null
        )
            .toHistoryOverview(HistoryOverviewQuery(context))
        assertEquals(LocalDate.of(2026, 9, 1), overview.firstDay)
        assertEquals(LocalDate.of(2026, 9, 30), overview.lastDay)
    }
}
