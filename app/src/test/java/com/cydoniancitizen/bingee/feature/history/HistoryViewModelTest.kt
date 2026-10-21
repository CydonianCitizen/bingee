package com.cydoniancitizen.bingee.feature.history

import androidx.lifecycle.SavedStateHandle
import com.cydoniancitizen.bingee.core.model.Episode
import com.cydoniancitizen.bingee.core.model.ExternalMediaRef
import com.cydoniancitizen.bingee.core.model.HistoryEpisode
import com.cydoniancitizen.bingee.core.model.HistoryEpisodePage
import com.cydoniancitizen.bingee.core.model.HistoryEpisodeQuery
import com.cydoniancitizen.bingee.core.model.HistoryFilter
import com.cydoniancitizen.bingee.core.model.HistoryItem
import com.cydoniancitizen.bingee.core.model.HistoryOverview
import com.cydoniancitizen.bingee.core.model.HistoryOverviewQuery
import com.cydoniancitizen.bingee.core.model.HistoryPage
import com.cydoniancitizen.bingee.core.model.HistoryPageQuery
import com.cydoniancitizen.bingee.core.model.HistorySnapshot
import com.cydoniancitizen.bingee.core.model.HistorySnapshotQuery
import com.cydoniancitizen.bingee.core.model.MediaSource
import com.cydoniancitizen.bingee.core.model.MediaType
import com.cydoniancitizen.bingee.core.result.AppError
import com.cydoniancitizen.bingee.core.result.AppResult
import com.cydoniancitizen.bingee.data.importexport.RestoreGeneration
import com.cydoniancitizen.bingee.debug.FakeLibraryRepository
import com.cydoniancitizen.bingee.domain.repository.LibraryRepository
import com.cydoniancitizen.bingee.testutil.TestCalendarDateSource
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val day = LocalDate.of(2026, 10, 2)

    @Before fun setup() = Dispatchers.setMain(dispatcher)

    @After fun teardown() = Dispatchers.resetMain()

    @Test fun firstEntryQueriesAllRetainedHistoryInsteadOfTheCurrentMonth() = runTest(dispatcher) {
        val repo = HistoryFake()
        val vm = HistoryViewModel(
            repo,
            TestCalendarDateSource(LocalDate.of(2026, 10, 20)),
            RestoreGeneration(),
            SavedStateHandle()
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
        runCurrent()
        assertEquals(listOf(repo.row), vm.uiState.value.page.items)
        assertNull(repo.lastQuery!!.month)
    }

    @Test fun automaticWindowsKeepOverlapAndAnchorWhileReleasingDistantRows() = runTest(dispatcher) {
        val rows = (1L..155L).map { id ->
            HistoryItem(
                if (id <=
                    75
                ) {
                    LocalDate.of(2016, 2, 1)
                } else {
                    day
                },
                id,
                MediaType.MOVIE,
                "Movie $id",
                null,
                null,
                0
            )
        }
        val retained = MutableStateFlow(rows)
        val repository = object : LibraryRepository by FakeLibraryRepository() {
            override fun observeHistory(query: HistorySnapshotQuery): Flow<AppResult<HistorySnapshot>> =
                retained.map { current ->
                    val candidates = current.filter { row ->
                        val cursor = query.page.cursor
                        cursor == null ||
                            if (query.page.backwards) row.mediaId < cursor.mediaId else row.mediaId > cursor.mediaId
                    }.let { if (query.page.backwards) it.asReversed() else it }
                    val selected = candidates.take(query.page.limit).let {
                        if (query.page.backwards) it.asReversed() else it
                    }
                    AppResult.Success(
                        HistorySnapshot(
                            HistoryOverview(rows.first().day, day),
                            HistoryPage(
                                selected,
                                hasPrevious = if (query.page.backwards) {
                                    candidates.size > query.page.limit
                                } else {
                                    query.page.cursor !=
                                        null
                                },
                                hasNext = if (query.page.backwards) {
                                    query.page.cursor != null
                                } else {
                                    candidates.size >
                                        query.page.limit
                                }
                            )
                        )
                    )
                }
        }
        val saved = SavedStateHandle()
        val vm =
            HistoryViewModel(repository, TestCalendarDateSource(LocalDate.of(2026, 10, 20)), RestoreGeneration(), saved)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
        runCurrent()
        assertEquals((1L..90L).toList(), vm.uiState.value.page.items.map { it.mediaId })
        vm.saveAnchor(rows[85].key, 17)
        vm.loadWindow(false)
        runCurrent()
        assertEquals((31L..120L).toList(), vm.uiState.value.page.items.map { it.mediaId })
        assertEquals(rows[85].key, vm.anchorKey)
        assertEquals(17, vm.anchorOffset)
        val recreated = HistoryViewModel(
            repository,
            TestCalendarDateSource(LocalDate.of(2026, 10, 20)),
            RestoreGeneration(),
            SavedStateHandle(saved.keys().associateWith { saved.get<Any>(it) })
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { recreated.uiState.collect() }
        runCurrent()
        assertEquals((31L..120L).toList(), recreated.uiState.value.page.items.map { it.mediaId })
        assertEquals(rows[85].key, recreated.anchorKey)
        assertEquals(17, recreated.anchorOffset)
        vm.loadWindow(false)
        runCurrent()
        vm.loadWindow(false)
        runCurrent()
        assertEquals((91L..155L).toList(), vm.uiState.value.page.items.map { it.mediaId })
        vm.loadWindow(true)
        runCurrent()
        assertEquals((61L..150L).toList(), vm.uiState.value.page.items.map { it.mediaId })
        retained.value = rows.filter { it.mediaId != 100L }
        runCurrent()
        assertTrue(vm.uiState.value.page.items.none { it.mediaId == 100L })
        assertTrue(vm.uiState.value.page.items.size <= 90)
    }

    @Test fun errorsRetryRestoreAndZoneChangesKeepSelectionButResetTransientIdentity() = runTest(dispatcher) {
        val repo = HistoryFake()
        val dates = TestCalendarDateSource(LocalDate.of(2026, 10, 20))
        val restore = RestoreGeneration()
        val saved = SavedStateHandle()
        val vm = HistoryViewModel(repo, dates, restore, saved)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
        runCurrent()
        assertEquals(1, vm.uiState.value.page.items.size)
        vm.toggleGroup(vm.uiState.value.page.items.single())
        runCurrent()
        assertEquals(1, vm.uiState.value.groups.size)
        repo.page.value = AppResult.Failure(AppError.LocalStorageFailure)
        runCurrent()
        assertEquals(AppError.LocalStorageFailure, vm.uiState.value.error)
        assertEquals(1, vm.uiState.value.page.items.size)
        repo.page.value = AppResult.Success(HistoryPage(listOf(repo.row)))
        vm.retry()
        runCurrent()
        assertNull(vm.uiState.value.error)
        restore.beginRestore()
        restore.endRestore()
        runCurrent()
        assertTrue(vm.uiState.value.groups.isEmpty())
        assertNull(repo.lastQuery!!.month)
        vm.toggleGroup(repo.row)
        runCurrent()
        dates.changeZone(ZoneId.of("America/New_York"))
        runCurrent()
        assertTrue(vm.uiState.value.groups.isEmpty())
        assertEquals(ZoneId.of("America/New_York"), repo.lastQuery!!.context.zoneId)
    }

    @Test fun switchingFilterCancelsOldReadsAndLeavingReleasesConsumers() = runTest(dispatcher) {
        val repo = HistoryFake()
        val vm =
            HistoryViewModel(
                repo,
                TestCalendarDateSource(LocalDate.of(2026, 10, 20)),
                RestoreGeneration(),
                SavedStateHandle()
            )
        val consumer = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
        runCurrent()
        vm.selectFilter(HistoryFilter.MOVIES)
        runCurrent()
        assertNull(repo.lastQuery!!.month)
        assertEquals(HistoryFilter.MOVIES, repo.lastQuery!!.filter)
        assertEquals(1, repo.activePages)
        consumer.cancel()
        advanceTimeBy(5_001)
        runCurrent()
        assertEquals(0, repo.activePages)
    }

    @Test fun childFailureDoesNotEraseMainPageAndClosingGroupReleasesChildren() = runTest(dispatcher) {
        val repo = HistoryFake()
        val vm =
            HistoryViewModel(
                repo,
                TestCalendarDateSource(LocalDate.of(2026, 10, 20)),
                RestoreGeneration(),
                SavedStateHandle()
            )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
        runCurrent()
        vm.toggleGroup(repo.row)
        runCurrent()
        repo.children.value = AppResult.Failure(AppError.LocalStorageFailure)
        runCurrent()
        assertNull(vm.uiState.value.error)
        assertEquals(1, vm.uiState.value.page.items.size)
        assertEquals(AppError.LocalStorageFailure, vm.uiState.value.groups.getValue(repo.row.key).error)
        vm.toggleGroup(repo.row)
        runCurrent()
        assertEquals(0, repo.activeChildren)
        assertTrue(vm.uiState.value.groups.isEmpty())
    }

    @Test fun recreationAfterZoneChangeDoesNotReuseSavedCursorOrExpansion() = runTest(dispatcher) {
        val saved = SavedStateHandle(
            mapOf(
                "history_month" to "2026-10",
                "history_zone" to "Pacific/Honolulu",
                "history_cursor" to "2026-10-02|1|1",
                "history_groups" to arrayListOf("2026-10-02:SERIES:1|false")
            )
        )
        val repo = HistoryFake()
        val vm = HistoryViewModel(repo, TestCalendarDateSource(LocalDate.of(2026, 10, 20)), RestoreGeneration(), saved)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
        runCurrent()
        assertNull(repo.lastQuery!!.cursor)
        assertTrue(vm.uiState.value.groups.isEmpty())
    }

    @Test
    fun childPageThatDisappearsIsReadAgainFromFirstPage() = runTest(dispatcher) {
        val repo = HistoryFake()
        repo.children.value =
            AppResult.Success(HistoryEpisodePage(listOf(HistoryEpisode(1, 1, 1, "Episode")), hasNext = true))
        val vm =
            HistoryViewModel(
                repo,
                TestCalendarDateSource(LocalDate.of(2026, 10, 20)),
                RestoreGeneration(),
                SavedStateHandle()
            )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
        runCurrent()
        vm.toggleGroup(repo.row)
        runCurrent()
        vm.navigateGroup(repo.row, false)
        runCurrent()
        assertTrue(repo.lastChildQuery!!.cursor != null)
        repo.children.value = AppResult.Success(HistoryEpisodePage())
        runCurrent()
        assertNull(repo.lastChildQuery!!.cursor)
    }

    @Test
    fun removedGroupsArePrunedFromSavedState() = runTest(dispatcher) {
        val repo = HistoryFake()
        val saved = SavedStateHandle()
        val vm = HistoryViewModel(repo, TestCalendarDateSource(LocalDate.of(2026, 10, 20)), RestoreGeneration(), saved)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
        runCurrent()
        vm.toggleGroup(repo.row)
        runCurrent()
        assertEquals(1, saved.get<ArrayList<String>>("history_groups")!!.size)
        repo.page.value = AppResult.Success(HistoryPage())
        runCurrent()
        assertTrue(saved.get<ArrayList<String>>("history_groups")!!.isEmpty())
        assertTrue(vm.uiState.value.groups.isEmpty())
    }

    private inner class HistoryFake : LibraryRepository by FakeLibraryRepository() {
        val row = HistoryItem(day, 1, MediaType.SERIES, "Series", null, ExternalMediaRef(MediaSource.TMDB, "1"), 1)
        val page = MutableStateFlow<AppResult<HistoryPage>>(AppResult.Success(HistoryPage(listOf(row))))
        val children =
            MutableStateFlow<AppResult<HistoryEpisodePage>>(
                AppResult.Success(HistoryEpisodePage(listOf(HistoryEpisode(1, 1, 1, "Episode"))))
            )
        var lastQuery: HistoryPageQuery? = null
        var lastChildQuery: HistoryEpisodeQuery? = null
        var activePages = 0
        var activeChildren = 0
        override fun observeHistory(query: HistorySnapshotQuery): Flow<AppResult<HistorySnapshot>> = flow {
            lastQuery = query.page
            lastChildQuery = query.groups.values.firstOrNull()
            activePages++
            activeChildren += query.groups.size
            try {
                emitAll(
                    kotlinx.coroutines.flow.combine(page, children) { parent, child ->
                        when (parent) {
                            is AppResult.Failure -> parent
                            is AppResult.Success -> {
                                val keys = parent.value.items.map { it.key }.toSet()
                                AppResult.Success(
                                    HistorySnapshot(
                                        HistoryOverview(day, day),
                                        parent.value,
                                        query.groups.filterKeys { it in keys }.mapValues { child }
                                    )
                                )
                            }
                        }
                    }
                )
            } finally {
                activePages--
                activeChildren -= query.groups.size
            }
        }
    }
}
