package com.cydoniancitizen.bingee.feature.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cydoniancitizen.bingee.core.model.HISTORY_PAGE_SIZE
import com.cydoniancitizen.bingee.core.model.HISTORY_WINDOW_SIZE
import com.cydoniancitizen.bingee.core.model.HistoryContext
import com.cydoniancitizen.bingee.core.model.HistoryCursor
import com.cydoniancitizen.bingee.core.model.HistoryEpisodeCursor
import com.cydoniancitizen.bingee.core.model.HistoryEpisodeQuery
import com.cydoniancitizen.bingee.core.model.HistoryFilter
import com.cydoniancitizen.bingee.core.model.HistoryItem
import com.cydoniancitizen.bingee.core.model.HistoryPageQuery
import com.cydoniancitizen.bingee.core.model.HistorySnapshotQuery
import com.cydoniancitizen.bingee.core.model.MediaType
import com.cydoniancitizen.bingee.core.result.AppResult
import com.cydoniancitizen.bingee.data.importexport.RestoreGeneration
import com.cydoniancitizen.bingee.domain.calendar.CalendarDateSource
import com.cydoniancitizen.bingee.domain.repository.LibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private data class GroupRequest(val cursor: HistoryEpisodeCursor? = null, val backwards: Boolean = false)
private data class HistoryRequest(
    val filter: HistoryFilter,
    val context: HistoryContext,
    val restore: Long,
    val restoring: Boolean = false,
    val cursor: HistoryCursor? = null,
    val backwards: Boolean = false,
    val groups: Map<String, GroupRequest> = emptyMap(),
    val retry: Int = 0
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
internal class HistoryViewModel @Inject constructor(
    private val repository: LibraryRepository,
    dateSource: CalendarDateSource,
    restoreGeneration: RestoreGeneration,
    private val saved: SavedStateHandle
) : ViewModel() {
    private val initialContext = dateSource.currentContext().let { HistoryContext(it.today, it.zoneId) }
    private val savedZone = saved.get<String>("history_zone")
    private val savedToday = saved.get<String>("history_today")
    private val savedContextValid = saved.get<Boolean>("history_horizontal") == true &&
        saved.get<String>("history_filter") in listOf(HistoryFilter.MOVIES.name, HistoryFilter.EPISODES.name) &&
        (savedZone == null || savedZone == initialContext.zoneId.id) &&
        (savedToday == null || savedToday == initialContext.today.toString())
    private val requests = MutableStateFlow(
        HistoryRequest(
            filter = runCatching {
                HistoryFilter.valueOf(saved.get<String>("history_filter").orEmpty())
            }.getOrNull()?.takeIf { it != HistoryFilter.ALL } ?: HistoryFilter.MOVIES,
            context = initialContext,
            restore = saved.get<Long>("history_restore") ?: restoreGeneration.changes.value.generation,
            restoring = restoreGeneration.changes.value.activeRestores > 0,
            cursor = if (savedContextValid) decodeCursor(saved["history_cursor"]) else null,
            backwards = savedContextValid && (saved["history_backwards"] ?: false),
            groups = saved.get<ArrayList<String>>("history_groups").orEmpty().takeIf {
                savedContextValid
            }.orEmpty().take(HISTORY_WINDOW_SIZE).mapNotNull { encoded ->
                runCatching {
                    val parts = encoded.split('|')
                    val cursor = if (parts.size ==
                        5
                    ) {
                        HistoryEpisodeCursor(parts[1].toInt(), parts[2].toInt(), parts[3].toLong())
                    } else {
                        null
                    }
                    parts[0] to GroupRequest(cursor, parts.last() == "true")
                }.getOrNull()
            }.toMap()
        )
    )

    val uiState = requests.flatMapLatest { request ->
        if (request.restoring) flowOf(HistoryUiState(request.filter)) else mainPage(request)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000, 0),
        HistoryUiState(requests.value.filter)
    )

    init {
        if (!savedContextValid) {
            clearAnchor()
            change(requests.value)
        }
        viewModelScope.launch {
            combine(dateSource.observeContext(), restoreGeneration.changes) { date, generation ->
                HistoryContext(date.today, date.zoneId) to generation
            }.collect { (context, identity) ->
                val current = requests.value
                if (current.context != context || current.restore != identity.generation ||
                    current.restoring != (identity.activeRestores > 0)
                ) {
                    clearAnchor()
                    change(
                        current.copy(
                            context = context,
                            restore = identity.generation,
                            restoring = identity.activeRestores > 0,
                            cursor = null,
                            backwards = false,
                            groups = emptyMap()
                        )
                    )
                }
            }
        }
    }

    private fun mainPage(request: HistoryRequest): Flow<HistoryUiState> = flow {
        val token = listOf(request.context, request.restore, request.filter).joinToString("|")
        val previous = uiState.value
        var state = if (previous.resetToken == token) {
            previous.copy(
                loadingMore = true,
                groups = request.groups.mapValues { (key, _) -> previous.groups[key] ?: HistoryGroupState() }
            )
        } else {
            HistoryUiState(
                request.filter,
                resetToken = token,
                anchorKey = saved["history_anchor"],
                anchorOffset = saved["history_offset"] ?: 0
            )
        }
        emit(state)
        val pageQuery =
            HistoryPageQuery(null, request.context, request.filter, request.cursor, request.backwards)
        val groups = request.groups.mapNotNull { (key, group) ->
            runCatching {
                val parts = key.split(':')
                key to HistoryEpisodeQuery(
                    request.context,
                    LocalDate.parse(parts[0]),
                    parts[2].toLong(),
                    group.cursor,
                    group.backwards
                )
            }.getOrNull()
        }.toMap()
        val query = HistorySnapshotQuery(pageQuery, groups, request.restore)
        repository.observeHistory(query).collect { result ->
            if (request != requests.value) return@collect
            when (result) {
                is AppResult.Failure -> {
                    state = state.copy(loading = false, loadingMore = false, error = result.error)
                    emit(state)
                }
                is AppResult.Success -> {
                    val snapshot = result.value
                    val visibleKeys = snapshot.page.items.filter { it.mediaType == MediaType.SERIES }
                        .mapTo(mutableSetOf()) { it.key }
                    val visibleGroups = request.groups.filterKeys { it in visibleKeys }
                    if (visibleGroups != request.groups) {
                        change(request.copy(groups = visibleGroups))
                        return@collect
                    }
                    val emptyCursors = snapshot.groups.filter { (key, result) ->
                        result is AppResult.Success && result.value.items.isEmpty() &&
                            request.groups[key]?.cursor != null
                    }.keys
                    if (emptyCursors.isNotEmpty()) {
                        change(
                            request.copy(
                                groups = request.groups.mapValues { (key, value) ->
                                    if (key in emptyCursors) GroupRequest() else value
                                }
                            )
                        )
                    } else if (snapshot.page.items.isEmpty() && request.cursor != null) {
                        clearAnchor()
                        change(request.copy(cursor = null, backwards = false, groups = emptyMap()))
                    } else {
                        state = state.copy(
                            loading = false,
                            loadingMore = false,
                            page = snapshot.page,
                            overview = snapshot.overview,
                            error = null,
                            anchorKey = saved["history_anchor"],
                            anchorOffset =
                            saved["history_offset"] ?: 0,
                            groups = snapshot.groups.mapValues { (_, result) ->
                                when (result) {
                                    is AppResult.Success -> HistoryGroupState(false, result.value)
                                    is AppResult.Failure -> HistoryGroupState(false, error = result.error)
                                }
                            }
                        )
                        emit(state)
                    }
                }
            }
        }
    }

    fun selectFilter(filter: HistoryFilter) {
        if (filter == HistoryFilter.ALL || filter == requests.value.filter) return
        clearAnchor()
        change(requests.value.copy(filter = filter, cursor = null, backwards = false, groups = emptyMap()))
    }

    fun retry() {
        change(requests.value.copy(retry = requests.value.retry + 1))
    }

    fun loadWindow(backwards: Boolean) {
        if (uiState.value.loading || uiState.value.loadingMore || uiState.value.error != null) return
        val page = uiState.value.page
        if (if (backwards) !page.hasPrevious else !page.hasNext) return
        val index = if (backwards) {
            (HISTORY_WINDOW_SIZE - HISTORY_PAGE_SIZE).coerceAtMost(page.items.lastIndex)
        } else {
            (HISTORY_PAGE_SIZE - 1).coerceAtMost(page.items.lastIndex)
        }
        val cursor = page.items.getOrNull(index)?.cursor ?: return
        if (requests.value.cursor == cursor && requests.value.backwards == backwards) return
        change(requests.value.copy(cursor = cursor, backwards = backwards))
    }

    fun toggleGroup(item: HistoryItem) {
        if (item.mediaType != MediaType.SERIES || uiState.value.page.items.none { it.key == item.key }) return
        val current = requests.value.groups
        change(
            requests.value.copy(
                groups = if (item.key in
                    current
                ) {
                    current - item.key
                } else {
                    current + (item.key to GroupRequest())
                }
            )
        )
    }

    fun navigateGroup(item: HistoryItem, backwards: Boolean) {
        val page = uiState.value.groups[item.key]?.page ?: return
        if (if (backwards) !page.hasPrevious else !page.hasNext) return
        val cursor = (if (backwards) page.items.firstOrNull() else page.items.lastOrNull())?.cursor ?: return
        change(requests.value.copy(groups = requests.value.groups + (item.key to GroupRequest(cursor, backwards))))
    }

    fun retryGroup(item: HistoryItem) {
        if (item.key !in requests.value.groups) return
        change(
            requests.value.copy(
                groups = requests.value.groups + (item.key to GroupRequest()),
                retry =
                requests.value.retry + 1
            )
        )
    }

    fun saveAnchor(key: String?, offset: Int) {
        saved["history_anchor"] = key
        saved["history_offset"] = offset
    }

    val anchorKey: String? get() = saved["history_anchor"]
    val anchorOffset: Int get() = saved["history_offset"] ?: 0

    private fun clearAnchor() {
        saveAnchor(null, 0)
    }

    private fun change(request: HistoryRequest) {
        requests.value = request
        saved["history_horizontal"] = true
        saved["history_zone"] = request.context.zoneId.id
        saved["history_today"] = request.context.today.toString()
        saved["history_filter"] = request.filter.name
        saved["history_restore"] = request.restore
        saved["history_cursor"] = request.cursor?.let { "${it.day}|${it.kind}|${it.mediaId}" }
        saved["history_backwards"] = request.backwards
        saved["history_groups"] = ArrayList(
            request.groups.map { (key, value) ->
                value.cursor?.let { "$key|${it.season}|${it.episode}|${it.id}|${value.backwards}" } ?: "$key|false"
            }
        )
    }

    private fun decodeCursor(value: String?): HistoryCursor? = runCatching {
        val parts = checkNotNull(value).split('|')
        HistoryCursor(LocalDate.parse(parts[0]), parts[1].toInt(), parts[2].toLong())
    }.getOrNull()
}
