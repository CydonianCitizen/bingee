package com.cydoniancitizen.bingee.feature.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.cydoniancitizen.bingee.R
import com.cydoniancitizen.bingee.core.designsystem.theme.BingeeDimensions
import com.cydoniancitizen.bingee.core.model.MediaType
import com.cydoniancitizen.bingee.core.model.MovieWatchState
import com.cydoniancitizen.bingee.core.model.TrackedEpisode
import com.cydoniancitizen.bingee.core.ui.toUiError
import java.time.LocalDate
import java.time.ZoneId

@Composable
internal fun DetailPersonalSummary(
    state: MediaDetailsUiState,
    onToggleLibrary: () -> Unit,
    onToggleMovieWatched: () -> Unit,
    onToggleSeriesAbandoned: () -> Unit,
    onSetWatchedDate: (LocalDate?) -> Unit,
    onGoToEpisode: (TrackedEpisode) -> Unit,
    onSelectRating: (Int) -> Unit,
    onSaveRating: () -> Unit,
    onRemoveRating: () -> Unit,
    onDismissRatingError: () -> Unit,
    onRetryMembership: () -> Unit,
    modifier: Modifier = Modifier
) {
    val details = (state.content as? DetailContentState.Content)?.cached?.details ?: return
    val updating = state.libraryAction == DetailLibraryActionState.UPDATING
    val summary = state.seriesPersonalSummary()
    val movieWatched = (state.movieProgress as? MovieProgressState.Ready)?.state as? MovieWatchState.Watched
    val effectiveDate = state.watchedDate ?: movieWatched?.let {
        it.watchedDate ?: it.watchedAt.atZone(ZoneId.systemDefault()).toLocalDate()
    } ?: state.seriesCompletedAt?.atZone(ZoneId.systemDefault())?.toLocalDate()
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(BingeeDimensions.elementSpacing)) {
        state.membershipError?.let { error ->
            Text(stringResource(error.toUiError().messageRes), color = MaterialTheme.colorScheme.error)
            TextButton(onClick = onRetryMembership) { Text(stringResource(R.string.action_retry)) }
        }
        when (state.isInLibrary) {
            null -> if (state.membershipError == null) Text(stringResource(R.string.library_action_updating))
            false -> Button(onClick = onToggleLibrary, enabled = !updating, modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(if (updating) R.string.library_action_updating else R.string.library_action_add),
                    textAlign = TextAlign.Center
                )
            }
            true -> FilledTonalButton(
                onClick = onToggleLibrary,
                enabled = !updating,
                modifier = Modifier.fillMaxWidth()
            ) {
                val removalLabel = if (updating) {
                    R.string.library_action_updating
                } else {
                    R.string.library_action_remove
                }
                Text(stringResource(removalLabel), textAlign = TextAlign.Center)
            }
        }
        if (details.mediaType == MediaType.MOVIE) {
            MovieProgressSection(state.movieProgress, onToggleMovieWatched)
        } else {
            if (summary != null) {
                val label = when (summary.label) {
                    DetailSeriesLabel.OUTSIDE_COLLECTION -> R.string.detail_tracking_outside
                    DetailSeriesLabel.WATCH_LATER -> R.string.detail_tracking_watch_later
                    DetailSeriesLabel.WATCHING -> R.string.detail_tracking_watching
                    DetailSeriesLabel.WATCHED -> R.string.detail_tracking_watched
                    DetailSeriesLabel.CAUGHT_UP -> R.string.detail_tracking_caught_up
                    DetailSeriesLabel.AVAILABLE_WATCHED -> R.string.detail_tracking_available_watched
                    DetailSeriesLabel.ABANDONED -> R.string.detail_tracking_abandoned
                }
                Text(
                    stringResource(label),
                    Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleLarge
                )
                val ready = state.series.content as SeriesContentState.Ready
                if (ready.progress.trackableEpisodes > 0) {
                    val progressText = stringResource(
                        R.string.detail_series_progress,
                        ready.progress.watchedEpisodes,
                        ready.progress.trackableEpisodes,
                        ready.progress.completedSeasons,
                        ready.progress.trackableSeasons
                    )
                    Text(
                        if (summary.incompleteMetadata) {
                            stringResource(R.string.detail_series_local_progress, progressText)
                        } else {
                            progressText
                        }
                    )
                } else {
                    Text(stringResource(R.string.library_progress_unavailable))
                }
                if (summary.incompleteMetadata) {
                    Text(
                        stringResource(R.string.detail_tracking_incomplete),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                summary.nextEpisode?.let { next ->
                    val episode = next.episode
                    val nextLabel = if (state.series.hideSpoilers) {
                        stringResource(
                            R.string.detail_next_episode_position,
                            episode.seasonNumber,
                            episode.episodeNumber
                        )
                    } else {
                        stringResource(
                            R.string.detail_next_episode_title,
                            episode.seasonNumber,
                            episode.episodeNumber,
                            episode.title
                        )
                    }
                    Text(nextLabel, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Button(
                        onClick = { onGoToEpisode(next) },
                        enabled = episode.externalRef !in state.series.pendingEpisodes
                    ) {
                        Text(stringResource(R.string.detail_go_to_episode))
                    }
                }
            } else {
                when (val content = state.series.content) {
                    SeriesContentState.Loading -> Text(stringResource(R.string.detail_seasons_loading))
                    is SeriesContentState.Error -> Text(
                        stringResource(content.error.toUiError().messageRes),
                        color = MaterialTheme.colorScheme.error
                    )
                    is SeriesContentState.Ready -> if (state.isInLibrary != null && state.membershipError == null) {
                        Text(stringResource(R.string.library_action_updating))
                    }
                    else -> Unit
                }
            }
            if (state.isInLibrary == true) {
                if (state.isAbandoned) {
                    Button(onClick = onToggleSeriesAbandoned, enabled = !updating) {
                        Text(stringResource(R.string.series_tracking_restore))
                    }
                } else {
                    TextButton(onClick = onToggleSeriesAbandoned, enabled = !updating) {
                        Text(stringResource(R.string.series_tracking_abandon))
                    }
                }
            }
        }
        if (movieWatched != null || summary?.canEditCompletionDate == true) {
            WatchedDateSection(
                effectiveDate,
                state.today,
                state.watchedDateUpdating,
                details.releaseDate,
                details.mediaType,
                onSetWatchedDate,
                canClear = state.watchedDate != null,
                isFinalCompletion = summary?.label == DetailSeriesLabel.WATCHED
            )
        }
        RatingSection(state.rating, onSelectRating, onSaveRating, onRemoveRating, onDismissRatingError)
    }
}
