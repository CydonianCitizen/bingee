package com.cydoniancitizen.bingee.feature.details

import com.cydoniancitizen.bingee.core.model.ContinueWatchingItem
import com.cydoniancitizen.bingee.core.model.EpisodeWatchState
import com.cydoniancitizen.bingee.core.model.ExternalMediaRef
import com.cydoniancitizen.bingee.core.model.LibraryProgress
import com.cydoniancitizen.bingee.core.model.MediaType
import com.cydoniancitizen.bingee.core.model.ProductionStatus
import com.cydoniancitizen.bingee.core.model.SeriesTrackingState
import com.cydoniancitizen.bingee.core.model.TrackedEpisode
import com.cydoniancitizen.bingee.core.model.hasSufficientEpisodeCoverage
import com.cydoniancitizen.bingee.core.model.resolveSeriesTrackingState
import com.cydoniancitizen.bingee.domain.policy.ContinueWatchingPolicy

internal enum class DetailSeriesLabel {
    OUTSIDE_COLLECTION,
    WATCH_LATER,
    WATCHING,
    WATCHED,
    CAUGHT_UP,
    AVAILABLE_WATCHED,
    ABANDONED
}

internal data class DetailSeriesSummary(
    val label: DetailSeriesLabel,
    val incompleteMetadata: Boolean,
    val nextEpisode: TrackedEpisode?,
    val canEditCompletionDate: Boolean
)

internal fun MediaDetailsUiState.seriesPersonalSummary(): DetailSeriesSummary? {
    val details = (content as? DetailContentState.Content)?.cached?.details ?: return null
    if (details.mediaType != MediaType.SERIES || isInLibrary == null) return null
    val ready = series.content as? SeriesContentState.Ready ?: return null
    val projected = (libraryProgress as? LibraryProgress.Series)?.progress
    // Independent Room observations can arrive in either order; wait for matching progress facts.
    if (projected != null && projected.copy(
            watchedDate = null,
            lastWatchedAt = null,
            nextEpisode = null
        ) != ready.progress
    ) {
        return null
    }
    val regular = ready.seasons.filter { it.season.seasonNumber > 0 }
    val relationship = resolveSeriesTrackingState(isInLibrary, ready.progress, isAbandoned)
    val label = when (relationship) {
        null -> DetailSeriesLabel.OUTSIDE_COLLECTION
        SeriesTrackingState.WATCH_LATER -> DetailSeriesLabel.WATCH_LATER
        SeriesTrackingState.WATCHING -> DetailSeriesLabel.WATCHING
        SeriesTrackingState.ABANDONED -> DetailSeriesLabel.ABANDONED
        SeriesTrackingState.WATCHED -> when (details.productionStatus) {
            ProductionStatus.ENDED, ProductionStatus.CANCELED -> if (regular.any { season ->
                    season.episodes.any { it.watchState == EpisodeWatchState.Unavailable }
                }
            ) {
                DetailSeriesLabel.AVAILABLE_WATCHED
            } else {
                DetailSeriesLabel.WATCHED
            }
            ProductionStatus.RETURNING_SERIES -> DetailSeriesLabel.CAUGHT_UP
            else -> DetailSeriesLabel.AVAILABLE_WATCHED
        }
    }
    val position = projected?.nextEpisode
    val candidate = ContinueWatchingItem(
        mediaRef = details.externalRef,
        mediaType = details.mediaType,
        title = details.title,
        posterUrl = details.posterUrl,
        progress = ready.progress,
        nextEpisode = position,
        updatedAt = null,
        isAbandoned = isAbandoned,
        inLibrary = isInLibrary
    )
    val earlierUnwatched = position != null && regular.any { season ->
        season.episodes.any {
            it.watchState == EpisodeWatchState.Unwatched &&
                (
                    it.episode.seasonNumber < position.seasonNumber ||
                        (
                            it.episode.seasonNumber == position.seasonNumber &&
                                it.episode.episodeNumber < position.episodeNumber
                            )
                    )
        }
    }
    val next = if (position != null && !earlierUnwatched && ContinueWatchingPolicy.isContinueWatching(candidate)) {
        regular.firstOrNull { it.season.seasonNumber == position.seasonNumber }
            ?.episodes?.firstOrNull {
                it.episode.episodeNumber == position.episodeNumber && it.watchState == EpisodeWatchState.Unwatched
            }
    } else {
        null
    }
    return DetailSeriesSummary(
        label = label,
        incompleteMetadata = regular.isEmpty() || regular.any { !it.hasSufficientEpisodeCoverage() },
        nextEpisode = next,
        canEditCompletionDate = ready.progress.isComplete && projected?.isComplete == true && seriesCompletedAt != null
    )
}

/** Offset within tvSeriesItems: one section heading followed by regular season headers and expanded rows. */
internal fun seriesEpisodeItemOffset(state: SeriesDetailUiState, target: ExternalMediaRef): Int? {
    val ready = state.content as? SeriesContentState.Ready ?: return null
    var offset = 1
    for (season in ready.seasons.filter { it.season.seasonNumber > 0 }) {
        offset++
        if (season.season.externalRef in state.expandedSeasons) {
            val episodeIndex = season.episodes.indexOfFirst { it.episode.externalRef == target }
            if (episodeIndex >= 0) return offset + episodeIndex
            offset += season.episodes.size
        }
    }
    return null
}
