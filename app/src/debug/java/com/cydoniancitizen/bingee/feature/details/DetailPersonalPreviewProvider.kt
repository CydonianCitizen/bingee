package com.cydoniancitizen.bingee.feature.details

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.cydoniancitizen.bingee.core.model.EpisodePosition
import com.cydoniancitizen.bingee.core.model.EpisodeWatchState
import com.cydoniancitizen.bingee.core.model.LibraryProgress
import com.cydoniancitizen.bingee.core.model.MovieWatchState
import com.cydoniancitizen.bingee.core.model.PersonalRating
import com.cydoniancitizen.bingee.core.model.deriveSeasonProgress
import com.cydoniancitizen.bingee.core.model.deriveSeriesProgress
import com.cydoniancitizen.bingee.debug.FakeMediaData
import java.time.LocalDate

internal class DetailPersonalPreviewProvider : PreviewParameterProvider<MediaDetailsUiState> {
    override val values: Sequence<MediaDetailsUiState>
        get() {
            val movie = MediaDetailsUiState(
                today = LocalDate.of(2026, 8, 18),
                content = DetailContentState.Content(
                    FakeMediaData.freshMovieDetails.let {
                        it.copy(details = it.details.copy(posterUrl = null, backdropUrl = null))
                    }
                ),
                isInLibrary = true,
                rating = DetailRatingState.Ready(null),
                movieProgress = MovieProgressState.Ready(MovieWatchState.Unwatched)
            )
            val seasons = FakeMediaData.previewSeasons.filter { it.season.seasonNumber == 1 }
            val progress = deriveSeriesProgress(seasons)
            val watching = movie.copy(
                content = DetailContentState.Content(FakeMediaData.staleSeriesDetails),
                movieProgress = MovieProgressState.NotApplicable,
                rating = DetailRatingState.Ready(PersonalRating(7), selectedValue = 7),
                libraryProgress = LibraryProgress.Series(progress.copy(nextEpisode = EpisodePosition(1, 2))),
                series = SeriesDetailUiState(content = SeriesContentState.Ready(seasons, progress))
            )
            val completedSeasons = seasons.map { season ->
                val episodes = season.episodes.map {
                    if (it.watchState == EpisodeWatchState.Unavailable) {
                        it
                    } else {
                        it.copy(watchState = EpisodeWatchState.Watched(FakeMediaData.fixedNow))
                    }
                }
                season.copy(episodes = episodes, progress = deriveSeasonProgress(episodes))
            }
            val completedProgress = deriveSeriesProgress(completedSeasons)
            val caughtUp = watching.copy(
                libraryProgress = LibraryProgress.Series(completedProgress),
                seriesCompletedAt = FakeMediaData.fixedNow,
                series = SeriesDetailUiState(content = SeriesContentState.Ready(completedSeasons, completedProgress))
            )
            val partialSeasons = completedSeasons.map {
                it.copy(season = it.season.copy(episodeCount = it.season.episodeCount + 1))
            }
            val partialProgress = deriveSeriesProgress(partialSeasons)
            return sequenceOf(
                movie,
                movie.copy(
                    movieProgress = MovieProgressState.Ready(MovieWatchState.Watched(FakeMediaData.fixedNow)),
                    rating = DetailRatingState.Ready(PersonalRating(8), selectedValue = 8)
                ),
                watching,
                caughtUp,
                watching.copy(isAbandoned = true),
                watching.copy(isInLibrary = false),
                caughtUp.copy(
                    libraryProgress = LibraryProgress.Series(partialProgress),
                    series = SeriesDetailUiState(content = SeriesContentState.Ready(partialSeasons, partialProgress))
                )
            )
        }
}
