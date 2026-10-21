package com.cydoniancitizen.bingee

import android.content.Intent
import android.net.ConnectivityManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cydoniancitizen.bingee.core.model.MediaSource
import com.cydoniancitizen.bingee.core.model.MediaType
import com.cydoniancitizen.bingee.core.navigation.DetailRouteArgs
import com.cydoniancitizen.bingee.data.library.local.ALL_MIGRATIONS
import com.cydoniancitizen.bingee.data.library.local.BingeeDatabase
import com.cydoniancitizen.bingee.data.library.local.EpisodeEntity
import com.cydoniancitizen.bingee.data.library.local.MediaDetailsEntity
import com.cydoniancitizen.bingee.data.library.local.MediaEntity
import com.cydoniancitizen.bingee.data.library.local.SeasonEntity
import com.cydoniancitizen.bingee.data.notification.NotificationDetailIntent
import com.cydoniancitizen.bingee.data.settings.bingeePreferences
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationActivityNavigationTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun completeOnboarding() {
        runBlocking {
            context.bingeePreferences.edit { preferences ->
                preferences[booleanPreferencesKey("onboarding_complete")] = true
            }
        }
    }

    @Test
    fun coldStartNavigatesOnceAndBackReturnsToHome() {
        val scenario = ActivityScenario.launch<MainActivity>(
            NotificationDetailIntent.intent(
                context,
                DetailRouteArgs(MediaType.MOVIE, 101)
            )
        )
        try {
            composeRule.onNodeWithText(context.getString(R.string.detail_screen_title)).assertIsDisplayed()
            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            composeRule.onNodeWithText(context.getString(R.string.home_title)).assertIsDisplayed()
        } finally {
            scenario.onActivity { it.finish() }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        }
    }

    @Test
    fun warmTvIntentNavigatesToExistingDetailRoute() {
        ActivityScenario.launch<MainActivity>(Intent(context, MainActivity::class.java)).use { scenario ->
            composeRule.onNodeWithText(context.getString(R.string.home_title)).assertIsDisplayed()
            scenario.onActivity { activity ->
                InstrumentationRegistry.getInstrumentation().callActivityOnNewIntent(
                    activity,
                    NotificationDetailIntent.intent(
                        context,
                        DetailRouteArgs(MediaType.SERIES, 202)
                    )
                )
            }

            composeRule.onNodeWithText(context.getString(R.string.detail_screen_title)).assertIsDisplayed()
            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            composeRule.onNodeWithText(context.getString(R.string.home_title)).assertIsDisplayed()
        }
    }

    @Test
    fun warmDetailsIntentChangesQualifiedIdentityAndBackRestoresPreviousTitles() = withCachedTitles {
        ActivityScenario.launch<MainActivity>(Intent(context, MainActivity::class.java)).use { scenario ->
            composeRule.onNodeWithText(context.getString(R.string.home_title)).assertIsDisplayed()
            deliver(scenario, MediaType.MOVIE, 990001)
            assertTitle("Navigation movie one")
            deliver(scenario, MediaType.SERIES, 990001)
            assertTitle("Navigation series")
            deliver(scenario, MediaType.MOVIE, 990002)
            assertTitle("Navigation movie two")

            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            assertTitle("Navigation series")
            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            assertTitle("Navigation movie one")
            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            composeRule.onNodeWithText(context.getString(R.string.home_title)).assertIsDisplayed()
        }
    }

    @Test
    fun repeatedSameTitleIntentDoesNotAddBackEntriesAfterRecreation() = withCachedTitles {
        ActivityScenario.launch<MainActivity>(Intent(context, MainActivity::class.java)).use { scenario ->
            composeRule.onNodeWithText(context.getString(R.string.home_title)).assertIsDisplayed()
            deliver(scenario, MediaType.MOVIE, 990001)
            assertTitle("Navigation movie one")
            deliver(scenario, MediaType.MOVIE, 990001)
            assertTitle("Navigation movie one")
            // The scenario tracker can retain a stale lifecycle stage after an intent is consumed.
            var previous: MainActivity? = null
            scenario.onActivity {
                previous = it
                it.recreate()
            }
            composeRule.waitForIdle()
            scenario.onActivity { assertNotSame(previous, it) }
            assertTitle("Navigation movie one")
            deliver(scenario, MediaType.MOVIE, 990001)
            assertTitle("Navigation movie one")
            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            composeRule.onNodeWithText(context.getString(R.string.home_title)).assertIsDisplayed()
        }
    }

    @Test
    fun detailsRatingDraftSurvivesBackAndRealRecreationWithoutImplicitRoomWrites() = withCachedTitles { database ->
        fun savedRating() = runBlocking {
            database.ratingDao().observeRating(MediaSource.TMDB, MediaType.MOVIE, "990001").first()
        }
        runBlocking { database.ratingDao().setRating(MediaSource.TMDB, MediaType.MOVIE, "990001", 8, Instant.now()) }
        val originalRating = savedRating()
        assertEquals(8, originalRating?.ratingValue)
        fun slider(value: Int) = SemanticsMatcher.expectValue(
            SemanticsProperties.StateDescription,
            context.getString(R.string.detail_rating_slider, value)
        )
        ActivityScenario.launch<MainActivity>(Intent(context, MainActivity::class.java)).use { scenario ->
            fun recreateActivity() {
                var previous: MainActivity? = null
                scenario.onActivity {
                    previous = it
                    it.recreate()
                }
                composeRule.waitForIdle()
                scenario.onActivity { assertNotSame(previous, it) }
            }
            fun reach(matcher: SemanticsMatcher) {
                composeRule.onNode(hasScrollAction()).performScrollToNode(matcher)
            }
            composeRule.onNodeWithText(context.getString(R.string.home_title)).assertIsDisplayed()
            deliver(scenario, MediaType.MOVIE, 990001)
            assertTitle("Navigation movie one")
            val savedLabel = context.getString(R.string.detail_rating_value, 8)
            reach(hasText(savedLabel))
            composeRule.onNodeWithText(savedLabel).performClick()
            composeRule.onNode(slider(8)).performSemanticsAction(SemanticsActions.SetProgress) { it(9f) }
            deliver(scenario, MediaType.MOVIE, 990002)
            assertTitle("Navigation movie two")
            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            assertTitle("Navigation movie one")
            reach(slider(9))
            composeRule.onNode(slider(9)).assertIsDisplayed()
            assertEquals(originalRating, savedRating())
            recreateActivity()
            assertTitle("Navigation movie one")
            reach(slider(9))
            composeRule.onNode(slider(9)).assertIsDisplayed()
            assertEquals(originalRating, savedRating())
            composeRule.onNodeWithText(context.getString(R.string.action_cancel)).performScrollTo().performClick()
            assertEquals(originalRating, savedRating())
            reach(hasText(savedLabel))
            composeRule.onNodeWithText(savedLabel).performClick()
            composeRule.onNode(slider(8)).performSemanticsAction(SemanticsActions.SetProgress) { it(9f) }
            composeRule.onNodeWithText(context.getString(R.string.detail_rating_set)).performScrollTo().performClick()
            composeRule.waitUntil(5_000) { savedRating()?.ratingValue == 9 }
            val saved = savedRating()
            recreateActivity()
            val updatedLabel = context.getString(R.string.detail_rating_value, 9)
            reach(hasText(updatedLabel))
            composeRule.onNodeWithText(updatedLabel).assertIsDisplayed()
            composeRule.onNode(slider(9)).assertDoesNotExist()
            assertEquals(saved, savedRating())
            deliver(scenario, MediaType.MOVIE, 990002)
            assertTitle("Navigation movie two")
            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            assertTitle("Navigation movie one")
            reach(hasText(updatedLabel))
            composeRule.onNodeWithText(updatedLabel).assertIsDisplayed()
            composeRule.onNode(slider(9)).assertDoesNotExist()
            assertEquals(saved, savedRating())
        }
    }

    @Test
    fun savedSeasonsRemainReadableAfterFailedRefreshRetryAndActivityRecreation() = withCachedTitles { database ->
        val requireOffline = InstrumentationRegistry.getArguments().getString("requireOffline") == "true"
        fun checkOffline() {
            if (requireOffline) {
                assertEquals(null, context.getSystemService(ConnectivityManager::class.java).activeNetwork)
            }
        }
        checkOffline()
        val fetchedAt = Instant.EPOCH
        runBlocking {
            (1..2).forEach { number ->
                val season = SeasonEntity(
                    localMediaId = 0, source = MediaSource.TMDB, externalId = "offline-season-$number",
                    seasonNumber = number, name = "Offline season $number", overview = null, posterUrl = null,
                    airDate = LocalDate.of(2020, 1, 1), episodeCount = 1, metadataUpdatedAt = fetchedAt,
                    episodesFetchedAt = null
                )
                database.seriesDao().storeSeasonEpisodes(
                    MediaSource.TMDB,
                    "990001",
                    season,
                    listOf(
                        EpisodeEntity(
                            localSeasonId = 0, source = MediaSource.TMDB, externalId = "offline-episode-$number",
                            episodeNumber = 1, title = "Offline episode $number", overview = null,
                            airDate = LocalDate.of(2020, 1, 1), runtimeMinutes = 40, stillUrl = null,
                            metadataUpdatedAt = fetchedAt
                        )
                    ),
                    fetchedAt
                )
            }
        }
        fun savedRows() = runBlocking {
            database.seriesDao().observeSeriesSeasons(MediaSource.TMDB, "990001").first()
        }
        val original = savedRows()
        ActivityScenario.launch<MainActivity>(Intent(context, MainActivity::class.java)).use { scenario ->
            composeRule.onNodeWithText(context.getString(R.string.home_title)).assertIsDisplayed()
            deliver(scenario, MediaType.SERIES, 990001)
            assertTitle("Navigation series")
            fun reach(text: String) {
                composeRule.onNode(hasScrollAction()).performScrollToNode(hasText(text))
                composeRule.onNodeWithText(text).assertIsDisplayed()
            }
            fun activate(text: String) {
                reach(text)
                composeRule.onNodeWithText(text).performSemanticsAction(SemanticsActions.OnClick) { it() }
            }
            val error = context.getString(R.string.error_unauthorized)
            val retry = context.getString(R.string.action_retry)
            (1..2).forEach { number ->
                // Close the other season so error and retry belong unambiguously to this one.
                if (number == 2) activate("Offline season 1")
                activate("Offline season $number")
                // This isolated installation has no credential: refresh fails before any TMDB call.
                reach(error)
                reach(context.getString(R.string.detail_episode_title, 1, "Offline episode $number"))
                activate(retry)
                reach(error)
                reach(context.getString(R.string.detail_episode_title, 1, "Offline episode $number"))
                assertEquals(original, savedRows())
            }
            var previous: MainActivity? = null
            scenario.onActivity {
                previous = it
                it.recreate()
            }
            composeRule.waitUntil(5_000) {
                var changed = false
                scenario.onActivity { changed = it !== previous }
                changed
            }
            scenario.onActivity { assertNotSame(previous, it) }
            assertTitle("Navigation series")
            // Expansion is transient ViewModel state; open the saved season again if necessary.
            activate("Offline season 1")
            reach(context.getString(R.string.detail_episode_title, 1, "Offline episode 1"))
            assertEquals(original, savedRows())
            checkOffline()
        }
    }

    @Test
    fun malformedNotificationIntentLeavesLauncherBehaviorUnchanged() {
        val malformed = Intent(context, MainActivity::class.java).apply {
            action = NotificationDetailIntent.ACTION_OPEN_DETAILS
        }
        ActivityScenario.launch<MainActivity>(malformed).use {
            composeRule.onNodeWithText(context.getString(R.string.home_title)).assertIsDisplayed()
        }
    }

    @Test
    fun malformedProviderNotificationIntentDoesNotCrashOrOpenDetails() {
        val unsupported = Intent(context, MainActivity::class.java).apply {
            action = NotificationDetailIntent.ACTION_OPEN_DETAILS
            putExtra("notification_source", "IMDB")
            putExtra("notification_media_type", "MOVIE")
            putExtra("notification_external_id", "tt123")
        }
        ActivityScenario.launch<MainActivity>(unsupported).use {
            composeRule.onNodeWithText(context.getString(R.string.home_title)).assertIsDisplayed()
        }
    }

    private fun assertTitle(title: String) {
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithText(title).fetchSemanticsNodes().isNotEmpty() }
        val nodes = composeRule.onAllNodesWithText(title)
        assertTrue(
            "Expected visible title: $title",
            nodes.fetchSemanticsNodes().indices.any {
                nodes[it].isDisplayed()
            }
        )
    }

    private fun deliver(scenario: ActivityScenario<MainActivity>, type: MediaType, id: Long) {
        scenario.onActivity { activity ->
            InstrumentationRegistry.getInstrumentation().callActivityOnNewIntent(
                activity,
                NotificationDetailIntent.intent(context, DetailRouteArgs(type, id))
            )
        }
        // The title may already be visible on repeated delivery; wait for this intent to be consumed.
        composeRule.waitUntil(5_000) {
            var consumed = false
            scenario.onActivity { consumed = it.intent.action != NotificationDetailIntent.ACTION_OPEN_DETAILS }
            consumed
        }
    }

    private fun withCachedTitles(test: (BingeeDatabase) -> Unit) {
        val database = Room.databaseBuilder(context, BingeeDatabase::class.java, BingeeDatabase.DATABASE_NAME)
            .addMigrations(*ALL_MIGRATIONS)
            .build()
        val localIds = mutableListOf<Long>()
        try {
            runBlocking {
                val now = Instant.now()
                listOf(
                    Triple(MediaType.MOVIE, 990001L, "Navigation movie one"),
                    Triple(MediaType.SERIES, 990001L, "Navigation series"),
                    Triple(MediaType.MOVIE, 990002L, "Navigation movie two")
                ).forEach { (type, id, title) ->
                    database.detailsDao().storeDetails(
                        candidate = MediaEntity(
                            mediaType = type,
                            title = title,
                            originalTitle = null,
                            overview = "Cached fixture",
                            posterUrl = null,
                            releaseDate = null,
                            createdAt = now,
                            metadataUpdatedAt = now
                        ),
                        source = MediaSource.TMDB,
                        externalId = id.toString(),
                        details = MediaDetailsEntity(
                            localMediaId = 0, backdropUrl = null, productionStatus = "RELEASED",
                            originalLanguage = "en", runtimeMinutes = 100, episodeRuntimeMinutes = null,
                            numberOfSeasons = 0, numberOfEpisodes = 0, detailsFetchedAt = now, language = "en-US"
                        ),
                        genres = emptyList()
                    )
                    localIds += database.detailsDao().getCachedDetails(MediaSource.TMDB, type, id.toString())!!
                        .media.localMediaId
                }
            }
            test(database)
        } finally {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            localIds.forEach { id ->
                database.openHelper.writableDatabase.execSQL(
                    "DELETE FROM media_entries WHERE local_media_id = ?",
                    arrayOf(id)
                )
            }
            database.close()
        }
    }
}
