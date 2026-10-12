package com.cydoniancitizen.bingee.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cydoniancitizen.bingee.core.model.ContinueWatchingItem
import com.cydoniancitizen.bingee.core.model.ExternalMediaRef
import com.cydoniancitizen.bingee.core.model.LibraryEntry
import com.cydoniancitizen.bingee.core.model.LibraryProgress
import com.cydoniancitizen.bingee.core.model.LibraryQuery
import com.cydoniancitizen.bingee.core.model.MediaType
import com.cydoniancitizen.bingee.core.model.MovieWatchState
import com.cydoniancitizen.bingee.core.model.PersonalViewingEntry
import com.cydoniancitizen.bingee.core.model.SeriesTrackingState
import com.cydoniancitizen.bingee.core.model.isWatched
import com.cydoniancitizen.bingee.core.model.toContinueWatchingItem
import com.cydoniancitizen.bingee.core.result.AppError
import com.cydoniancitizen.bingee.core.result.AppResult
import com.cydoniancitizen.bingee.data.settings.ProfileCategory
import com.cydoniancitizen.bingee.data.settings.ProfileCollection
import com.cydoniancitizen.bingee.data.settings.ProfileDisplayModePreferences
import com.cydoniancitizen.bingee.data.settings.ProfileDisplayModes
import com.cydoniancitizen.bingee.data.settings.ProfileViewMode
import com.cydoniancitizen.bingee.di.DefaultDispatcher
import com.cydoniancitizen.bingee.domain.calendar.CalendarDateSource
import com.cydoniancitizen.bingee.domain.model.StatisticsMediaScope
import com.cydoniancitizen.bingee.domain.model.TasteStatistics
import com.cydoniancitizen.bingee.domain.model.WatchedStatistics
import com.cydoniancitizen.bingee.domain.model.calculateTasteStatistics
import com.cydoniancitizen.bingee.domain.model.calculateWatchedStatistics
import com.cydoniancitizen.bingee.domain.policy.ContinueWatchingPolicy
import com.cydoniancitizen.bingee.domain.repository.LibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class StatisticsUpdate(
    val today: LocalDate,
    val statistics: WatchedStatistics,
    val tasteStatistics: TasteStatistics,
    val error: AppError?
)

enum class ProfileSortOption {
    RECENTLY_ADDED,
    TITLE,
    RATING,
    PROGRESS
}

internal enum class ProfileCollectionShortcut { WATCHING, WATCH_LATER, WATCHED, FAVORITES, ABANDONED }

internal data class ProfileCollectionCounts(
    val watchLater: Int = 0,
    val watched: Int = 0,
    val favorites: Int = 0,
    val abandoned: Int = 0
)

internal data class ProfileUiState(
    val today: LocalDate,
    val collection: ProfileCollection = ProfileCollection.WATCHED,
    val category: ProfileCategory = ProfileCategory.MOVIES,
    val sortOption: ProfileSortOption = ProfileSortOption.RECENTLY_ADDED,
    val displayModes: ProfileDisplayModes = ProfileDisplayModes(),
    val entries: List<LibraryEntry> = emptyList(),
    val watching: List<ContinueWatchingItem> = emptyList(),
    val favorites: List<LibraryEntry> = emptyList(),
    val collectionCounts: ProfileCollectionCounts = ProfileCollectionCounts(),
    val statistics: WatchedStatistics = WatchedStatistics(),
    val statisticsMediaScope: StatisticsMediaScope = StatisticsMediaScope.ALL,
    val tasteStatistics: TasteStatistics = TasteStatistics(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val pendingRemovals: Set<Pair<ExternalMediaRef, MediaType>> = emptySet(),
    val actionError: AppError? = null,
    val loadError: AppError? = null,
    val statisticsError: AppError? = null,
    val isStatisticsLoading: Boolean = true,
    val selectedStatisticsMonth: Int? = null,
    val isAbandonedCollection: Boolean = false,
    val isWatchingCollection: Boolean = false
) {
    val currentViewMode: ProfileViewMode get() = displayModes.getMode(collection, category)
}

fun LibraryEntry.belongsToCategory(category: ProfileCategory): Boolean = when (category) {
    ProfileCategory.MOVIES -> mediaType == MediaType.MOVIE
    ProfileCategory.TV_SERIES -> mediaType == MediaType.SERIES
}

fun LibraryEntry.progressSortMetric(): Double = when (val p = progress) {
    is LibraryProgress.Series -> if (p.progress.trackableEpisodes > 0) {
        p.progress.watchedEpisodes.toDouble() / p.progress.trackableEpisodes
    } else {
        0.0
    }
    is LibraryProgress.Movie -> if (p.state is MovieWatchState.Watched) 1.0 else 0.0
    LibraryProgress.Unavailable -> 0.0
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
internal class ProfileViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val displayModePreferences: ProfileDisplayModePreferences,
    private val dateSource: CalendarDateSource,
    @param:DefaultDispatcher private val statisticsDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val mutableUiState = MutableStateFlow(ProfileUiState(today = dateSource.currentDate()))
    val uiState: StateFlow<ProfileUiState> = mutableUiState.asStateFlow()

    private var personalViewingEntries: List<PersonalViewingEntry> = emptyList()
    private val rawEntries = MutableStateFlow<List<LibraryEntry>>(emptyList())
    private val collectionRequests = MutableStateFlow(CollectionRequest(mutableUiState.value))
    private val statisticsRequests = MutableStateFlow(StatisticsRequest())
    private val libraryRetries = MutableStateFlow(0L)
    private val statisticsRetries = MutableStateFlow(0L)

    init {
        viewModelScope.launch {
            mutableUiState.subscriptionCount
                .map { it > 0 }
                .distinctUntilChanged()
                .transformLatest { subscribed ->
                    if (!subscribed) delay(5_000)
                    emit(subscribed)
                }
                .collectLatest { subscribed ->
                    if (subscribed) {
                        coroutineScope {
                            launch { observeDisplayModes() }
                            launch { observeLibraryEntries() }
                            launch { observeCollection() }
                            launch { observePersonalViewing() }
                            awaitCancellation()
                        }
                    }
                }
        }
    }

    fun setCollection(collection: ProfileCollection) {
        mutableUiState.update {
            it.copy(collection = collection, isAbandonedCollection = false, isWatchingCollection = false)
        }
        refilter()
    }

    fun setAbandonedCollection() {
        mutableUiState.update {
            it.copy(collection = ProfileCollection.WATCHED, isAbandonedCollection = true, isWatchingCollection = false)
        }
        refilter()
    }

    fun setWatchingCollection() {
        mutableUiState.update {
            it.copy(
                collection = ProfileCollection.WATCHED,
                sortOption = ProfileSortOption.RECENTLY_ADDED,
                isAbandonedCollection = false,
                isWatchingCollection = true
            )
        }
        refilter()
    }

    fun setCategory(category: ProfileCategory) {
        mutableUiState.update { it.copy(category = category) }
        refilter()
    }

    fun setSortOption(sortOption: ProfileSortOption) {
        mutableUiState.update { it.copy(sortOption = sortOption) }
        refilter()
    }

    fun setViewMode(mode: ProfileViewMode) {
        val current = mutableUiState.value
        mutableUiState.update { it.copy(actionError = null) }
        viewModelScope.launch {
            // The mode shown is the one the preferences emit, so a failed write keeps the previous one.
            try {
                displayModePreferences.setDisplayMode(current.collection, current.category, mode)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                mutableUiState.update { it.copy(actionError = AppError.LocalStorageFailure) }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        mutableUiState.update { it.copy(searchQuery = query) }
        refilter()
    }

    fun clearSearch() {
        onSearchQueryChanged("")
    }

    fun toggleFavorite(entry: LibraryEntry) {
        val ref = entry.mediaRef
        viewModelScope.launch {
            when (val result = libraryRepository.setFavorite(ref, entry.mediaType, !entry.isFavorite)) {
                is AppResult.Success -> {}
                is AppResult.Failure -> {
                    mutableUiState.update { it.copy(actionError = result.error) }
                }
            }
        }
    }

    fun setWatchedDate(entry: LibraryEntry, watchedDate: LocalDate?) {
        val ref = entry.mediaRef
        viewModelScope.launch {
            when (val result = libraryRepository.setWatchedDate(ref, entry.mediaType, watchedDate)) {
                is AppResult.Success -> {}
                is AppResult.Failure -> {
                    mutableUiState.update { it.copy(actionError = result.error) }
                }
            }
        }
    }

    fun remove(entry: LibraryEntry) {
        val ref = entry.mediaRef
        val pending = ref to entry.mediaType
        if (pending in mutableUiState.value.pendingRemovals) return
        mutableUiState.update {
            it.copy(pendingRemovals = it.pendingRemovals + pending, actionError = null)
        }
        viewModelScope.launch {
            when (val result = libraryRepository.remove(ref, entry.mediaType)) {
                is AppResult.Success -> {
                    mutableUiState.update { state ->
                        state.copy(pendingRemovals = state.pendingRemovals - pending)
                    }
                }
                is AppResult.Failure -> {
                    mutableUiState.update { state ->
                        state.copy(
                            pendingRemovals = state.pendingRemovals - pending,
                            actionError = result.error
                        )
                    }
                }
            }
        }
    }

    fun clearActionError() {
        mutableUiState.update { it.copy(actionError = null) }
    }

    fun retry() {
        mutableUiState.update { it.copy(isLoading = true, loadError = null) }
        libraryRetries.update { it + 1 }
    }

    fun retryStatistics() {
        mutableUiState.update { it.copy(isStatisticsLoading = true, statisticsError = null) }
        statisticsRetries.update { it + 1 }
    }

    fun setStatisticsMediaScope(scope: StatisticsMediaScope) {
        mutableUiState.update { it.copy(statisticsMediaScope = scope) }
        statisticsRequests.update { it.copy(scope = scope) }
    }

    fun setStatisticsViewingYear(year: Int) {
        val availableYears = mutableUiState.value.statistics.monthlyViewing.availableYears
        if (availableYears.isNotEmpty() && year !in availableYears) return
        mutableUiState.update { it.copy(selectedStatisticsMonth = null) }
        statisticsRequests.update { it.copy(year = year.takeUnless { it == dateSource.currentDate().year }) }
    }

    fun setStatisticsViewingMonth(month: Int?) {
        val monthly = mutableUiState.value.statistics.monthlyViewing
        val isFuture = month != null &&
            monthly.selectedYear == monthly.currentYear &&
            month > monthly.currentMonth
        if (month != null && (month !in 1..12 || isFuture)) return
        mutableUiState.update { it.copy(selectedStatisticsMonth = month) }
    }

    private suspend fun observeDisplayModes() {
        displayModePreferences.observeDisplayModes().collectLatest { modes ->
            mutableUiState.update { it.copy(displayModes = modes) }
        }
    }

    private suspend fun observeLibraryEntries() {
        libraryRetries.flatMapLatest { libraryRepository.observeEntries(LibraryQuery()) }
            .collectLatest { result ->
                when (result) {
                    is AppResult.Success -> {
                        val dashboard = withContext(statisticsDispatcher) {
                            val context = currentCoroutineContext()
                            val entries = result.value
                            Triple(
                                selectWatchingPreview(entries, checkCancelled = context::ensureActive),
                                selectFavoritePreview(entries, checkCancelled = context::ensureActive),
                                countCollections(entries, context::ensureActive)
                            )
                        }
                        rawEntries.value = result.value
                        mutableUiState.update {
                            it.copy(
                                isLoading = false,
                                loadError = null,
                                watching = dashboard.first,
                                favorites = dashboard.second,
                                collectionCounts = dashboard.third
                            )
                        }
                    }
                    is AppResult.Failure -> mutableUiState.update {
                        it.copy(isLoading = false, loadError = result.error)
                    }
                }
            }
    }

    private suspend fun observePersonalViewing() {
        combine(
            statisticsRetries.flatMapLatest { libraryRepository.observePersonalViewing() },
            dateSource.observeDate(),
            statisticsRequests
        ) { result, currentDate, request -> Triple(result, currentDate, request) }
            .mapLatest { (result, currentDate, request) ->
                if (result is AppResult.Success) personalViewingEntries = result.value
                val entries = personalViewingEntries
                val update = withContext(statisticsDispatcher) {
                    val context = currentCoroutineContext()
                    StatisticsUpdate(
                        today = currentDate,
                        statistics = calculateWatchedStatistics(
                            entries,
                            ZoneId.systemDefault(),
                            currentDate,
                            request.year ?: currentDate.year,
                            context::ensureActive
                        ),
                        tasteStatistics = calculateTasteStatistics(entries, request.scope, context::ensureActive),
                        error = (result as? AppResult.Failure)?.error
                    )
                }
                request to update
            }
            .collect { (request, update) ->
                // A dispatched result may already be queued when the user makes another selection.
                if (request != statisticsRequests.value) return@collect
                mutableUiState.update {
                    it.copy(
                        today = update.today,
                        statistics = update.statistics,
                        tasteStatistics = update.tasteStatistics,
                        isStatisticsLoading = false,
                        statisticsError = update.error
                    )
                }
            }
    }

    private suspend fun observeCollection() {
        combine(rawEntries, collectionRequests) { entries, request -> entries to request }
            .mapLatest { (entries, request) ->
                val sorted = withContext(statisticsDispatcher) {
                    val context = currentCoroutineContext()
                    filterCollection(entries, request, context::ensureActive)
                }
                Triple(entries, request, sorted)
            }
            .collect { (entries, request, sorted) ->
                if (request == collectionRequests.value && entries === rawEntries.value) {
                    mutableUiState.update { it.copy(entries = sorted) }
                }
            }
    }

    private fun refilter() {
        collectionRequests.value = CollectionRequest(mutableUiState.value)
    }
}

private data class StatisticsRequest(val year: Int? = null, val scope: StatisticsMediaScope = StatisticsMediaScope.ALL)

private data class CollectionRequest(
    val collection: ProfileCollection,
    val category: ProfileCategory,
    val sortOption: ProfileSortOption,
    val searchQuery: String,
    val isAbandonedCollection: Boolean,
    val isWatchingCollection: Boolean
) {
    constructor(state: ProfileUiState) : this(
        state.collection,
        state.category,
        state.sortOption,
        state.searchQuery,
        state.isAbandonedCollection,
        state.isWatchingCollection
    )
}

private fun filterCollection(
    allRaw: List<LibraryEntry>,
    state: CollectionRequest,
    checkCancelled: () -> Unit
): List<LibraryEntry> {
    val filtered = allRaw.filter { entry ->
        checkCancelled()
        val matchesCollection = if (state.isWatchingCollection) {
            entry.toContinueWatchingItem()?.let(ContinueWatchingPolicy::isContinueWatching) == true
        } else {
            when (state.collection) {
                ProfileCollection.WATCHED -> if (state.isAbandonedCollection) {
                    entry.inLibrary && entry.mediaType == MediaType.SERIES &&
                        entry.serialState == SeriesTrackingState.ABANDONED
                } else {
                    entry.inLibrary && entry.isWatched()
                }
                ProfileCollection.WATCH_LATER -> when (entry.mediaType) {
                    MediaType.SERIES -> entry.serialState == SeriesTrackingState.WATCH_LATER
                    MediaType.MOVIE -> !entry.isWatched() && entry.inLibrary
                }
                ProfileCollection.FAVORITES -> entry.isFavorite
            }
        }
        val matchesCategory = entry.belongsToCategory(state.category)
        val matchesSearch = if (state.searchQuery.isBlank()) {
            true
        } else {
            entry.title.contains(state.searchQuery, ignoreCase = true) ||
                entry.originalTitle?.contains(state.searchQuery, ignoreCase = true) == true
        }

        matchesCollection && matchesCategory && matchesSearch
    }

    checkCancelled()
    val sorted = when (state.sortOption) {
        ProfileSortOption.RECENTLY_ADDED -> when {
            state.isWatchingCollection -> filtered.sortedByDescending {
                checkCancelled()
                (it.progress as? LibraryProgress.Series)?.progress?.lastWatchedAt ?: Instant.MIN
            }
            state.collection == ProfileCollection.FAVORITES -> filtered.sortedWith(
                compareByDescending<LibraryEntry> {
                    checkCancelled()
                    it.favoriteAddedAt ?: Instant.MIN
                }
                    .thenBy { it.title.lowercase(Locale.ROOT) }
                    .thenBy { it.mediaRef.source.name }
                    .thenBy { it.mediaRef.externalId }
            )
            else -> filtered.sortedByDescending {
                checkCancelled()
                it.addedAt
            }
        }
        ProfileSortOption.TITLE -> filtered.sortedBy {
            checkCancelled()
            it.title.lowercase()
        }
        ProfileSortOption.RATING -> filtered.sortedWith(
            compareByDescending<LibraryEntry> {
                checkCancelled()
                it.personalRating?.value ?: -1
            }
                .thenBy { it.title.lowercase() }
        )
        ProfileSortOption.PROGRESS -> filtered.sortedWith(
            compareByDescending<LibraryEntry> {
                checkCancelled()
                it.progressSortMetric()
            }
                .thenBy { it.title.lowercase() }
        )
    }

    checkCancelled()
    return sorted
}

internal const val PROFILE_PREVIEW_LIMIT = 7

internal fun selectWatchingPreview(
    entries: Iterable<LibraryEntry>,
    limit: Int = PROFILE_PREVIEW_LIMIT,
    checkCancelled: () -> Unit = {}
): List<ContinueWatchingItem> = ContinueWatchingPolicy.select(
    entries.mapNotNull {
        checkCancelled()
        it.toContinueWatchingItem()
    },
    checkCancelled
).take(limit.coerceAtLeast(0))

internal fun selectFavoritePreview(
    entries: Iterable<LibraryEntry>,
    limit: Int = PROFILE_PREVIEW_LIMIT,
    checkCancelled: () -> Unit = {}
): List<LibraryEntry> = entries.asSequence()
    .filter {
        checkCancelled()
        it.isFavorite
    }
    // ponytail: legacy favorites have no timestamp; identity tie-break keeps ordering honest until re-favorited.
    .sortedWith(
        compareByDescending<LibraryEntry> {
            checkCancelled()
            it.favoriteAddedAt ?: Instant.MIN
        }
            .thenBy { it.title.lowercase(Locale.ROOT) }
            .thenBy { it.mediaRef.source.name }
            .thenBy { it.mediaRef.externalId }
    )
    .take(limit.coerceAtLeast(0))
    .toList()

internal fun countCollections(
    entries: Iterable<LibraryEntry>,
    checkCancelled: () -> Unit = {}
): ProfileCollectionCounts {
    var watchLater = 0
    var watched = 0
    var favorites = 0
    var abandoned = 0
    entries.forEach { entry ->
        checkCancelled()
        if (entry.isFavorite) favorites++
        if (entry.inLibrary) {
            if (entry.isWatched()) watched++
            if (when (entry.mediaType) {
                    MediaType.SERIES -> entry.serialState == SeriesTrackingState.WATCH_LATER
                    MediaType.MOVIE -> !entry.isWatched()
                }
            ) {
                watchLater++
            }
            if (entry.mediaType == MediaType.SERIES && entry.serialState == SeriesTrackingState.ABANDONED) abandoned++
        }
    }
    return ProfileCollectionCounts(watchLater, watched, favorites, abandoned)
}
