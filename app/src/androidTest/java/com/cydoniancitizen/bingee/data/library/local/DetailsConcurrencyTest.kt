package com.cydoniancitizen.bingee.data.library.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.cydoniancitizen.bingee.core.model.Episode
import com.cydoniancitizen.bingee.core.model.ExternalMediaRef
import com.cydoniancitizen.bingee.core.model.MediaDetails
import com.cydoniancitizen.bingee.core.model.MediaSource
import com.cydoniancitizen.bingee.core.model.MediaType
import com.cydoniancitizen.bingee.core.model.Season
import com.cydoniancitizen.bingee.data.calendar.ReleaseEventProjector
import com.cydoniancitizen.bingee.data.calendar.RoomMetadataCalendarStore
import com.cydoniancitizen.bingee.data.tmdb.series.TmdbSeasonPayload
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DetailsConcurrencyTest {
    private lateinit var database: BingeeDatabase
    private lateinit var store: RoomMetadataCalendarStore
    private val reference = ExternalMediaRef(MediaSource.TMDB, "550")
    private val now = Instant.parse("2026-08-03T12:00:00Z")
    private val today = LocalDate.of(2026, 8, 3)

    @Before
    fun openDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            BingeeDatabase::class.java
        ).build()
        store = RoomMetadataCalendarStore(
            database,
            database.detailsDao(),
            database.seriesDao(),
            database.releaseEventDao(),
            ReleaseEventProjector()
        )
    }

    @After
    fun closeDatabase() = database.close()

    @Test
    fun metadataRefreshAndMovieProgressDateWritesKeepEveryPersonalField() = runBlocking {
        val movie = MediaDetails(reference, MediaType.MOVIE, "Saved movie", releaseDate = today.minusYears(1))
        val series = MediaDetails(reference, MediaType.SERIES, "Same ID series")
        store.storeDetails(reference, movie, emptyList(), now, "en-US")
        store.storeDetails(reference, series, emptyList(), now, "en-US")
        val original = database.detailsDao().getCachedDetails(MediaSource.TMDB, MediaType.MOVIE, "550")!!
        val addedAt = now.minusSeconds(3600)
        database.libraryDao().addToLibrary(original.media, MediaSource.TMDB, "550", addedAt)
        database.libraryDao().updateFavoriteState(MediaSource.TMDB, MediaType.MOVIE, "550", true)
        database.ratingDao().setRating(MediaSource.TMDB, MediaType.MOVIE, "550", 8, now)
        val rating = database.ratingDao().observeRating(MediaSource.TMDB, MediaType.MOVIE, "550").first()
        database.watchProgressDao().markMovieWatched(MediaSource.TMDB, "550", now)
        val gate = CompletableDeferred<Unit>()
        val refresh = async(Dispatchers.Default) {
            gate.await()
            store.storeDetails(
                reference,
                movie.copy(title = "Refreshed movie"),
                emptyList(),
                now.plusSeconds(60),
                "en-US"
            )
        }
        val progress = async(Dispatchers.Default) {
            gate.await()
            database.watchProgressDao().markMovieWatched(MediaSource.TMDB, "550", now.plusSeconds(30))
        }
        val date = async(Dispatchers.Default) {
            gate.await()
            database.watchProgressDao().setMediaWatchedDate(
                MediaSource.TMDB,
                MediaType.MOVIE,
                "550",
                today.minusDays(1),
                now.plusSeconds(40)
            )
        }
        gate.complete(Unit)
        refresh.await()
        assertEquals(ProgressWriteOutcome.SUCCESS, progress.await())
        assertEquals(ProgressWriteOutcome.SUCCESS, date.await())
        val saved = database.detailsDao().getCachedDetails(MediaSource.TMDB, MediaType.MOVIE, "550")!!
        assertEquals(original.media.localMediaId, saved.media.localMediaId)
        assertEquals("Refreshed movie", saved.media.title)
        assertTrue(saved.media.isFavorite)
        assertEquals(
            addedAt,
            database.libraryDao().observeLibraryItem(MediaSource.TMDB, MediaType.MOVIE, "550").first()?.addedAt
        )
        assertEquals(rating, database.ratingDao().observeRating(MediaSource.TMDB, MediaType.MOVIE, "550").first())
        val watched = database.watchProgressDao().observeMovieProgress(MediaSource.TMDB, "550").first()!!
        assertEquals(now.plusSeconds(30), watched.watchedAt)
        assertEquals(today.minusDays(1), watched.watchedDate)
        assertEquals(
            "Same ID series",
            database.detailsDao().getCachedDetails(MediaSource.TMDB, MediaType.SERIES, "550")?.media?.title
        )
    }

    @Test
    fun seasonRefreshConcurrentWithUnwatchAndDateSaveCannotResurrectCompletion() = runBlocking {
        val seasonRef = ExternalMediaRef(MediaSource.TMDB, "551")
        val season = Season(reference, seasonRef, 1, "Saved season", episodeCount = 2)
        val episodes = (1..2).map { number ->
            Episode(
                reference,
                seasonRef,
                ExternalMediaRef(MediaSource.TMDB, "55$number"),
                1,
                number,
                "Episode $number",
                airDate = today
            )
        }
        store.storeDetails(
            reference,
            MediaDetails(reference, MediaType.SERIES, "Saved series"),
            listOf(season),
            now,
            "en-US"
        )
        store.storeSeason(reference, TmdbSeasonPayload(season, episodes, "en-US"), now)
        val progress = database.watchProgressDao()
        episodes.forEach { progress.markEpisodeWatched(MediaSource.TMDB, it.externalRef.externalId, today, now) }
        assertEquals(
            ProgressWriteOutcome.SUCCESS,
            progress.setMediaWatchedDate(MediaSource.TMDB, MediaType.SERIES, "550", today, now)
        )
        val gate = CompletableDeferred<Unit>()
        val refresh = async(Dispatchers.Default) {
            gate.await()
            store.storeSeason(
                reference,
                TmdbSeasonPayload(
                    season,
                    episodes.map {
                        it.copy(title = "Updated ${it.episodeNumber}")
                    },
                    "en-US"
                ),
                now.plusSeconds(60)
            )
        }
        val unwatch = async(Dispatchers.Default) {
            gate.await()
            progress.markEpisodeUnwatched(MediaSource.TMDB, "552")
        }
        val date = async(Dispatchers.Default) {
            gate.await()
            progress.setMediaWatchedDate(
                MediaSource.TMDB,
                MediaType.SERIES,
                "550",
                today.minusDays(1),
                now.plusSeconds(40)
            )
        }
        gate.complete(Unit)
        refresh.await()
        assertEquals(ProgressWriteOutcome.SUCCESS, unwatch.await())
        assertTrue(date.await() in setOf(ProgressWriteOutcome.SUCCESS, ProgressWriteOutcome.INCOMPLETE))
        val saved = database.seriesDao().observeSeriesSeasons(MediaSource.TMDB, "550").first().single()
        assertEquals(listOf("Updated 1", "Updated 2"), saved.episodes.map { it.episode.title })
        assertEquals(now, saved.episodes.first().progress?.watchedAt)
        assertNull(saved.episodes.last().progress)
        assertTrue(database.portableSnapshotDao().readSnapshot().seriesProgress.isEmpty())
        assertEquals(
            ProgressWriteOutcome.INCOMPLETE,
            progress.setMediaWatchedDate(MediaSource.TMDB, MediaType.SERIES, "550", today, now)
        )
    }
}
