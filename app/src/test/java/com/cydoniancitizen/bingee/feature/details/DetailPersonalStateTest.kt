package com.cydoniancitizen.bingee.feature.details

import com.cydoniancitizen.bingee.core.model.CacheFreshness
import com.cydoniancitizen.bingee.core.model.CachedMediaDetails
import com.cydoniancitizen.bingee.core.model.CachedSeason
import com.cydoniancitizen.bingee.core.model.Episode
import com.cydoniancitizen.bingee.core.model.EpisodePosition
import com.cydoniancitizen.bingee.core.model.EpisodeWatchState
import com.cydoniancitizen.bingee.core.model.ExternalMediaRef
import com.cydoniancitizen.bingee.core.model.LibraryProgress
import com.cydoniancitizen.bingee.core.model.MediaDetails
import com.cydoniancitizen.bingee.core.model.MediaSource
import com.cydoniancitizen.bingee.core.model.MediaType
import com.cydoniancitizen.bingee.core.model.ProductionStatus
import com.cydoniancitizen.bingee.core.model.Season
import com.cydoniancitizen.bingee.core.model.TrackedEpisode
import com.cydoniancitizen.bingee.core.model.deriveSeasonProgress
import com.cydoniancitizen.bingee.core.model.deriveSeriesProgress
import com.cydoniancitizen.bingee.core.result.AppError
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailPersonalStateTest {
    private val now = Instant.parse("2026-10-19T10:00:00Z")
    private val ref = ExternalMediaRef(MediaSource.TMDB, "100")
    private val seasonRef = ExternalMediaRef(MediaSource.TMDB, "200")

    @Test
    fun relationshipLabelsIncludeCanceledAbandonedOutsideAndNotStarted() {
        assertEquals(DetailSeriesLabel.WATCH_LATER, state(watched = 0).seriesPersonalSummary()?.label)
        assertEquals(
            DetailSeriesLabel.ABANDONED,
            state(watched = 1).copy(isAbandoned = true).seriesPersonalSummary()?.label
        )
        assertEquals(
            DetailSeriesLabel.OUTSIDE_COLLECTION,
            state(watched = 1).copy(isInLibrary = false).seriesPersonalSummary()?.label
        )
        assertEquals(
            DetailSeriesLabel.WATCHED,
            state(watched = 2, status = ProductionStatus.CANCELED).seriesPersonalSummary()?.label
        )
    }

    @Test
    fun oldCompleteMetadataKeepsEvidenceWhileEmptyLoadingAndErrorsRemainDistinct() {
        val original = state(watched = 2).copy(seriesCompletedAt = now)
        val ready = original.series.content as SeriesContentState.Ready
        val old = ready.seasons.single().copy(episodesFetchedAt = null, episodeCacheFreshness = CacheFreshness.STALE)
        val oldState = original.copy(series = original.series.copy(content = ready.copy(seasons = listOf(old))))
        assertEquals(DetailSeriesLabel.CAUGHT_UP, oldState.seriesPersonalSummary()?.label)
        assertTrue(oldState.seriesPersonalSummary()!!.canEditCompletionDate)
        val loading = original.copy(series = original.series.copy(content = SeriesContentState.Loading))
        val failed = original.copy(
            series = original.series.copy(content = SeriesContentState.Error(AppError.LocalStorageFailure))
        )
        assertNull(loading.seriesPersonalSummary())
        assertNull(failed.seriesPersonalSummary())
        val empty = original.copy(
            libraryProgress = LibraryProgress.Unavailable,
            series = original.series.copy(
                content = SeriesContentState.Ready(emptyList(), deriveSeriesProgress(emptyList()))
            )
        ).seriesPersonalSummary()!!
        assertEquals(DetailSeriesLabel.WATCH_LATER, empty.label)
        assertTrue(empty.incompleteMetadata)
        assertFalse(empty.canEditCompletionDate)
    }

    @Test
    fun completedAvailableProgressDistinguishesReturningEndedAndUnknownShows() {
        assertEquals(DetailSeriesLabel.CAUGHT_UP, state(watched = 2).seriesPersonalSummary()?.label)
        assertEquals(
            DetailSeriesLabel.WATCHED,
            state(watched = 2, status = ProductionStatus.ENDED).seriesPersonalSummary()?.label
        )
        assertEquals(
            DetailSeriesLabel.AVAILABLE_WATCHED,
            state(watched = 2, status = ProductionStatus.UNKNOWN).seriesPersonalSummary()?.label
        )
    }

    @Test
    fun missingCoverageNeverClaimsCaughtUpAndSpecialsNeverCompleteTheSeries() {
        val partial = state(watched = 2, declared = 3).copy(seriesCompletedAt = now).seriesPersonalSummary()!!
        assertEquals(DetailSeriesLabel.WATCHING, partial.label)
        assertTrue(partial.incompleteMetadata)
        assertFalse(partial.canEditCompletionDate)
        val original = state(watched = 2)
        val content = original.series.content as SeriesContentState.Ready
        val specials = content.seasons.single().let { it.copy(season = it.season.copy(seasonNumber = 0)) }
        val onlySpecials = original.copy(
            libraryProgress = LibraryProgress.Series(deriveSeriesProgress(listOf(specials))),
            series = original.series.copy(
                content = SeriesContentState.Ready(listOf(specials), deriveSeriesProgress(listOf(specials)))
            )
        )
        assertEquals(DetailSeriesLabel.WATCH_LATER, onlySpecials.seriesPersonalSummary()?.label)
    }

    @Test
    fun zeroEpisodesNeverCertifyCompletionEvenWithAnOldCompletionRecord() {
        for ((declared, knownEmpty) in listOf(0 to false, 3 to false, 0 to true)) {
            val original = state(watched = 0, stored = 0, declared = declared, status = ProductionStatus.ENDED)
            val ready = original.series.content as SeriesContentState.Ready
            val seasons = ready.seasons.map { it.copy(isKnownEmpty = knownEmpty) }
            val progress = deriveSeriesProgress(seasons)
            val summary = original.copy(
                seriesCompletedAt = now,
                libraryProgress = LibraryProgress.Series(progress),
                series = original.series.copy(content = SeriesContentState.Ready(seasons, progress))
            ).seriesPersonalSummary()!!
            assertEquals(DetailSeriesLabel.WATCH_LATER, summary.label)
            assertEquals(!knownEmpty, summary.incompleteMetadata)
            assertFalse(summary.canEditCompletionDate)
            assertNull(summary.nextEpisode)
        }
    }

    @Test
    fun outsideCollectionAndAbandonedTakePrecedenceOverCompletedProduction() {
        val completed = state(watched = 2, status = ProductionStatus.ENDED).copy(seriesCompletedAt = now)
        val abandoned = completed.copy(isAbandoned = true)
        assertEquals(DetailSeriesLabel.ABANDONED, abandoned.seriesPersonalSummary()?.label)
        assertNull(abandoned.seriesPersonalSummary()?.nextEpisode)
        val outside = abandoned.copy(isInLibrary = false)
        assertEquals(DetailSeriesLabel.OUTSIDE_COLLECTION, outside.seriesPersonalSummary()?.label)
        assertNull(outside.seriesPersonalSummary()?.nextEpisode)
    }

    @Test
    fun nextEpisodeComesFromCanonicalProjectionAndRequiresCurrentEligibleEpisode() {
        val original = state(watched = 1)
        assertEquals(DetailSeriesLabel.WATCHING, original.seriesPersonalSummary()?.label)
        val next = original.seriesPersonalSummary()?.nextEpisode
        assertEquals(2, next?.episode?.episodeNumber)
        assertNull(original.copy(isAbandoned = true).seriesPersonalSummary()?.nextEpisode)
        assertNull(original.copy(isInLibrary = false).seriesPersonalSummary()?.nextEpisode)
        assertNull(state(watched = 0).seriesPersonalSummary()?.nextEpisode)
        assertNull(original.copy(libraryProgress = LibraryProgress.Unavailable).seriesPersonalSummary()?.nextEpisode)
        val content = original.series.content as SeriesContentState.Ready
        val stale = original.copy(
            series = original.series.copy(
                content = content.copy(
                    seasons = content.seasons.map { season ->
                        season.copy(
                            episodes = season.episodes.map { it.copy(watchState = EpisodeWatchState.Watched(now)) }
                        )
                    }
                )
            )
        )
        assertNull(stale.seriesPersonalSummary()?.nextEpisode)
    }

    @Test
    fun completionDateRequiresActualRecordAndTrustworthyCurrentCompletion() {
        val complete = state(watched = 2)
        assertFalse(complete.seriesPersonalSummary()!!.canEditCompletionDate)
        assertTrue(complete.copy(seriesCompletedAt = now).seriesPersonalSummary()!!.canEditCompletionDate)
        assertFalse(state(watched = 1).copy(seriesCompletedAt = now).seriesPersonalSummary()!!.canEditCompletionDate)
        assertNull(complete.copy(isInLibrary = null).seriesPersonalSummary())
    }

    @Test
    fun knownFutureEpisodesPreventAFinalWatchedLabelEvenForAnEndedShow() {
        val original = state(watched = 2, status = ProductionStatus.ENDED)
        val content = original.series.content as SeriesContentState.Ready
        val season = content.seasons.single()
        val future = season.episodes.last().let {
            it.copy(
                episode = it.episode.copy(episodeNumber = 3, externalRef = ExternalMediaRef(MediaSource.TMDB, "999")),
                watchState = EpisodeWatchState.Unavailable
            )
        }
        val withFuture = season.copy(season = season.season.copy(episodeCount = 3), episodes = season.episodes + future)
        val changed = original.copy(
            series = original.series.copy(
                content = SeriesContentState.Ready(
                    listOf(withFuture),
                    deriveSeriesProgress(listOf(withFuture))
                )
            )
        )
        assertEquals(DetailSeriesLabel.AVAILABLE_WATCHED, changed.seriesPersonalSummary()?.label)
    }

    @Test
    fun asynchronousProgressSnapshotsNeverOfferAStaleNextEpisodeOrCompletionDate() {
        val original = state(watched = 1, stored = 3)
        val ready = original.series.content as SeriesContentState.Ready
        val season = ready.seasons.single()
        val reversedEpisodes = season.episodes.map {
            it.copy(
                watchState = if (it.episode.episodeNumber != 3) {
                    EpisodeWatchState.Unwatched
                } else {
                    EpisodeWatchState.Watched(now)
                }
            )
        }
        val reversedSeason = season.copy(episodes = reversedEpisodes, progress = deriveSeasonProgress(reversedEpisodes))
        val reversed = original.copy(
            series = original.series.copy(
                content = SeriesContentState.Ready(listOf(reversedSeason), deriveSeriesProgress(listOf(reversedSeason)))
            )
        )
        // Equal counts, different order: the old projected E2 cannot skip the now-unwatched E1.
        assertNull(reversed.seriesPersonalSummary()?.nextEpisode)
        val completed = state(watched = 2).copy(seriesCompletedAt = now)
        val seasonsFirst = completed.copy(libraryProgress = original.libraryProgress)
        assertNull(seasonsFirst.seriesPersonalSummary()?.nextEpisode)
        assertFalse(seasonsFirst.seriesPersonalSummary()?.canEditCompletionDate == true)
        val projectionFirst = original.copy(libraryProgress = completed.libraryProgress, seriesCompletedAt = now)
        assertNull(projectionFirst.seriesPersonalSummary()?.nextEpisode)
        assertFalse(projectionFirst.seriesPersonalSummary()?.canEditCompletionDate == true)
    }

    @Test
    fun episodeOffsetTracksOnlyExpandedRegularRowsWithoutUsingFetchTimestamps() {
        val original = state(watched = 1)
        val content = original.series.content as SeriesContentState.Ready
        val cached = content.seasons.single().copy(episodesFetchedAt = null)
        val series = original.series.copy(content = content.copy(seasons = listOf(cached)))
        val target = cached.episodes.last().episode.externalRef
        assertNull(seriesEpisodeItemOffset(series, target))
        assertEquals(3, seriesEpisodeItemOffset(series.copy(expandedSeasons = setOf(seasonRef)), target))
    }

    private fun state(
        watched: Int,
        stored: Int = 2,
        declared: Int = stored,
        status: ProductionStatus = ProductionStatus.RETURNING_SERIES
    ): MediaDetailsUiState {
        val episodes = (1..stored).map { number ->
            TrackedEpisode(
                Episode(
                    ref,
                    seasonRef,
                    ExternalMediaRef(MediaSource.TMDB, "${300 + number}"),
                    1,
                    number,
                    "Episode $number"
                ),
                if (number <= watched) EpisodeWatchState.Watched(now) else EpisodeWatchState.Unwatched
            )
        }
        val season = CachedSeason(
            Season(ref, seasonRef, 1, episodeCount = declared),
            now,
            now,
            episodes,
            deriveSeasonProgress(episodes),
            CacheFreshness.FRESH
        )
        val progress = deriveSeriesProgress(listOf(season))
        return MediaDetailsUiState(
            today = LocalDate.of(2026, 10, 19),
            content = DetailContentState.Content(
                CachedMediaDetails(
                    MediaDetails(ref, MediaType.SERIES, "Series", productionStatus = status),
                    now,
                    CacheFreshness.FRESH
                )
            ),
            isInLibrary = true,
            libraryProgress = LibraryProgress.Series(progress.copy(nextEpisode = EpisodePosition(1, 2))),
            series = SeriesDetailUiState(content = SeriesContentState.Ready(listOf(season), progress))
        )
    }
}
