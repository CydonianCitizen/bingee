package com.cydoniancitizen.bingee.feature.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cydoniancitizen.bingee.R
import com.cydoniancitizen.bingee.core.designsystem.component.MediaPoster
import com.cydoniancitizen.bingee.core.designsystem.theme.BingeeDimensions
import com.cydoniancitizen.bingee.core.model.ExternalMediaRef
import com.cydoniancitizen.bingee.core.model.HistoryFilter
import com.cydoniancitizen.bingee.core.model.HistoryItem
import com.cydoniancitizen.bingee.core.model.MediaType
import com.cydoniancitizen.bingee.core.ui.formatLocalized
import com.cydoniancitizen.bingee.core.ui.toUiError
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
internal fun HistoryScreen(
    onBack: () -> Unit,
    onOpenDetails: (ExternalMediaRef, MediaType) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryContent(
        state.copy(anchorKey = viewModel.anchorKey, anchorOffset = viewModel.anchorOffset), onBack, onOpenDetails,
        onFilter = viewModel::selectFilter, onRetry = viewModel::retry, onLoadWindow = viewModel::loadWindow,
        onToggleGroup = viewModel::toggleGroup, onGroupPage = viewModel::navigateGroup,
        onGroupRetry = viewModel::retryGroup, onAnchor = viewModel::saveAnchor
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HistoryContent(
    state: HistoryUiState,
    onBack: () -> Unit,
    onOpenDetails: (ExternalMediaRef, MediaType) -> Unit,
    modifier: Modifier = Modifier,
    onFilter: (HistoryFilter) -> Unit = {},
    onRetry: () -> Unit = {},
    onLoadWindow: (Boolean) -> Unit = {},
    onToggleGroup: (HistoryItem) -> Unit = {},
    onGroupPage: (HistoryItem, Boolean) -> Unit = { _, _ -> },
    onGroupRetry: (HistoryItem) -> Unit = {},
    onAnchor: (String?, Int) -> Unit = { _, _ -> }
) {
    val configuration = LocalConfiguration.current
    val locale = configuration.locales[0]
    val pointWidth = (configuration.screenWidthDp - 64).coerceIn(224, 320).dp
    var information by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val currentState by rememberUpdatedState(state)
    val currentAnchorCallback by rememberUpdatedState(onAnchor)
    val currentLoadCallback by rememberUpdatedState(onLoadWindow)
    var positioned by rememberSaveable(state.resetToken) { mutableStateOf(false) }
    LaunchedEffect(state.loading, state.resetToken) {
        if (!state.loading && !positioned && state.page.items.isNotEmpty()) {
            val index = state.page.items.indexOfFirst { it.key == state.anchorKey }.coerceAtLeast(0)
            listState.scrollToItem(index, state.anchorOffset)
            positioned = true
        }
    }
    LaunchedEffect(listState) {
        snapshotFlow {
            val latest = currentState
            val layout = listState.layoutInfo
            val first = layout.visibleItemsInfo.firstOrNull()
            val last = layout.visibleItemsInfo.lastOrNull()
            val matches = layout.totalItemsCount == latest.page.items.size &&
                listOfNotNull(first, last).all { latest.page.items.getOrNull(it.index)?.key == it.key }
            if (!positioned || latest.loading || latest.loadingMore || !matches) {
                null
            } else {
                Triple(
                    first?.let { it.key.toString() to -it.offset },
                    first?.index to last?.index,
                    Triple(latest.page.hasPrevious, latest.page.hasNext, latest.page.items.size)
                )
            }
        }.distinctUntilChanged().collect { viewport ->
            val (anchor, range, available) = viewport ?: return@collect
            val (first, last) = range
            val (previous, next, count) = available
            anchor?.let { currentAnchorCallback(it.first, it.second.coerceAtLeast(0)) }
            when {
                first != null && first < 5 && previous -> currentLoadCallback(true)
                last != null && last >= count - 5 && next -> currentLoadCallback(false)
            }
        }
    }
    Scaffold(modifier = modifier.fillMaxSize(), topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.history_title)) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.detail_back))
                }
            },
            actions = {
                IconButton(onClick = { information = true }) {
                    Icon(Icons.Outlined.Info, stringResource(R.string.history_information))
                }
            },
            windowInsets = WindowInsets(0, 0, 0, 0)
        )
    }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            FlowRow(
                Modifier.padding(horizontal = BingeeDimensions.screenPadding),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HistoryFilter.entries.forEach { filter ->
                    val label = when (filter) {
                        HistoryFilter.ALL -> R.string.history_all
                        HistoryFilter.MOVIES -> R.string.history_movies
                        HistoryFilter.EPISODES -> R.string.history_episodes
                    }
                    FilterChip(selected = state.filter == filter, onClick = { onFilter(filter) }, label = {
                        Text(stringResource(label))
                    })
                }
            }
            if (state.error != null) {
                Column(Modifier.padding(horizontal = BingeeDimensions.screenPadding)) {
                    Text(stringResource(state.error.toUiError().messageRes), color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
                }
            }
            Box(Modifier.fillMaxWidth().height(4.dp)) {
                if (state.loadingMore) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            when {
                state.loading -> Box(
                    Modifier.fillMaxWidth().weight(1f).testTag("history-loading"),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
                state.page.items.isEmpty() && state.error == null -> Box(
                    Modifier.fillMaxWidth().weight(1f).padding(BingeeDimensions.screenPadding),
                    contentAlignment = Alignment.Center
                ) { Text(stringResource(R.string.history_empty), style = MaterialTheme.typography.bodyLarge) }
                else -> LazyRow(
                    Modifier.weight(1f).testTag("history-list"),
                    state = listState,
                    contentPadding = PaddingValues(horizontal = BingeeDimensions.screenPadding, vertical = 16.dp)
                ) {
                    items(state.page.items, key = { it.key }) { row ->
                        Column(Modifier.width(pointWidth).fillMaxHeight()) {
                            Text(
                                row.day.formatLocalized(locale),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 12.dp)
                                    .semantics { heading() }
                            )
                            HistoryRail(
                                start = state.page.hasPrevious || row.key != state.page.items.first().key,
                                end = state.page.hasNext || row.key != state.page.items.last().key
                            )
                            Surface(
                                Modifier.padding(horizontal = 8.dp).fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surfaceContainer
                            ) {
                                Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
                                    HistoryRowContent(
                                        row,
                                        state.groups[row.key],
                                        onOpenDetails,
                                        onToggleGroup,
                                        onGroupPage,
                                        onGroupRetry
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (information) {
        AlertDialog(
            onDismissRequest = { information = false },
            title = { Text(stringResource(R.string.history_information)) },
            text = {
                Text(
                    stringResource(R.string.history_information_body),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                )
            },
            confirmButton = {
                TextButton(onClick = { information = false }) { Text(stringResource(R.string.action_dismiss)) }
            }
        )
    }
}

@Composable
private fun HistoryRail(start: Boolean, end: Boolean) {
    val line = MaterialTheme.colorScheme.outlineVariant
    val point = MaterialTheme.colorScheme.primary
    Canvas(Modifier.fillMaxWidth().height(32.dp)) {
        val y = 10.dp.toPx()
        drawLine(
            line,
            Offset(if (start) 0f else size.width / 2, y),
            Offset(if (end) size.width else size.width / 2, y),
            2.dp.toPx()
        )
        drawLine(line, Offset(size.width / 2, y), Offset(size.width / 2, size.height), 2.dp.toPx())
        drawCircle(point, 6.dp.toPx(), Offset(size.width / 2, y))
    }
}

@Composable
private fun HistoryRowContent(
    row: HistoryItem,
    group: HistoryGroupState?,
    onOpenDetails: (ExternalMediaRef, MediaType) -> Unit,
    onToggleGroup: (HistoryItem) -> Unit,
    onGroupPage: (HistoryItem, Boolean) -> Unit,
    onGroupRetry: (HistoryItem) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().clickable(enabled = row.detailsRef != null) {
                row.detailsRef?.let { onOpenDetails(it, row.mediaType) }
            }.padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MediaPoster(row.title, row.posterUrl, width = 72.dp, height = 108.dp, contentDescription = null)
            Text(row.title, style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(
                    if (row.mediaType ==
                        MediaType.MOVIE
                    ) {
                        R.string.history_movie
                    } else {
                        R.string.history_series
                    }
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (row.mediaType == MediaType.SERIES) {
            val description = stringResource(
                if (group ==
                    null
                ) {
                    R.string.history_collapsed
                } else {
                    R.string.history_expanded
                }
            )
            TextButton(
                onClick = {
                    onToggleGroup(row)
                },
                modifier = Modifier.testTag("history-group-${row.key}").semantics {
                    stateDescription = description
                }
            ) { Text(pluralStringResource(R.plurals.history_episode_count, row.episodeCount, row.episodeCount)) }
            if (group != null) {
                when {
                    group.loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
                    group.error != null -> {
                        Text(
                            stringResource(group.error.toUiError().messageRes),
                            color = MaterialTheme.colorScheme.error
                        )
                        TextButton(onClick = { onGroupRetry(row) }) { Text(stringResource(R.string.action_retry)) }
                    }
                    else -> {
                        group.page.items.forEach { episode ->
                            val number = if (episode.season ==
                                0
                            ) {
                                stringResource(R.string.history_special, episode.number)
                            } else {
                                stringResource(R.string.history_episode_number, episode.season, episode.number)
                            }
                            TextButton(
                                onClick = { row.detailsRef?.let { onOpenDetails(it, row.mediaType) } },
                                shape = MaterialTheme.shapes.small,
                                enabled = row.detailsRef != null,
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("$number \u00b7 ${episode.title}", modifier = Modifier.fillMaxWidth()) }
                        }
                        HistoryEpisodePagination(group.page.hasPrevious, group.page.hasNext) { onGroupPage(row, it) }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryEpisodePagination(previous: Boolean, next: Boolean, onPage: (Boolean) -> Unit) {
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        if (previous) {
            TextButton(onClick = {
                onPage(true)
            }) { Text(stringResource(R.string.history_previous_episodes)) }
        }
        if (next) TextButton(onClick = { onPage(false) }) { Text(stringResource(R.string.history_next_episodes)) }
    }
}
