package com.cydoniancitizen.bingee.feature.details

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cydoniancitizen.bingee.R
import com.cydoniancitizen.bingee.core.designsystem.component.ErrorState
import com.cydoniancitizen.bingee.core.designsystem.component.LoadingState
import com.cydoniancitizen.bingee.core.designsystem.component.OfflineBanner
import com.cydoniancitizen.bingee.core.designsystem.theme.BingeeDimensions
import com.cydoniancitizen.bingee.core.model.CacheFreshness
import com.cydoniancitizen.bingee.core.model.MediaDetails
import com.cydoniancitizen.bingee.core.model.MediaType
import com.cydoniancitizen.bingee.core.model.ProductionStatus
import com.cydoniancitizen.bingee.core.model.WatchedDateChoice
import com.cydoniancitizen.bingee.core.model.WatchedDateValidationResult
import com.cydoniancitizen.bingee.core.model.isValid
import com.cydoniancitizen.bingee.core.model.validateWatchedDate
import com.cydoniancitizen.bingee.core.result.AppError
import com.cydoniancitizen.bingee.core.ui.formatLocalized
import com.cydoniancitizen.bingee.core.ui.toUiError
import java.time.LocalDate
import kotlin.math.roundToInt

/** Scroll distance over which the transparent top app bar fades into an opaque one. */
private val BarCollapseDistance = 160.dp

/**
 * Steps used to quantize the top-bar collapse fraction for tint interpolation, so icon tint changes
 * stay visually smooth while recomposing at most this many times during a collapse animation.
 */
private const val TINT_QUANTIZATION_STEPS = 16

@Composable
internal fun MediaDetailsScreen(
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MediaDetailsViewModel = hiltViewModel(),
    onOpenEpisode: (com.cydoniancitizen.bingee.core.model.TrackedEpisode) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    MediaDetailsContent(
        state = state,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        onRetry = viewModel::retry,
        onRetryMembership = viewModel::retryMembership,
        onToggleLibrary = viewModel::toggleLibrary,
        onToggleFavorite = viewModel::toggleFavorite,
        onToggleSeriesAbandoned = viewModel::toggleSeriesAbandoned,
        onSetWatchedDate = viewModel::setWatchedDate,
        onToggleMovieWatched = viewModel::toggleMovieWatched,
        onToggleSeasonExpanded = viewModel::toggleSeasonExpanded,
        onRetrySeason = viewModel::retrySeason,
        onToggleEpisode = viewModel::toggleEpisode,
        onMarkPreviousEpisodes = viewModel::markPreviousEpisodesWatched,
        onDismissPreviousEpisodes = viewModel::dismissPreviousEpisodesPrompt,
        onToggleSeasonWatched = viewModel::toggleSeasonWatched,
        onSelectRating = viewModel::selectRating,
        onSaveRating = viewModel::setRating,
        onRemoveRating = viewModel::removeRating,
        onDismissRatingError = viewModel::dismissRatingError,
        onDismissLibraryError = viewModel::dismissLibraryError,
        onDismissProgressError = viewModel::dismissProgressError,
        onOpenSettings = onOpenSettings,
        onOpenEpisode = onOpenEpisode,
        modifier = modifier
    )
}

@Composable
internal fun MediaDetailsContent(
    state: MediaDetailsUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onToggleLibrary: () -> Unit,
    onDismissLibraryError: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    onToggleFavorite: () -> Unit = {},
    onToggleSeriesAbandoned: () -> Unit = {},
    onSetWatchedDate: (LocalDate?) -> Unit = {},
    onToggleMovieWatched: () -> Unit = {},
    onToggleSeasonExpanded: (com.cydoniancitizen.bingee.core.model.CachedSeason) -> Unit = {},
    onRetrySeason: (com.cydoniancitizen.bingee.core.model.CachedSeason) -> Unit = {},
    onToggleEpisode: (com.cydoniancitizen.bingee.core.model.TrackedEpisode) -> Unit = {},
    onMarkPreviousEpisodes: () -> Unit = {},
    onDismissPreviousEpisodes: () -> Unit = {},
    onToggleSeasonWatched: (com.cydoniancitizen.bingee.core.model.CachedSeason) -> Unit = {},
    onSelectRating: (Int) -> Unit = {},
    onSaveRating: () -> Unit = {},
    onRemoveRating: () -> Unit = {},
    onDismissRatingError: () -> Unit = {},
    onDismissProgressError: () -> Unit = {},
    onRetryMembership: () -> Unit = {},
    onOpenEpisode: (com.cydoniancitizen.bingee.core.model.TrackedEpisode) -> Unit = {}
) {
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val content = state.content
    val details = (content as? DetailContentState.Content)?.cached?.details
    val collapseThresholdPx = with(LocalDensity.current) { BarCollapseDistance.toPx() }
    // Kept as State<Float> and handed down so the top bar can read it in the draw phase and in a
    // quantized derived state instead of recomposing on every scroll pixel.
    val collapseFractionState = remember(details, collapseThresholdPx) {
        derivedStateOf {
            when {
                // Loading and error states have no artwork behind the bar, so it starts opaque.
                details == null -> 1f
                listState.firstVisibleItemIndex > 0 -> 1f
                else -> (listState.firstVisibleItemScrollOffset / collapseThresholdPx).coerceIn(0f, 1f)
            }
        }
    }

    val libraryErrorText = state.libraryError?.let { stringResource(it.toUiError().messageRes) }
    LaunchedEffect(libraryErrorText) {
        if (libraryErrorText != null) {
            snackbarHostState.showSnackbar(libraryErrorText)
            onDismissLibraryError()
        }
    }
    val progressErrorText = state.progressError?.let { stringResource(it.toUiError().messageRes) }
    LaunchedEffect(progressErrorText) {
        if (progressErrorText != null) {
            snackbarHostState.showSnackbar(progressErrorText)
            onDismissProgressError()
        }
    }
    if (state.series.previousEpisodesPrompt != null) {
        PreviousEpisodesDialog(onConfirm = onMarkPreviousEpisodes, onDismiss = onDismissPreviousEpisodes)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        // The app shell already applies the status bar inset to the nav host.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            DetailTopBar(
                title = details?.title ?: stringResource(R.string.detail_screen_title),
                collapseFraction = collapseFractionState,
                state = state,
                onBack = onBack,
                onRefresh = onRefresh,
                onToggleFavorite = onToggleFavorite
            )
        }
    ) { innerPadding ->
        when (content) {
            DetailContentState.Resolving,
            DetailContentState.Loading -> LoadingState(
                message = stringResource(R.string.detail_loading),
                modifier = Modifier.padding(innerPadding)
            )
            is DetailContentState.Error -> FullDetailError(
                error = content.error,
                onRetry = onRetry,
                onOpenSettings = onOpenSettings,
                modifier = Modifier.padding(innerPadding)
            )
            is DetailContentState.Content -> DetailBody(
                listState = listState,
                // The hero scrolls underneath the transparent bar, so the body drops the top inset
                // and keeps only the bottom one.
                contentPadding = PaddingValues(
                    bottom = innerPadding.calculateBottomPadding() + BingeeDimensions.screenPadding
                ),
                details = content.cached.details,
                isStale = content.cached.freshness == CacheFreshness.STALE,
                refreshError = (state.refresh as? DetailRefreshState.Error)?.error,
                state = state,
                onSetWatchedDate = onSetWatchedDate,
                onToggleLibrary = onToggleLibrary,
                onRetryMembership = onRetryMembership,
                onToggleSeriesAbandoned = onToggleSeriesAbandoned,
                onToggleMovieWatched = onToggleMovieWatched,
                onToggleSeasonExpanded = onToggleSeasonExpanded,
                onRetrySeason = onRetrySeason,
                onToggleEpisode = onToggleEpisode,
                onOpenEpisode = onOpenEpisode,
                onToggleSeasonWatched = onToggleSeasonWatched,
                onSelectRating = onSelectRating,
                onSaveRating = onSaveRating,
                onRemoveRating = onRemoveRating,
                onDismissRatingError = onDismissRatingError,
                onOpenSettings = onOpenSettings
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailTopBar(
    title: String,
    collapseFraction: State<Float>,
    state: MediaDetailsUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    // The fraction moves on every scroll pixel; reading it here would recompose the whole bar each
    // frame. Icon tints therefore track a quantized fraction, while the bar background reads the
    // raw fraction inside drawBehind, skipping recomposition entirely.
    val tintFraction by remember {
        derivedStateOf {
            (collapseFraction.value * TINT_QUANTIZATION_STEPS).roundToInt() /
                TINT_QUANTIZATION_STEPS.toFloat()
        }
    }
    // Icons start white over the artwork and land on onSurface once the bar is opaque, so they stay
    // legible against a bright backdrop and against the bar's own surface alike.
    val iconTint = lerp(Color.White, scheme.onSurface, tintFraction)
    val favoriteTint = lerp(
        Color.White,
        if (state.isFavorite) scheme.error else scheme.onSurface,
        tintFraction
    )
    TopAppBar(
        title = {
            // Composed only once the bar is opaque: while the hero title is the one on screen, a
            // second node carrying the same text would make the title assertions ambiguous.
            if (tintFraction >= 1f) {
                Text(text = title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.detail_back),
                    tint = iconTint
                )
            }
        },
        actions = {
            IconToggleButton(
                checked = state.isFavorite,
                onCheckedChange = { onToggleFavorite() },
                enabled = !state.favoriteUpdating && state.isInLibrary != null
            ) {
                Icon(
                    imageVector = if (state.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = stringResource(
                        if (state.isFavorite) R.string.favorite_remove else R.string.favorite_add
                    ),
                    tint = favoriteTint
                )
            }
            if (state.refresh == DetailRefreshState.Refreshing) {
                CircularProgressIndicator(modifier = Modifier.padding(BingeeDimensions.elementSpacing))
            } else if (state.content is DetailContentState.Content) {
                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.detail_refresh),
                        tint = iconTint
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = scheme.onSurface
        ),
        modifier = Modifier.drawBehind {
            drawRect(scheme.surface, alpha = collapseFraction.value)
        },
        // The app shell already applies the status bar inset to the nav host.
        windowInsets = WindowInsets(0, 0, 0, 0)
    )
}

@Composable
private fun FullDetailError(
    error: AppError,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiError = error.toUiError()
    Column(
        modifier = modifier.fillMaxWidth().padding(BingeeDimensions.screenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(BingeeDimensions.elementSpacing)
    ) {
        ErrorState(
            title = stringResource(R.string.detail_error_title),
            message = stringResource(uiError.messageRes),
            retryLabel = if (uiError.canRetry) stringResource(R.string.action_retry) else null,
            onRetry = if (uiError.canRetry) onRetry else null
        )
        if (error == AppError.Unauthorized) {
            Button(onClick = onOpenSettings) { Text(stringResource(R.string.search_open_settings)) }
        }
    }
}

@Composable
private fun DetailBody(
    listState: LazyListState,
    contentPadding: PaddingValues,
    details: MediaDetails,
    isStale: Boolean,
    refreshError: AppError?,
    state: MediaDetailsUiState,
    onSetWatchedDate: (LocalDate?) -> Unit,
    onToggleLibrary: () -> Unit,
    onRetryMembership: () -> Unit,
    onToggleSeriesAbandoned: () -> Unit,
    onToggleMovieWatched: () -> Unit,
    onToggleSeasonExpanded: (com.cydoniancitizen.bingee.core.model.CachedSeason) -> Unit,
    onRetrySeason: (com.cydoniancitizen.bingee.core.model.CachedSeason) -> Unit,
    onToggleEpisode: (com.cydoniancitizen.bingee.core.model.TrackedEpisode) -> Unit,
    onOpenEpisode: (com.cydoniancitizen.bingee.core.model.TrackedEpisode) -> Unit,
    onToggleSeasonWatched: (com.cydoniancitizen.bingee.core.model.CachedSeason) -> Unit,
    onSelectRating: (Int) -> Unit,
    onSaveRating: () -> Unit,
    onRemoveRating: () -> Unit,
    onDismissRatingError: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val locale = LocalConfiguration.current.locales[0]
    val sectionModifier = Modifier.fillMaxWidth().padding(horizontal = BingeeDimensions.screenPadding)
    val series = state.series
    var pendingEpisode by remember { mutableStateOf<com.cydoniancitizen.bingee.core.model.TrackedEpisode?>(null) }
    val leadingKeys = listOfNotNull(
        "hero",
        "banner".takeIf { refreshError != null || isStale },
        "personal",
        "overview".takeIf { details.overview != null }
    )
    val nextReference = state.seriesPersonalSummary()?.nextEpisode?.episode?.externalRef
    val toolbarHeight = with(LocalDensity.current) { TopAppBarDefaults.TopAppBarExpandedHeight.roundToPx() }
    LaunchedEffect(pendingEpisode, series.expandedSeasons, series.content, leadingKeys.size, nextReference) {
        val target = pendingEpisode ?: return@LaunchedEffect
        if (target.episode.seriesRef != details.externalRef || target.episode.externalRef != nextReference) {
            pendingEpisode = null
            return@LaunchedEffect
        }
        val offset = seriesEpisodeItemOffset(series, target.episode.externalRef) ?: return@LaunchedEffect
        try {
            listState.animateScrollToItem(leadingKeys.size + offset, scrollOffset = -toolbarHeight)
        } finally {
            // A gesture or data update must not replay an interrupted command.
            if (pendingEpisode == target) pendingEpisode = null
        }
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.Top
    ) {
        leadingKeys.forEach { key ->
            detailSectionItem(key = key) {
                when (key) {
                    "hero" -> DetailHero(details)
                    "banner" -> {
                        val message = if (refreshError != null) {
                            stringResource(
                                R.string.detail_refresh_failed,
                                stringResource(refreshError.toUiError().messageRes)
                            )
                        } else {
                            stringResource(R.string.detail_stale_data)
                        }
                        OfflineBanner(message, modifier = sectionModifier)
                    }
                    "personal" -> DetailPersonalSummary(
                        state, onToggleLibrary, onToggleMovieWatched, onToggleSeriesAbandoned, onSetWatchedDate,
                        onGoToEpisode = { target ->
                            val season = (series.content as? SeriesContentState.Ready)?.seasons
                                ?.firstOrNull { it.season.externalRef == target.episode.seasonRef }
                            if (season != null) {
                                pendingEpisode = target
                                if (season.season.externalRef !in series.expandedSeasons) onToggleSeasonExpanded(season)
                            }
                        },
                        onSelectRating, onSaveRating, onRemoveRating, onDismissRatingError,
                        onRetryMembership, modifier = sectionModifier
                    )
                    "overview" -> Column(
                        modifier = sectionModifier,
                        verticalArrangement = Arrangement.spacedBy(BingeeDimensions.elementSpacing)
                    ) {
                        Text(stringResource(R.string.detail_overview), style = MaterialTheme.typography.titleLarge)
                        Text(requireNotNull(details.overview), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
        if (details.mediaType == MediaType.SERIES) {
            tvSeriesItems(
                series,
                onToggleSeasonExpanded,
                onRetrySeason,
                onToggleEpisode,
                onToggleSeasonWatched,
                onOpenSettings,
                sectionModifier,
                onOpenEpisode = onOpenEpisode
            )
        }
        detailSectionItem(key = "chips") {
            DetailChips(
                stringResource(statusString(details.productionStatus)),
                details.genres,
                modifier = sectionModifier
            )
        }
        detailSectionItem(key = "metadata") {
            Column(
                modifier = sectionModifier,
                verticalArrangement = Arrangement.spacedBy(BingeeDimensions.elementSpacing)
            ) {
                details.episodeRuntime?.let {
                    DetailField(
                        R.string.detail_episode_runtime,
                        stringResource(R.string.detail_minutes, it.toMinutes())
                    )
                }
                details.releaseDate?.let { DetailField(R.string.detail_date, it.formatLocalized(locale)) }
                details.originalLanguage?.let { DetailField(R.string.detail_original_language, it) }
                if (refreshError == AppError.Unauthorized) {
                    Button(onClick = onOpenSettings) {
                        Text(stringResource(R.string.search_open_settings))
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailField(@StringRes labelRes: Int, value: String) {
    Text(stringResource(labelRes), fontWeight = FontWeight.Bold)
    Text(value)
}

private fun statusString(status: ProductionStatus): Int = when (status) {
    ProductionStatus.RUMORED -> R.string.detail_status_rumored
    ProductionStatus.PLANNED -> R.string.detail_status_planned
    ProductionStatus.IN_PRODUCTION -> R.string.detail_status_in_production
    ProductionStatus.POST_PRODUCTION -> R.string.detail_status_post_production
    ProductionStatus.RELEASED -> R.string.detail_status_released
    ProductionStatus.RETURNING_SERIES -> R.string.detail_status_returning
    ProductionStatus.ENDED -> R.string.detail_status_ended
    ProductionStatus.CANCELED -> R.string.detail_status_canceled
    ProductionStatus.PILOT -> R.string.detail_status_pilot
    ProductionStatus.UNKNOWN -> R.string.detail_status_unknown
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WatchedDateSection(
    watchedDate: LocalDate?,
    today: LocalDate,
    isUpdating: Boolean,
    releaseDate: LocalDate?,
    mediaType: MediaType,
    onSetWatchedDate: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
    canClear: Boolean = watchedDate != null,
    isFinalCompletion: Boolean = true
) {
    var showDialog by remember { mutableStateOf(false) }
    val locale = LocalConfiguration.current.locales[0]
    val labelRes = when {
        mediaType == MediaType.MOVIE -> R.string.watched_date_label
        isFinalCompletion -> R.string.completion_date_label
        else -> R.string.available_completion_date_label
    }
    val editRes = when {
        mediaType == MediaType.MOVIE -> R.string.watched_date_edit
        isFinalCompletion -> R.string.completion_date_edit
        else -> R.string.available_completion_date_edit
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(BingeeDimensions.elementSpacing)
    ) {
        Text(
            text = stringResource(labelRes),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleLarge
        )
        if (watchedDate != null) {
            Text(watchedDate.formatLocalized(locale))
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(BingeeDimensions.elementSpacing),
            verticalArrangement = Arrangement.spacedBy(BingeeDimensions.elementSpacing)
        ) {
            FilledTonalButton(
                onClick = { showDialog = true },
                enabled = !isUpdating
            ) {
                Text(stringResource(editRes))
            }
            if (canClear) {
                TextButton(
                    onClick = { onSetWatchedDate(null) },
                    enabled = !isUpdating
                ) {
                    Text(stringResource(R.string.watched_date_clear))
                }
            }
        }
    }
    if (showDialog) {
        WatchedDateDialog(
            currentDate = watchedDate,
            today = today,
            releaseDate = releaseDate,
            mediaType = mediaType,
            onConfirm = { date ->
                onSetWatchedDate(date)
                showDialog = false
            },
            onDismiss = { showDialog = false },
            isFinalCompletion = isFinalCompletion
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WatchedDateDialog(
    currentDate: LocalDate?,
    today: LocalDate,
    releaseDate: LocalDate?,
    mediaType: MediaType = MediaType.MOVIE,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    isFinalCompletion: Boolean = true,
    initialChoice: WatchedDateChoice = WatchedDateChoice.TODAY
) {
    var selectedChoice by rememberSaveable { mutableStateOf(initialChoice) }
    var customDateEpochDay by rememberSaveable { mutableLongStateOf((currentDate ?: today).toEpochDay()) }
    val customDate = LocalDate.ofEpochDay(customDateEpochDay)
    var showCustomDatePicker by rememberSaveable { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }
    val locale = LocalConfiguration.current.locales[0]

    val resolvedDate = when (selectedChoice) {
        WatchedDateChoice.TODAY -> today
        WatchedDateChoice.RELEASE_DATE -> releaseDate ?: today
        WatchedDateChoice.CUSTOM_DATE -> customDate
    }

    val titleRes = when {
        mediaType == MediaType.MOVIE -> R.string.watched_date_label
        isFinalCompletion -> R.string.completion_date_label
        else -> R.string.available_completion_date_label
    }
    val futureDateError = stringResource(R.string.watched_date_future_error, today.formatLocalized(locale))
    val beforeReleaseDateError = releaseDate?.let {
        stringResource(R.string.watched_date_before_release_error, it.formatLocalized(locale))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(titleRes)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(BingeeDimensions.elementSpacing)) {
                WatchedDateChoice.entries.forEach { choice ->
                    if (choice == WatchedDateChoice.RELEASE_DATE && releaseDate == null) return@forEach
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedChoice == choice,
                                onClick = {
                                    selectedChoice = choice
                                    validationError = null
                                    if (choice == WatchedDateChoice.CUSTOM_DATE) {
                                        showCustomDatePicker = true
                                    }
                                },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selectedChoice == choice, onClick = null)
                        Text(
                            text = when (choice) {
                                WatchedDateChoice.TODAY -> stringResource(R.string.watched_date_today)
                                WatchedDateChoice.RELEASE_DATE -> stringResource(
                                    R.string.label_value,
                                    stringResource(R.string.watched_date_release_date),
                                    requireNotNull(releaseDate).formatLocalized(locale)
                                )
                                WatchedDateChoice.CUSTOM_DATE -> stringResource(
                                    R.string.label_value,
                                    stringResource(R.string.watched_date_custom),
                                    customDate.formatLocalized(locale)
                                )
                            },
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                if (selectedChoice == WatchedDateChoice.CUSTOM_DATE) {
                    TextButton(onClick = { showCustomDatePicker = true }) {
                        Text(
                            stringResource(
                                R.string.label_value,
                                stringResource(R.string.watched_date_custom),
                                customDate.formatLocalized(locale)
                            )
                        )
                    }
                }
                validationError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val result = validateWatchedDate(resolvedDate, releaseDate, today)
                if (result.isValid()) {
                    onConfirm(resolvedDate)
                } else {
                    validationError = when (result) {
                        is WatchedDateValidationResult.FutureDateRejected ->
                            futureDateError
                        is WatchedDateValidationResult.DatePrecedesReleaseRejected ->
                            beforeReleaseDateError
                        else -> null
                    }
                }
            }) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )

    if (showCustomDatePicker) {
        val initialEpochMillis = customDate.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        val datePickerState = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = initialEpochMillis
        )
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = { showCustomDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        customDateEpochDay = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneOffset.UTC)
                            .toLocalDate().toEpochDay()
                    }
                    showCustomDatePicker = false
                }) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDatePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        ) {
            androidx.compose.material3.DatePicker(state = datePickerState)
        }
    }
}

/** Offered after an episode is marked watched while earlier ones may still be unwatched. */
@Composable
internal fun PreviousEpisodesDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.detail_mark_previous_title)) },
        text = { Text(stringResource(R.string.detail_mark_previous_body)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.detail_mark_previous_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.detail_mark_previous_dismiss))
            }
        }
    )
}

/** Sections keep their outer gap; expanded season rows share one continuous surface. */
internal fun LazyListScope.detailSectionItem(
    key: String,
    contentType: String = key,
    content: @Composable LazyItemScope.() -> Unit
) {
    item(key = key, contentType = contentType) {
        Box(Modifier.padding(bottom = BingeeDimensions.contentSpacing)) { content() }
    }
}
