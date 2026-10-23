package com.cydoniancitizen.bingee.feature.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cydoniancitizen.bingee.R
import com.cydoniancitizen.bingee.core.designsystem.component.ErrorState
import com.cydoniancitizen.bingee.core.designsystem.theme.BingeeDimensions
import com.cydoniancitizen.bingee.core.model.EpisodeWatchState
import com.cydoniancitizen.bingee.core.model.MediaType
import com.cydoniancitizen.bingee.core.model.TrackedEpisode
import com.cydoniancitizen.bingee.core.model.WatchedDateChoice
import com.cydoniancitizen.bingee.core.ui.formatLocalized
import com.cydoniancitizen.bingee.core.ui.toUiError
import java.time.Instant
import java.time.LocalDate

@Composable
internal fun EpisodeDetailsScreen(episodeId: String, onBack: () -> Unit, viewModel: MediaDetailsViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    EpisodeDetailsContent(
        state,
        episodeId,
        onBack,
        viewModel::toggleEpisode,
        viewModel::setEpisodeWatchedDate,
        viewModel::markPreviousEpisodesWatched,
        viewModel::dismissPreviousEpisodesPrompt
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EpisodeDetailsContent(
    state: MediaDetailsUiState,
    episodeId: String,
    onBack: () -> Unit,
    onToggleEpisode: (TrackedEpisode) -> Unit,
    onSetWatchedDate: (TrackedEpisode, LocalDate) -> Unit,
    onMarkPreviousEpisodes: () -> Unit = {},
    onDismissPreviousEpisodes: () -> Unit = {}
) {
    val series = state.series.content
    val tracked = (series as? SeriesContentState.Ready)?.seasons
        ?.asSequence()?.flatMap { it.episodes.asSequence() }
        ?.firstOrNull { it.episode.externalRef.externalId == episodeId }
    var editingAt by rememberSaveable(episodeId) { mutableStateOf<String?>(null) }
    LaunchedEffect(tracked?.watchState) {
        if (series is SeriesContentState.Ready && tracked?.watchState !is EpisodeWatchState.Watched) editingAt = null
    }
    if (state.series.previousEpisodesPrompt != null) {
        PreviousEpisodesDialog(onMarkPreviousEpisodes, onDismissPreviousEpisodes)
    }
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.episode_details_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.detail_back))
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .padding(BingeeDimensions.screenPadding),
            verticalArrangement = Arrangement.spacedBy(BingeeDimensions.contentSpacing)
        ) {
            if (tracked == null) {
                if (series == SeriesContentState.Loading) {
                    CircularProgressIndicator()
                } else {
                    ErrorState(
                        stringResource(R.string.detail_error_title),
                        stringResource(
                            (series as? SeriesContentState.Error)?.error?.toUiError()?.messageRes
                                ?: R.string.error_missing_data
                        )
                    )
                }
                return@Column
            }
            val episode = tracked.episode
            val watched = tracked.watchState as? EpisodeWatchState.Watched
            val hidden = state.series.hideSpoilers && watched == null
            val pending = episode.externalRef in state.series.pendingEpisodes ||
                episode.seasonRef in state.series.pendingSeasons
            val locale = LocalConfiguration.current.locales[0]
            Text(
                stringResource(
                    R.string.notification_detail_episode_number,
                    episode.seasonNumber,
                    episode.episodeNumber
                ),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                if (hidden) {
                    stringResource(
                        R.string.detail_episode_title_hidden,
                        episode.episodeNumber
                    )
                } else {
                    episode.title
                },
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                stringResource(
                    when (tracked.watchState) {
                        is EpisodeWatchState.Watched -> R.string.detail_episode_watched_state
                        EpisodeWatchState.Unavailable -> R.string.detail_episode_future
                        EpisodeWatchState.Unwatched -> R.string.detail_episode_unwatched_state
                    }
                )
            )
            Text(
                episode.airDate?.let { stringResource(R.string.detail_episode_air_date, it.formatLocalized(locale)) }
                    ?: stringResource(R.string.detail_episode_air_date_unknown)
            )
            episode.runtime?.let { Text(stringResource(R.string.detail_minutes, it.toMinutes())) }
            if (!hidden) episode.overview?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
            Button(
                onClick = { onToggleEpisode(tracked) },
                enabled = !pending && tracked.watchState != EpisodeWatchState.Unavailable,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(
                        if (watched ==
                            null
                        ) {
                            R.string.detail_mark_episode_watched
                        } else {
                            R.string.detail_mark_episode_unwatched
                        }
                    )
                )
            }
            if (watched != null) {
                Text(stringResource(R.string.watched_date_label), style = MaterialTheme.typography.titleLarge)
                Text(watched.watchedAt.atZone(state.zoneId).toLocalDate().formatLocalized(locale))
                FilledTonalButton(onClick = { editingAt = watched.watchedAt.toString() }, enabled = !pending) {
                    Text(stringResource(R.string.watched_date_edit))
                }
                editingAt?.let { original ->
                    WatchedDateDialog(
                        currentDate = Instant.parse(original).atZone(state.zoneId).toLocalDate(),
                        today = state.today,
                        releaseDate = episode.airDate,
                        mediaType = MediaType.MOVIE,
                        onConfirm = { date ->
                            onSetWatchedDate(
                                tracked.copy(watchState = EpisodeWatchState.Watched(Instant.parse(original))),
                                date
                            )
                            editingAt = null
                        },
                        onDismiss = { editingAt = null },
                        initialChoice = WatchedDateChoice.CUSTOM_DATE
                    )
                }
            }
            if (pending) Text(stringResource(R.string.detail_progress_updating))
            state.progressError?.let {
                Text(
                    stringResource(it.toUiError().messageRes),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                )
            }
        }
    }
}
