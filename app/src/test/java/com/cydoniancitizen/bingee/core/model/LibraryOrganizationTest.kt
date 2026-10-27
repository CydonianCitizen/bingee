package com.cydoniancitizen.bingee.core.model

import com.cydoniancitizen.bingee.debug.organizeLibraryEntries
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryOrganizationTest {
    @Test
    fun defaultOrderKeepsRecencyThenNormalizedTitleOriginalTitleAndIdentity() {
        val sameTime = Instant.parse("2026-08-03T10:00:00Z")
        val entries = listOf(
            movie("30", title = "Beta", addedAt = sameTime),
            movie("2", title = "alpha", originalTitle = "B", addedAt = sameTime),
            movie("3", title = "ALPHA", originalTitle = "A", addedAt = sameTime),
            movie("1", title = "Alpha", originalTitle = "a", addedAt = sameTime),
            movie("0", title = "Zulu", addedAt = sameTime.plusSeconds(1))
        )
        assertEquals(listOf("0", "1", "3", "2", "30"), organizeLibraryEntries(entries, LibraryQuery()).ids())
        assertEquals(
            listOf("0", "1", "3", "2", "30"),
            organizeLibraryEntries(entries.reversed(), LibraryQuery()).ids()
        )
    }

    @Test
    fun mediaRestrictionPreservesTheFixedOrder() {
        val entries = listOf(movie("2"), movie("1"), movie("3").copy(mediaType = MediaType.SERIES))
        assertEquals(listOf("1", "2", "3"), organizeLibraryEntries(entries, LibraryQuery()).ids())
        assertEquals(
            listOf("1", "2"),
            organizeLibraryEntries(entries, LibraryQuery(LibraryMediaFilter.MOVIES)).ids()
        )
        assertEquals(
            listOf("3"),
            organizeLibraryEntries(entries, LibraryQuery(LibraryMediaFilter.TV_SERIES)).ids()
        )
    }

    private fun movie(
        id: String,
        title: String = "Movie",
        originalTitle: String? = null,
        addedAt: Instant = Instant.parse("2026-08-03T10:00:00Z")
    ) = LibraryEntry(
        mediaRef = ExternalMediaRef(MediaSource.TMDB, id),
        mediaType = MediaType.MOVIE,
        title = title,
        originalTitle = originalTitle,
        addedAt = addedAt
    )

    private fun List<LibraryEntry>.ids() = map { it.mediaRef.externalId }
}
