package com.cydoniancitizen.bingee.data.importexport

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cydoniancitizen.bingee.core.model.HistoryContext
import com.cydoniancitizen.bingee.core.model.HistoryEpisodeQuery
import com.cydoniancitizen.bingee.core.model.HistoryFilter
import com.cydoniancitizen.bingee.core.model.HistoryPageQuery
import com.cydoniancitizen.bingee.core.model.HistorySnapshotQuery
import com.cydoniancitizen.bingee.core.model.LibraryProgress
import com.cydoniancitizen.bingee.core.model.MediaSource
import com.cydoniancitizen.bingee.core.model.MediaType
import com.cydoniancitizen.bingee.core.model.SeriesTrackingState
import com.cydoniancitizen.bingee.core.model.isSeriesComplete
import com.cydoniancitizen.bingee.core.result.AppResult
import com.cydoniancitizen.bingee.data.library.DefaultLibraryRepository
import com.cydoniancitizen.bingee.data.library.local.BingeeDatabase
import com.cydoniancitizen.bingee.data.library.local.EpisodeEntity
import com.cydoniancitizen.bingee.data.library.local.EpisodeWatchProgressEntity
import com.cydoniancitizen.bingee.data.library.local.ExternalRefEntity
import com.cydoniancitizen.bingee.data.library.local.LibraryMembershipEntity
import com.cydoniancitizen.bingee.data.library.local.MediaDetailsEntity
import com.cydoniancitizen.bingee.data.library.local.MediaEntity
import com.cydoniancitizen.bingee.data.library.local.MediaRatingEntity
import com.cydoniancitizen.bingee.data.library.local.MovieWatchProgressEntity
import com.cydoniancitizen.bingee.data.library.local.SeasonEntity
import com.cydoniancitizen.bingee.data.library.local.SeriesStateOverrideEntity
import com.cydoniancitizen.bingee.data.library.local.SeriesWatchProgressEntity
import com.cydoniancitizen.bingee.data.settings.AppLanguage
import com.cydoniancitizen.bingee.data.settings.AppTheme
import com.cydoniancitizen.bingee.data.settings.DataStoreReleaseNotificationPreferences
import com.cydoniancitizen.bingee.data.settings.PortableUserPreferencesStore
import com.cydoniancitizen.bingee.data.settings.ProfileDisplayModes
import com.cydoniancitizen.bingee.data.settings.ProfileViewMode
import com.cydoniancitizen.bingee.data.settings.bingeePreferenceData
import com.cydoniancitizen.bingee.data.settings.bingeePreferences
import com.cydoniancitizen.bingee.domain.model.calculateWatchedStatistics
import com.cydoniancitizen.bingee.feature.history.HistoryViewModel
import com.cydoniancitizen.bingee.testutil.TestCalendarDateSource
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupDataStoreTest {
    private lateinit var database: BingeeDatabase
    private lateinit var store: BackupDataStore
    private val restoreGeneration = RestoreGeneration()
    private val exportedAt = Instant.parse("2026-08-04T10:00:00Z")
    private val validationDate = LocalDate.of(2026, 8, 18)

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            BingeeDatabase::class.java
        ).build()
        store = BackupDataStore(
            database,
            database.portableSnapshotDao(),
            database.releaseEventDao(),
            DataStoreReleaseNotificationPreferences(
                ApplicationProvider.getApplicationContext(),
                database,
                database.portableSnapshotDao()
            ),
            PortableUserPreferencesStore(
                ApplicationProvider.getApplicationContext(),
                database,
                database.portableSnapshotDao()
            ),
            restoreGeneration
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun portableSnapshotExcludesCacheAndRetainsEveryOwnershipPath() = runBlocking<Unit> {
        val dao = database.portableSnapshotDao()
        store.readPortableData() // Complete the preference bridge before comparing database snapshots.
        val ids = (0..8).map { index ->
            val type = if (index in 4..6 || index == 8) MediaType.SERIES else MediaType.MOVIE
            val id = dao.insertMedia(
                MediaEntity(
                    mediaType = type, title = "Title $index", originalTitle = null,
                    overview = "Cached metadata $index", posterUrl = null, releaseDate = null,
                    createdAt = exportedAt, metadataUpdatedAt = exportedAt, isFavorite = index == 0
                )
            )
            dao.insertExternalRef(ExternalRefEntity(id, MediaSource.TMDB, type, (1000 + index).toString()))
            id
        }
        dao.insertMembership(LibraryMembershipEntity(ids[1], exportedAt))
        dao.insertRating(MediaRatingEntity(ids[2], 8, exportedAt, exportedAt))
        dao.insertMovieProgress(MovieWatchProgressEntity(ids[3], exportedAt))
        dao.insertSeriesProgress(SeriesWatchProgressEntity(ids[4], completedAt = exportedAt))
        dao.insertSeriesStateOverride(SeriesStateOverrideEntity(ids[5]))
        for (index in listOf(4, 5, 6, 8)) {
            val season = dao.insertSeason(
                SeasonEntity(
                    localMediaId = ids[index], source = MediaSource.TMDB, externalId = "season-$index",
                    seasonNumber = 0, name = "Specials", overview = null, posterUrl = null, airDate = null,
                    episodeCount = 1, metadataUpdatedAt = exportedAt, episodesFetchedAt = exportedAt
                )
            )
            val episode = dao.insertEpisode(
                EpisodeEntity(
                    localSeasonId = season, source = MediaSource.TMDB, externalId = "episode-$index",
                    episodeNumber = 1, title = "Episode $index", overview = null, airDate = null,
                    runtimeMinutes = 25, stillUrl = null, metadataUpdatedAt = exportedAt
                )
            )
            if (index == 6) dao.insertEpisodeProgress(EpisodeWatchProgressEntity(episode, exportedAt))
        }
        val full = dao.readSnapshot()
        val selected = ids.take(7).toSet()
        val selectedSeasons = full.seasons.filter { it.localMediaId in selected }
        val seasonIds = selectedSeasons.map { it.localSeasonId }.toSet()
        val expected = full.copy(
            media = full.media.filter { it.localMediaId in selected },
            refs = full.refs.filter { it.localMediaId in selected },
            genres = full.genres.filter { it.localMediaId in selected },
            seasons = selectedSeasons,
            episodes = full.episodes.filter { it.localSeasonId in seasonIds }
        )
        assertEquals(expected, dao.readSnapshot(includeCache = false))
        val exported = store.readPortableData()
        assertEquals((1000..1006).map(Int::toString), exported.media.map { it.primaryRef.externalId })
        assertEquals(3, exported.seasons.size)
        assertEquals(3, exported.episodes.size)
        assertEquals(1, exported.episodeProgress.size)
        assertEquals(full, dao.readSnapshot())
    }

    @Test
    fun restoreReplacesPortableStateRegeneratesIdsAndSupportsRepeatImport() = runBlocking {
        val first = plan("1", includeSeries = false)
        store.restore(first)
        assertEquals(2L, restoreGeneration.changes.value.generation)
        val firstId = database.portableSnapshotDao().readSnapshot().media.single().localMediaId

        val second = plan("2", includeSeries = true)
        store.restore(second)
        val secondSnapshot = database.portableSnapshotDao().readSnapshot()
        assertEquals(2, secondSnapshot.media.size)
        assertNotEquals(firstId, secondSnapshot.media.single { it.mediaType == MediaType.MOVIE }.localMediaId)
        assertEquals(1, secondSnapshot.memberships.size)
        assertEquals(1, secondSnapshot.movieProgress.size)
        assertEquals(1, secondSnapshot.episodeProgress.size)
        assertEquals(1, secondSnapshot.ratings.size)
        assertEquals(1, secondSnapshot.seasons.size)
        assertEquals(1, secondSnapshot.episodes.size)
        assertEquals(1, store.currentLibraryCount())

        store.restore(second)
        val repeated = database.portableSnapshotDao().readSnapshot()
        assertEquals(secondSnapshot.media.size, repeated.media.size)
        assertEquals(secondSnapshot.refs.size, repeated.refs.size)
        assertEquals(secondSnapshot.memberships.size, repeated.memberships.size)
        assertEquals(secondSnapshot.episodeProgress.size, repeated.episodeProgress.size)
    }

    @Test
    fun restoreRollsBackAtEveryInsertionStage() = runBlocking {
        store.restore(plan("900", includeSeries = true))
        val before = database.portableSnapshotDao().readSnapshot()

        RestoreStage.entries.forEach { failedStage ->
            var failed = false
            try {
                store.restore(
                    plan(
                        (1000 + failedStage.ordinal).toString(),
                        includeSeries = true
                    ),
                    RestoreFailureInjector { stage ->
                        if (stage == failedStage) throw InjectedRestoreFailure(stage)
                    }
                )
            } catch (_: InjectedRestoreFailure) {
                failed = true
            }
            assertTrue("restore must fail at ${failedStage.name}", failed)
            assertEquals(before, database.portableSnapshotDao().readSnapshot())
        }
    }

    @Test
    fun restoreWhileHistoryIsUnsubscribedResetsLocalIdentityAndKeepsSelection() = runBlocking {
        store.restore(roundTripPlan())
        val repository = DefaultLibraryRepository(
            database.libraryDao(),
            database.watchProgressDao(),
            database.ratingDao(),
            Clock.fixed(exportedAt, ZoneOffset.UTC),
            TestCalendarDateSource(validationDate),
            Dispatchers.Default,
            database,
            restoreGeneration
        )
        val saved = SavedStateHandle()
        val vm = withContext(Dispatchers.Main) {
            HistoryViewModel(repository, TestCalendarDateSource(validationDate), restoreGeneration, saved)
        }
        val owner = ViewModelStore().apply { put("history", vm) }
        var collector = launch(Dispatchers.Default) { vm.uiState.collect() }
        try {
            withContext(Dispatchers.Main) {
                vm.selectFilter(HistoryFilter.EPISODES)
            }
            val before = withTimeout(10000) { vm.uiState.first { !it.loading && it.page.items.isNotEmpty() } }
            val group = before.page.items.first()
            withContext(Dispatchers.Main) { vm.toggleGroup(group) }
            withTimeout(10000) { vm.uiState.first { it.groups[group.key]?.loading == false } }
            withContext(Dispatchers.Main) { vm.saveAnchor(group.key, 12) }
            collector.cancelAndJoin()
            delay(5200) // A retained back-stack owner with no active History UI subscriber.
            store.restore(roundTripPlan())
            withTimeout(10000) {
                while (saved.get<Long>("history_restore") != restoreGeneration.changes.value.generation) delay(10)
            }
            assertNull(vm.anchorKey)
            assertTrue(saved.get<ArrayList<String>>("history_groups").orEmpty().isEmpty())
            collector = launch(Dispatchers.Default) { vm.uiState.collect() }
            val after = withTimeout(10000) { vm.uiState.first { !it.loading && it.page.items.isNotEmpty() } }
            assertEquals(HistoryFilter.EPISODES, after.filter)
            assertTrue(after.groups.isEmpty())
            assertEquals(before.page.items.map { it.title }, after.page.items.map { it.title })
        } finally {
            collector.cancelAndJoin()
            withContext(Dispatchers.Main) { owner.clear() }
        }
    }

    @Test
    fun everySupportedBackupVersionRestoresManualMovieDatesAndAbandonedSpecialsInHistory() = runBlocking {
        val original = roundTripPlan().document
        val manualDay = LocalDate.of(2026, 6, 18)
        val context = HistoryContext(validationDate, ZoneOffset.UTC)
        val repository = DefaultLibraryRepository(
            database.libraryDao(),
            database.watchProgressDao(),
            database.ratingDao(),
            Clock.fixed(exportedAt, ZoneOffset.UTC),
            TestCalendarDateSource(validationDate),
            Dispatchers.Default,
            database,
            restoreGeneration
        )
        for (version in 1..3) {
            val document = original.copy(
                schemaVersion = version,
                data = original.data.copy(
                    movieProgress = original.data.movieProgress.mapIndexed { index, progress ->
                        if (index == 0) progress.copy(watchedDate = manualDay) else progress
                    }
                )
            )
            val decoded = exportedDocument(BackupJsonCodec.encode(document))
            store.restore((validate(decoded) as BackupValidationResult.Success).plan)
            suspend fun read(month: Int) = (
                repository.observeHistory(
                    HistorySnapshotQuery(
                        HistoryPageQuery(YearMonth.of(2026, month), context),
                        restoreGeneration = restoreGeneration.changes.value.generation
                    )
                ).first() as AppResult.Success
                ).value
            assertEquals("550", read(6).page.items.single().detailsRef?.externalId)
            val group = read(7).page.items.single { it.mediaType == MediaType.SERIES }
            val snapshot = (
                repository.observeHistory(
                    HistorySnapshotQuery(
                        HistoryPageQuery(YearMonth.of(2026, 7), context),
                        mapOf(group.key to HistoryEpisodeQuery(context, group.day, group.mediaId)),
                        restoreGeneration.changes.value.generation
                    )
                ).first() as AppResult.Success
                ).value
            val children = (snapshot.groups.getValue(group.key) as AppResult.Success).value
            assertEquals(1, group.episodeCount)
            assertEquals(0, children.items.single().season)
        }
    }

    @Test
    fun exportedBackupRestoresToTheSameSemanticData() = runBlocking {
        store.restore(roundTripPlan())
        val clock = Clock.fixed(exportedAt, ZoneOffset.UTC)
        val first = exportedDocument(store.createPortableBackup(clock.instant()))

        assertEquals(exportedAt, first.exportedAt)
        val validated = validate(first)
        assertTrue(validated is BackupValidationResult.Success)
        store.restore((validated as BackupValidationResult.Success).plan)

        val secondBytes = store.createPortableBackup(clock.instant())
        val second = exportedDocument(secondBytes)
        assertEquals(first.data, second.data)
        assertArrayEquals(BackupJsonCodec.encode(first), secondBytes)
    }

    @Test
    fun editedEpisodeDateMovesHistoryAndStatisticsAndSurvivesEveryBackupVersion() = runBlocking {
        val original = roundTripPlan().document
        val data = original.data.copy(
            episodes = original.data.episodes.map {
                if (it.externalRef.externalId == "20001") {
                    it.copy(seasonRef = BackupRef(MediaSource.TMDB, "10002"), episodeNumber = 2)
                } else {
                    it
                }
            }
        )
        store.restore((validate(original.copy(data = data)) as BackupValidationResult.Success).plan)
        assertEquals(
            com.cydoniancitizen.bingee.data.library.local.ProgressWriteOutcome.SUCCESS,
            database.watchProgressDao().setEpisodeWatchedDate(
                MediaSource.TMDB,
                "20001",
                LocalDate.of(2026, 6, 18),
                Instant.parse("2026-07-01T08:00:00Z"),
                validationDate,
                ZoneOffset.UTC
            )
        )
        val exported = exportedDocument(store.createPortableBackup(exportedAt))
        assertEquals(Instant.parse("2026-06-18T08:00:00Z"), exported.data.episodeProgress.single().watchedAt)
        for (version in 1..3) {
            store.restore(
                (
                    validate(exportedDocument(BackupJsonCodec.encode(exported.copy(schemaVersion = version))))
                        as BackupValidationResult.Success
                    ).plan
            )
            val repository = DefaultLibraryRepository(
                database.libraryDao(),
                database.watchProgressDao(),
                database.ratingDao(),
                Clock.fixed(exportedAt, ZoneOffset.UTC),
                TestCalendarDateSource(validationDate),
                Dispatchers.Default,
                database,
                restoreGeneration
            )
            val viewing = (repository.observePersonalViewing().first() as AppResult.Success).value
            val statistics = calculateWatchedStatistics(viewing, ZoneOffset.UTC, validationDate, 2026)
            assertEquals(42L, statistics.monthlyViewing.months[5].seriesMinutes)
            assertEquals(0L, statistics.monthlyViewing.months[6].seriesMinutes)
            val history = (
                repository.observeHistory(
                    HistorySnapshotQuery(
                        HistoryPageQuery(YearMonth.of(2026, 6), HistoryContext(validationDate, ZoneOffset.UTC)),
                        restoreGeneration = restoreGeneration.changes.value.generation
                    )
                ).first() as AppResult.Success
                ).value
            assertEquals(LocalDate.of(2026, 6, 18), history.page.items.single().day)
        }
    }

    @Test
    fun exportBridgesLegacySettingsBeforeFirstReadAndRestorePreventsLegacyOverwrite() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val themeKey = stringPreferencesKey("app_theme")
        val languageKey = stringPreferencesKey("app_language")
        val spoilerKey = booleanPreferencesKey("hide_episode_spoilers")
        val watchedMoviesKey = stringPreferencesKey("profile_view_watched_movies")
        val watchedTvKey = stringPreferencesKey("profile_view_watched_tv")
        val watchLaterMoviesKey = stringPreferencesKey("profile_view_watch_later_movies")
        val watchLaterTvKey = stringPreferencesKey("profile_view_watch_later_tv")
        val favoritesMoviesKey = stringPreferencesKey("profile_view_favorites_movies")
        val favoritesTvKey = stringPreferencesKey("profile_view_favorites_tv")
        val prior = context.bingeePreferenceData.first()
        try {
            context.bingeePreferences.edit { values ->
                values[themeKey] = AppTheme.DARK.name
                values[languageKey] = AppLanguage.ITALIAN.name
                values[spoilerKey] = true
                values[watchedMoviesKey] = ProfileViewMode.GRID.name
                values[watchedTvKey] = ProfileViewMode.LIST.name
                values[watchLaterMoviesKey] = ProfileViewMode.GRID.name
                values[watchLaterTvKey] = ProfileViewMode.GRID.name
                values[favoritesMoviesKey] = ProfileViewMode.LIST.name
                values[favoritesTvKey] = ProfileViewMode.GRID.name
            }

            val firstExport = exportedDocument(store.createPortableBackup(exportedAt))
            val migratedPreferences = firstExport.data.preferences
            assertEquals(AppTheme.DARK, migratedPreferences.theme)
            assertEquals(AppLanguage.ITALIAN, migratedPreferences.language)
            assertTrue(migratedPreferences.hideEpisodeSpoilers)
            assertEquals(ProfileViewMode.GRID, migratedPreferences.profileDisplayModes.watchedMovies)
            assertEquals(ProfileViewMode.LIST, migratedPreferences.profileDisplayModes.watchedTvSeries)
            assertEquals(ProfileViewMode.GRID, migratedPreferences.profileDisplayModes.watchLaterMovies)
            assertEquals(ProfileViewMode.GRID, migratedPreferences.profileDisplayModes.watchLaterTvSeries)
            assertEquals(ProfileViewMode.LIST, migratedPreferences.profileDisplayModes.favoritesMovies)
            assertEquals(ProfileViewMode.GRID, migratedPreferences.profileDisplayModes.favoritesTvSeries)

            val restoredPreferences = migratedPreferences.copy(
                theme = AppTheme.LIGHT,
                language = AppLanguage.ENGLISH,
                hideEpisodeSpoilers = false,
                profileDisplayModes = ProfileDisplayModes()
            )
            store.restore(
                ValidatedBackupPlan(
                    BackupDocument(
                        BACKUP_FORMAT_ID,
                        BACKUP_SCHEMA_VERSION,
                        exportedAt,
                        BackupData(
                            media = emptyList(),
                            seasons = emptyList(),
                            episodes = emptyList(),
                            library = emptyList(),
                            movieProgress = emptyList(),
                            episodeProgress = emptyList(),
                            ratings = emptyList(),
                            preferences = restoredPreferences
                        )
                    )
                )
            )
            val restoredExport = exportedDocument(store.createPortableBackup(exportedAt))
            assertEquals(restoredPreferences, restoredExport.data.preferences)
            assertTrue(database.portableSnapshotDao().getPreferences()?.legacySettingsBridgeCompleted == true)
        } finally {
            context.bingeePreferences.edit { values ->
                if (prior[themeKey] == null) values.remove(themeKey) else values[themeKey] = prior[themeKey]!!
                if (prior[languageKey] ==
                    null
                ) {
                    values.remove(languageKey)
                } else {
                    values[languageKey] = prior[languageKey]!!
                }
                if (prior[spoilerKey] == null) values.remove(spoilerKey) else values[spoilerKey] = prior[spoilerKey]!!
                if (prior[watchedMoviesKey] == null) {
                    values.remove(watchedMoviesKey)
                } else {
                    values[watchedMoviesKey] = prior[watchedMoviesKey]!!
                }
                if (prior[watchedTvKey] ==
                    null
                ) {
                    values.remove(watchedTvKey)
                } else {
                    values[watchedTvKey] = prior[watchedTvKey]!!
                }
                if (prior[watchLaterMoviesKey] == null) {
                    values.remove(watchLaterMoviesKey)
                } else {
                    values[watchLaterMoviesKey] = prior[watchLaterMoviesKey]!!
                }
                if (prior[watchLaterTvKey] == null) {
                    values.remove(watchLaterTvKey)
                } else {
                    values[watchLaterTvKey] = prior[watchLaterTvKey]!!
                }
                if (prior[favoritesMoviesKey] == null) {
                    values.remove(favoritesMoviesKey)
                } else {
                    values[favoritesMoviesKey] = prior[favoritesMoviesKey]!!
                }
                if (prior[favoritesTvKey] == null) {
                    values.remove(favoritesTvKey)
                } else {
                    values[favoritesTvKey] = prior[favoritesTvKey]!!
                }
            }
        }
    }

    @Test
    fun personalDatesSurviveMetadataRefreshAndExportRestoreForMoviesAndSeries() = runBlocking {
        val initial = plan("800", includeSeries = true).document
        val watchedDate = validationDate.minusDays(2)
        val seriesRef = initial.data.media.single { it.mediaType == MediaType.SERIES }.primaryRef
        val dated = initial.copy(
            data = initial.data.copy(
                movieProgress = initial.data.movieProgress.map { it.copy(watchedDate = watchedDate) },
                seriesProgress = listOf(BackupSeriesProgress(seriesRef, exportedAt, watchedDate))
            )
        )
        store.restore((validate(dated) as BackupValidationResult.Success).plan)
        val before = store.readPortableData()
        val snapshot = database.portableSnapshotDao().readSnapshot()
        snapshot.media.forEach { media ->
            val ref = snapshot.refs.single { it.localMediaId == media.localMediaId }
            database.detailsDao().storeDetails(
                candidate = media.copy(releaseDate = validationDate),
                source = ref.source,
                externalId = ref.externalId,
                details = MediaDetailsEntity(
                    localMediaId = media.localMediaId,
                    backdropUrl = null,
                    productionStatus = "UNKNOWN",
                    originalLanguage = null,
                    runtimeMinutes = null,
                    episodeRuntimeMinutes = null,
                    numberOfSeasons = null,
                    numberOfEpisodes = null,
                    detailsFetchedAt = exportedAt
                ),
                genres = emptyList()
            )
        }
        val exported = exportedDocument(store.createPortableBackup(exportedAt))
        assertTrue(exported.data.media.all { it.releaseDate == validationDate })
        assertEquals(before.movieProgress, exported.data.movieProgress)
        assertEquals(before.seriesProgress, exported.data.seriesProgress)
        val validated = validate(exported)
        assertTrue(validated is BackupValidationResult.Success)
        store.restore((validated as BackupValidationResult.Success).plan)
        assertEquals(exported.data, store.readPortableData())
    }

    @Test
    fun favoriteAndRatingChronologySurviveExportAndRestoreUnchanged() = runBlocking {
        val dated = BackupRef(MediaSource.TMDB, "700")
        val legacyFavorite = BackupRef(MediaSource.TMDB, "701")
        val favoriteAddedAt = Instant.parse("2026-05-06T07:08:09Z")
        val ratedAt = Instant.parse("2026-03-01T09:00:00Z")
        val ratingUpdatedAt = Instant.parse("2026-06-12T18:30:00Z")
        val data = BackupData(
            media = listOf(
                BackupMedia(
                    dated,
                    listOf(dated),
                    MediaType.MOVIE,
                    "Favorite with chronology",
                    null,
                    null,
                    null,
                    null,
                    isFavorite = true,
                    favoriteAddedAt = favoriteAddedAt
                ),
                // A backup produced before v4 recorded chronology: the flag without the timestamp.
                BackupMedia(
                    legacyFavorite,
                    listOf(legacyFavorite),
                    MediaType.MOVIE,
                    "Legacy favorite",
                    null,
                    null,
                    null,
                    null,
                    isFavorite = true
                )
            ),
            seasons = emptyList(),
            episodes = emptyList(),
            library = listOf(BackupLibraryEntry(dated, exportedAt)),
            movieProgress = emptyList(),
            episodeProgress = emptyList(),
            // Rating chronology is two independent instants: when the title was first rated and when
            // that rating last changed.
            ratings = listOf(BackupRating(dated, 7, ratedAt, ratingUpdatedAt)),
            preferences = BackupPreferences(3, true, false, true)
        )
        val plan = (
            validate(BackupDocument(BACKUP_FORMAT_ID, BACKUP_SCHEMA_VERSION, exportedAt, data))
                as BackupValidationResult.Success
            ).plan

        store.restore(plan)
        val exported = exportedDocument(store.createPortableBackup(exportedAt))
        val reimported = (validate(exported) as BackupValidationResult.Success).plan
        store.restore(reimported)

        val snapshot = database.portableSnapshotDao().readSnapshot()
        val refsByMedia = snapshot.refs.associate { it.localMediaId to it.externalId }
        val restoredDated = snapshot.media.single { refsByMedia[it.localMediaId] == "700" }
        val restoredLegacy = snapshot.media.single { refsByMedia[it.localMediaId] == "701" }

        assertTrue(restoredDated.isFavorite)
        assertEquals(favoriteAddedAt, restoredDated.favoriteAddedAt)
        assertTrue(restoredLegacy.isFavorite)
        assertNull(restoredLegacy.favoriteAddedAt)

        val rating = snapshot.ratings.single()
        assertEquals(ratedAt, rating.ratedAt)
        assertEquals(ratingUpdatedAt, rating.updatedAt)
    }

    @Test
    fun sameTmdbIdMovieAndSeriesAndMovieRuntimeSurviveExportAndRestore() = runBlocking {
        // TMDB movie 1399 and series 1399 are unrelated works; the backup must keep both apart.
        val ref = BackupRef(MediaSource.TMDB, "1399")
        val data = BackupData(
            media = listOf(
                BackupMedia(ref, listOf(ref), MediaType.MOVIE, "Movie", null, null, null, null, runtimeMinutes = 116),
                BackupMedia(ref, listOf(ref), MediaType.SERIES, "Series", null, null, null, null)
            ),
            seasons = emptyList(),
            episodes = emptyList(),
            library = listOf(
                BackupLibraryEntry(ref, exportedAt, MediaType.MOVIE),
                BackupLibraryEntry(ref, exportedAt, MediaType.SERIES)
            ),
            movieProgress = listOf(BackupMovieProgress(ref, exportedAt)),
            episodeProgress = emptyList(),
            ratings = listOf(BackupRating(ref, 6, exportedAt, exportedAt, MediaType.SERIES)),
            preferences = BackupPreferences(3, true, false, true)
        )
        store.restore(
            (
                validate(BackupDocument(BACKUP_FORMAT_ID, BACKUP_SCHEMA_VERSION, exportedAt, data))
                    as BackupValidationResult.Success
                ).plan
        )
        val exported = exportedDocument(store.createPortableBackup(exportedAt))
        store.restore((validate(exported) as BackupValidationResult.Success).plan)

        val snapshot = database.portableSnapshotDao().readSnapshot()
        val movie = snapshot.media.single { it.mediaType == MediaType.MOVIE }
        val series = snapshot.media.single { it.mediaType == MediaType.SERIES }
        assertEquals(2, snapshot.refs.count { it.externalId == "1399" })
        assertEquals(2, snapshot.memberships.size)
        assertEquals(116, movie.runtimeMinutes)
        assertNull(series.runtimeMinutes)
        assertEquals(series.localMediaId, snapshot.ratings.single().localMediaId)
        assertEquals(movie.localMediaId, snapshot.movieProgress.single().localMediaId)
    }

    @Test
    fun seriesCompletenessSurvivesAnOfflineRoundTripInLibraryAndStatistics() = runBlocking {
        val watchedAt = Instant.parse("2026-08-01T20:00:00Z")
        // Production write paths, so the seasons carry a real fetch timestamp before export.
        addSeries("1399", "Complete")
        storeSeason("1399", "13990", 0, declared = 2, episodes = listOf("139901"))
        storeSeason("1399", "13991", 1, declared = 2, episodes = listOf("139911", "139912"))
        storeSeason("1399", "13992", 2, declared = 0, episodes = emptyList())
        addSeries("1400", "Partial season")
        storeSeason("1400", "14001", 1, declared = 3, episodes = listOf("140011", "140012"))
        addSeries("1401", "Unwatched episode")
        storeSeason("1401", "14011", 1, declared = 2, episodes = listOf("140111", "140112"))
        val progress = database.watchProgressDao()
        progress.markSeasonWatched(MediaSource.TMDB, "13991", validationDate, watchedAt)
        progress.markSeasonWatched(MediaSource.TMDB, "14001", validationDate, watchedAt)
        progress.markEpisodeWatched(MediaSource.TMDB, "140111", validationDate, watchedAt)
        val expected = mapOf("1399" to true, "1400" to false, "1401" to false)
        assertEquals(expected, seriesCompleteness())

        val exported = exportedDocument(store.createPortableBackup(exportedAt))
        store.restore((validate(exported) as BackupValidationResult.Success).plan)

        assertEquals(expected, seriesCompleteness())
        // Freshness is not faked: a restored season has no fetch timestamp until TMDB answers again.
        assertTrue(database.portableSnapshotDao().readSnapshot().seasons.all { it.episodesFetchedAt == null })

        val reexported = exportedDocument(store.createPortableBackup(exportedAt))
        assertEquals(exported.data.seasons, reexported.data.seasons)
        assertTrue(reexported.data.seasons.single { it.externalRef.externalId == "13992" }.isKnownEmpty)
        store.restore((validate(reexported) as BackupValidationResult.Success).plan)
        assertEquals(expected, seriesCompleteness())

        progress.markEpisodeUnwatched(MediaSource.TMDB, "139911")
        assertEquals(expected + ("1399" to false), seriesCompleteness())
        assertTrue(database.portableSnapshotDao().readSnapshot().seriesProgress.isEmpty())
        progress.markEpisodeWatched(MediaSource.TMDB, "139911", validationDate, watchedAt.plusSeconds(60))
        assertEquals(expected, seriesCompleteness())
        assertEquals(1, database.portableSnapshotDao().readSnapshot().seriesProgress.size)

        // Older files lack empty-season evidence. Positive counts still work; zero must stay unknown.
        val legacy = exported.copy(
            schemaVersion = BACKUP_SCHEMA_VERSION_V1,
            data = exported.data.copy(
                media = exported.data.media.map { it.copy(genres = emptyList(), runtimeMinutes = null) },
                seasons = exported.data.seasons.map { it.copy(isKnownEmpty = false) },
                library = exported.data.library.map { it.copy(mediaType = null) }
            )
        )
        store.restore((validate(legacy) as BackupValidationResult.Success).plan)
        assertEquals(expected + ("1399" to false), seriesCompleteness())
    }

    /** Library state and the statistics projection must agree on which series are complete. */
    private suspend fun seriesCompleteness(): Map<String, Boolean> {
        val repository = DefaultLibraryRepository(
            database.libraryDao(),
            database.watchProgressDao(),
            database.ratingDao(),
            Clock.fixed(exportedAt, ZoneOffset.UTC),
            TestCalendarDateSource(validationDate),
            kotlinx.coroutines.Dispatchers.Default,
            historyDatabase = database
        )
        val entries = (repository.observeEntries().first() as AppResult.Success).value
        val library = entries.associate { it.mediaRef.externalId to (it.serialState == SeriesTrackingState.WATCHED) }
        val viewing = (repository.observePersonalViewing().first() as AppResult.Success).value
        assertEquals(library, viewing.associate { it.mediaRef.externalId to it.isCompletedTitle })
        val continueWatching = database.libraryDao().observeContinueWatchingRows(MediaSource.TMDB, validationDate)
            .first().associate {
                it.externalId to isSeriesComplete(
                    it.watchedEpisodes,
                    it.trackableEpisodes,
                    it.hasSufficientCoverage
                )
            }
        val withNextEpisode = entries.filter {
            val progress = (it.progress as? LibraryProgress.Series)?.progress
            progress != null && progress.watchedEpisodes < progress.trackableEpisodes
        }.map { it.mediaRef.externalId }.toSet()
        assertEquals(library.filterKeys { it in withNextEpisode }, continueWatching)
        assertEquals(library.count { it.value }, calculateWatchedStatistics(viewing).tvSeriesCompletedCount)
        return library
    }

    private suspend fun addSeries(externalId: String, title: String) {
        database.libraryDao().addToLibrary(
            MediaEntity(
                mediaType = MediaType.SERIES,
                title = title,
                originalTitle = null,
                overview = null,
                posterUrl = null,
                releaseDate = null,
                createdAt = exportedAt,
                metadataUpdatedAt = exportedAt
            ),
            MediaSource.TMDB,
            externalId,
            exportedAt
        )
    }

    private suspend fun storeSeason(
        seriesId: String,
        seasonId: String,
        number: Int,
        declared: Int,
        episodes: List<String>
    ) {
        database.seriesDao().storeSeasonEpisodes(
            MediaSource.TMDB,
            seriesId,
            SeasonEntity(
                localMediaId = 0,
                source = MediaSource.TMDB,
                externalId = seasonId,
                seasonNumber = number,
                name = null,
                overview = null,
                posterUrl = null,
                airDate = null,
                episodeCount = declared,
                metadataUpdatedAt = exportedAt,
                episodesFetchedAt = null
            ),
            episodes.mapIndexed { index, episodeId ->
                EpisodeEntity(
                    localSeasonId = 0,
                    source = MediaSource.TMDB,
                    externalId = episodeId,
                    episodeNumber = index + 1,
                    title = "Episode ${index + 1}",
                    overview = null,
                    airDate = LocalDate.of(2026, 7, 1),
                    runtimeMinutes = 45,
                    stillUrl = null,
                    metadataUpdatedAt = exportedAt
                )
            },
            exportedAt
        )
    }

    private fun exportedDocument(bytes: ByteArray): BackupDocument =
        (BackupJsonCodec.parse(bytes) as BackupParseResult.Success).document

    private class InjectedRestoreFailure(stage: RestoreStage) : RuntimeException(stage.name)

    private fun roundTripPlan(): ValidatedBackupPlan {
        val movie = BackupRef(MediaSource.TMDB, "550")
        val movieAlias = BackupRef(MediaSource.TMDB, "551")
        val series = BackupRef(MediaSource.TMDB, "1399")
        val seriesAlias = BackupRef(MediaSource.TMDB, "1400")
        val specials = BackupRef(MediaSource.TMDB, "10001")
        val regular = BackupRef(MediaSource.TMDB, "10002")
        val watchedEpisode = BackupRef(MediaSource.TMDB, "20001")
        val futureEpisode = BackupRef(MediaSource.TMDB, "20002")
        val removedMovie = BackupRef(MediaSource.TMDB, "600")
        val watchedAt = Instant.parse("2026-07-01T08:00:00Z")
        val ratedAt = Instant.parse("2026-07-02T08:00:00Z")
        val data = BackupData(
            media = listOf(
                BackupMedia(
                    movie,
                    listOf(movie, movieAlias),
                    MediaType.MOVIE,
                    "æ˜ ç”» â€” æ—¥æœ¬èªž",
                    null,
                    null,
                    null,
                    null
                ),
                BackupMedia(
                    series,
                    listOf(series, seriesAlias),
                    MediaType.SERIES,
                    "Series with optional metadata",
                    null,
                    null,
                    null,
                    null
                ),
                BackupMedia(
                    removedMovie,
                    listOf(removedMovie),
                    MediaType.MOVIE,
                    "Removed but rated",
                    null,
                    null,
                    null,
                    null
                )
            ),
            seasons = listOf(
                BackupSeason(series, specials, 0, "Specials", null, null, null, 1),
                BackupSeason(series, regular, 1, "Season 1", null, null, null, 2)
            ),
            episodes = listOf(
                BackupEpisode(specials, watchedEpisode, 1, "Special", null, null, 42, null),
                BackupEpisode(
                    regular,
                    futureEpisode,
                    1,
                    "Future episode",
                    null,
                    java.time.LocalDate.of(2026, 12, 31),
                    null,
                    null
                )
            ),
            library = listOf(
                BackupLibraryEntry(movie, exportedAt),
                BackupLibraryEntry(series, exportedAt)
            ),
            movieProgress = listOf(
                BackupMovieProgress(movie, watchedAt),
                BackupMovieProgress(removedMovie, watchedAt)
            ),
            episodeProgress = listOf(BackupEpisodeProgress(watchedEpisode, watchedAt)),
            ratings = listOf(
                BackupRating(movie, 10, ratedAt, ratedAt),
                BackupRating(removedMovie, 4, ratedAt, ratedAt)
            ),
            preferences = BackupPreferences(7, false, true, false),
            abandonedSeries = listOf(BackupAbandonedSeries(series))
        )
        val document = BackupDocument(BACKUP_FORMAT_ID, BACKUP_SCHEMA_VERSION, exportedAt, data)
        return (validate(document) as BackupValidationResult.Success).plan
    }

    private fun plan(
        movieId: String,
        includeSeries: Boolean,
        schemaVersion: Int = BACKUP_SCHEMA_VERSION
    ): ValidatedBackupPlan {
        val movieRef = BackupRef(MediaSource.TMDB, movieId)
        val seriesRef = BackupRef(MediaSource.TMDB, "${movieId}1")
        val seasonRef = BackupRef(MediaSource.TMDB, "${movieId}2")
        val episodeRef = BackupRef(MediaSource.TMDB, "${movieId}3")
        val data = BackupData(
            media = buildList {
                add(BackupMedia(movieRef, listOf(movieRef), MediaType.MOVIE, "Movie $movieId", null, null, null, null))
                if (includeSeries) {
                    add(
                        BackupMedia(
                            seriesRef,
                            listOf(seriesRef),
                            MediaType.SERIES,
                            "Series $movieId",
                            null,
                            null,
                            null,
                            null
                        )
                    )
                }
            },
            seasons = if (includeSeries) {
                listOf(
                    BackupSeason(seriesRef, seasonRef, 0, "Specials", null, null, null, 1)
                )
            } else {
                emptyList()
            },
            episodes = if (includeSeries) {
                listOf(
                    BackupEpisode(seasonRef, episodeRef, 1, "Episode", null, null, 40, null)
                )
            } else {
                emptyList()
            },
            library = buildList {
                add(BackupLibraryEntry(movieRef, exportedAt))
            },
            movieProgress = listOf(BackupMovieProgress(movieRef, exportedAt)),
            episodeProgress = if (includeSeries) listOf(BackupEpisodeProgress(episodeRef, exportedAt)) else emptyList(),
            ratings = buildList {
                add(BackupRating(movieRef, 9, exportedAt, exportedAt))
            },
            preferences = BackupPreferences(3, true, false, true)
        )
        val document = BackupDocument(BACKUP_FORMAT_ID, schemaVersion, exportedAt, data)
        return (validate(document) as BackupValidationResult.Success).plan
    }

    private fun validate(document: BackupDocument) = BackupValidator.validate(document, validationDate)
}
