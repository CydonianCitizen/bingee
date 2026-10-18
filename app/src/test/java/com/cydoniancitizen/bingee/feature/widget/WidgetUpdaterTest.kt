package com.cydoniancitizen.bingee.feature.widget

import com.cydoniancitizen.bingee.core.model.ContinueWatchingItem
import com.cydoniancitizen.bingee.core.model.EpisodePosition
import com.cydoniancitizen.bingee.core.model.ExternalMediaRef
import com.cydoniancitizen.bingee.core.model.MediaSource
import com.cydoniancitizen.bingee.core.model.MediaType
import com.cydoniancitizen.bingee.core.model.ReleaseEvent
import com.cydoniancitizen.bingee.core.model.ReleaseEventType
import com.cydoniancitizen.bingee.core.model.ReleaseSubjectIdentity
import com.cydoniancitizen.bingee.core.model.ReleaseSubjectType
import com.cydoniancitizen.bingee.core.model.SeriesProgress
import com.cydoniancitizen.bingee.core.result.AppResult
import com.cydoniancitizen.bingee.data.settings.AppLanguage
import com.cydoniancitizen.bingee.data.settings.AppTheme
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WidgetUpdaterTest {
    private val today = LocalDate.of(2026, 10, 10)
    private val both = WidgetTargets(true, true)
    private val posterOnly = WidgetTargets(continueWatching = true)
    private val upNextOnly = WidgetTargets(upNext = true)

    @Test
    fun noInstalledWidgetsNeverStartsDataObservers() = runTest {
        val data = Inputs()
        val installed = MutableStateFlow(WidgetTargets())
        val updates = mutableListOf<WidgetTargets>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            observeWidgetUpdates(installed, data::snapshots).collect { updates += it }
        }
        runCurrent()
        data.watching.value = AppResult.Success(emptyList())
        data.dates.value = today.plusDays(1)
        data.themes.value = AppTheme.DARK
        runCurrent()
        assertEquals(0, data.starts)
        assertEquals(emptyList<WidgetTargets>(), updates)
        data.assertSubscribers(0)
    }

    @Test
    fun placementRemovalAndReplacementReconcileWithoutProcessRestartOrDuplicateCollectors() = runTest {
        val data = Inputs()
        val installed = MutableSharedFlow<WidgetTargets>(replay = 1).apply { tryEmit(WidgetTargets()) }
        val updates = mutableListOf<WidgetTargets>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            observeWidgetUpdates(installed, data::snapshots).collect { updates += it }
        }
        installed.emit(posterOnly)
        runCurrent()
        assertEquals(listOf(posterOnly), updates)
        data.assertSubscribers(1, calendar = 0)
        installed.emit(posterOnly)
        runCurrent()
        assertEquals(1, data.starts)

        installed.emit(both)
        runCurrent()
        assertEquals(both, updates.last())
        data.assertSubscribers(1)
        installed.emit(upNextOnly)
        runCurrent()
        assertEquals(upNextOnly, updates.last())
        data.assertSubscribers(1)

        installed.emit(WidgetTargets())
        runCurrent()
        data.assertSubscribers(0)
        val count = updates.size
        data.watching.value = AppResult.Success(emptyList())
        data.releases.value = AppResult.Success(emptyList())
        runCurrent()
        assertEquals(count, updates.size)

        installed.emit(both)
        runCurrent()
        assertEquals(both, updates.last())
        data.assertSubscribers(1)
        assertEquals(4, data.starts)
        job.cancel()
        runCurrent()
        data.assertSubscribers(0)
    }

    @Test
    fun changesToHiddenItemsAndUndrawnMetadataDoNotUpdateEitherType() = runTest {
        val data = Inputs()
        val updates = mutableListOf<WidgetTargets>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            observeWidgetUpdates(MutableStateFlow(both), data::snapshots).collect { updates += it }
        }
        runCurrent()
        val first = data.series
        data.watching.value = AppResult.Success(
            listOf(
                first.copy(progress = first.progress.copy(watchedEpisodes = 2), updatedAt = Instant.EPOCH),
                first.copy(mediaRef = ref("hidden"), title = "Hidden change")
            )
        )
        data.releases.value = AppResult.Success(
            listOf(
                movie("first", today).copy(subjectTitle = "Not drawn", subject = movie("other-subject", today).subject),
                movie("second", today.plusDays(1)),
                movie("third-changed", today.plusDays(5)),
                movie("past-changed", today.minusDays(1))
            )
        )
        runCurrent()
        assertEquals(listOf(both), updates)
    }

    @Test
    fun calendarChangesOnlyWakeUpNextAndThemeChangesWakeBoth() = runTest {
        val data = Inputs()
        val updates = mutableListOf<WidgetTargets>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            observeWidgetUpdates(MutableStateFlow(both), data::snapshots).collect { updates += it }
        }
        runCurrent()
        data.releases.value = AppResult.Success(listOf(movie("new", today)))
        runCurrent()
        assertEquals(listOf(both, upNextOnly), updates)
        data.themes.value = AppTheme.DARK
        runCurrent()
        assertEquals(listOf(both, upNextOnly, both), updates)
    }

    @Test
    fun languageChangesWakeBothTypesWithoutAnyLibraryOrThemeChange() = runTest {
        for (targets in listOf(both, posterOnly, upNextOnly)) {
            val data = Inputs()
            val updates = mutableListOf<WidgetTargets>()
            val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                observeWidgetUpdates(MutableStateFlow(targets), data::snapshots).collect { updates += it }
            }
            runCurrent()
            data.languages.value = AppLanguage.ITALIAN
            runCurrent()
            data.languages.value = AppLanguage.ENGLISH
            runCurrent()
            assertEquals(listOf(targets, targets, targets), updates)
            job.cancel()
            runCurrent()
            data.assertSubscribers(0)
            assertEquals(0, data.languages.subscriptionCount.value)
        }
    }

    @Test
    fun posterOnlyDoesNotWakeForCalendarOrDayChanges() = runTest {
        val data = Inputs()
        val updates = mutableListOf<WidgetTargets>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            observeWidgetUpdates(MutableStateFlow(posterOnly), data::snapshots).collect { updates += it }
        }
        runCurrent()
        data.releases.value = AppResult.Success(listOf(movie("new", today.plusDays(2))))
        data.dates.value = today.plusDays(1)
        runCurrent()
        assertEquals(listOf(posterOnly), updates)
        assertEquals(emptyList<LocalDate>(), data.cancelledDays)
        data.assertSubscribers(1, calendar = 0)
    }

    @Test
    fun dayRolloverWakesUpNextEvenWhenReleaseListIsUnchanged() = runTest {
        val data = Inputs()
        data.releases.value = AppResult.Success(listOf(movie("future", today.plusDays(2))))
        val updates = mutableListOf<WidgetTargets>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            observeWidgetUpdates(MutableStateFlow(both), data::snapshots).collect { updates += it }
        }
        runCurrent()
        data.dates.value = today.plusDays(1)
        runCurrent()
        assertEquals(listOf(both, upNextOnly), updates)
        assertEquals(listOf(today), data.cancelledDays)
        data.assertSubscribers(1)
    }

    @Test
    fun displayedFieldsAndEpisodeActionIdentityWakeInstalledTypes() = runTest {
        val data = Inputs()
        val updates = mutableListOf<WidgetTargets>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            observeWidgetUpdates(MutableStateFlow(both), data::snapshots).collect { updates += it }
        }
        runCurrent()
        val changes = listOf(
            data.series.copy(nextEpisode = EpisodePosition(1, 3)),
            data.series.copy(nextEpisodeRef = ref("replacement-episode")),
            data.series.copy(title = "Renamed"),
            data.series.copy(posterUrl = "https://example.test/new.jpg"),
            data.series.copy(mediaType = MediaType.MOVIE),
            data.series.copy(mediaRef = ref("replacement-series"))
        )
        changes.forEachIndexed { index, changed ->
            data.watching.value = AppResult.Success(listOf(changed))
            runCurrent()
            assertEquals(index * 2 + 2, updates.size)
            data.watching.value = AppResult.Success(listOf(data.series))
            runCurrent()
        }
        assertEquals(List(changes.size * 2 + 1) { both }, updates)
        data.watching.value = AppResult.Success(emptyList())
        runCurrent()
        assertEquals(changes.size * 2 + 2, updates.size)
        assertEquals(both, updates.last())
    }

    @Test
    fun visibleReleaseIdentityArtworkAndOrderAreNotSuppressed() = runTest {
        val data = Inputs()
        val updates = mutableListOf<WidgetTargets>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            observeWidgetUpdates(MutableStateFlow(upNextOnly), data::snapshots).collect { updates += it }
        }
        runCurrent()
        val first = movie("first", today)
        listOf(
            first.copy(mediaRef = ref("replacement-parent")),
            first.copy(title = "Renamed"),
            first.copy(posterUrl = "https://example.test/new.jpg"),
            first.copy(eventDate = today.plusDays(3))
        ).forEachIndexed { index, changed ->
            data.releases.value = AppResult.Success(listOf(changed, movie("second", today.plusDays(1))))
            runCurrent()
            assertEquals(index * 2 + 2, updates.size)
            data.releases.value = AppResult.Success(listOf(first, movie("second", today.plusDays(1))))
            runCurrent()
        }
        assertEquals(List(9) { upNextOnly }, updates)
    }

    @Test
    fun anEmptyCalendarDoesNotWakeWidgetsJustForDayRollover() = runTest {
        val data = Inputs()
        data.releases.value = AppResult.Success(emptyList())
        val updates = mutableListOf<WidgetTargets>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            observeWidgetUpdates(MutableStateFlow(both), data::snapshots).collect { updates += it }
        }
        runCurrent()
        data.dates.value = today.plusDays(1)
        runCurrent()
        assertEquals(listOf(both), updates)
    }

    private inner class Inputs {
        val series = ContinueWatchingItem(
            mediaRef = ref("series"),
            mediaType = MediaType.SERIES,
            title = "Series",
            posterUrl = null,
            progress = SeriesProgress(1, 4, 0, 1, false),
            nextEpisode = EpisodePosition(1, 2),
            nextEpisodeRef = ref("episode-2"),
            updatedAt = null
        )
        val watching = MutableStateFlow<AppResult<List<ContinueWatchingItem>>>(AppResult.Success(listOf(series)))
        val releases = MutableStateFlow<AppResult<List<ReleaseEvent>>>(
            AppResult.Success(listOf(movie("first", today), movie("second", today.plusDays(1))))
        )
        val languages = MutableStateFlow(AppLanguage.ENGLISH)
        val themes = MutableStateFlow(AppTheme.LIGHT)
        val dates = MutableStateFlow(today)
        val cancelledDays = mutableListOf<LocalDate>()
        var starts = 0

        fun snapshots(targets: WidgetTargets) = observeWidgetSnapshot(
            watching,
            { day ->
                flow {
                    try {
                        releases.collect { emit(it) }
                    } finally {
                        cancelledDays += day
                    }
                }
            },
            dates,
            themes,
            languages,
            includeUpcoming = targets.upNext
        ).also { starts++ }

        fun assertSubscribers(expected: Int, calendar: Int = expected) {
            assertEquals(expected, watching.subscriptionCount.value)
            assertEquals(calendar, releases.subscriptionCount.value)
            assertEquals(expected, languages.subscriptionCount.value)
            assertEquals(expected, themes.subscriptionCount.value)
            assertEquals(expected, dates.subscriptionCount.value)
        }
    }

    private fun ref(id: String) = ExternalMediaRef(MediaSource.TMDB, id)

    private fun movie(id: String, date: LocalDate) = ReleaseEvent(
        mediaRef = ref("parent-$id"),
        subject = ReleaseSubjectIdentity(
            MediaSource.TMDB,
            ReleaseSubjectType.MEDIA,
            id,
            ReleaseEventType.MOVIE_RELEASE
        ),
        mediaType = MediaType.MOVIE,
        eventDate = date,
        title = id
    )
}
