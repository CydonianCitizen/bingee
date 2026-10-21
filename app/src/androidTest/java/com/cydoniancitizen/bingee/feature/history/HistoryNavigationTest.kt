package com.cydoniancitizen.bingee.feature.history

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.cydoniancitizen.bingee.MainActivity
import com.cydoniancitizen.bingee.R
import com.cydoniancitizen.bingee.core.model.MediaSource
import com.cydoniancitizen.bingee.core.model.MediaType
import com.cydoniancitizen.bingee.data.library.local.ALL_MIGRATIONS
import com.cydoniancitizen.bingee.data.library.local.BingeeDatabase
import com.cydoniancitizen.bingee.data.library.local.MediaDetailsEntity
import com.cydoniancitizen.bingee.data.library.local.MediaEntity
import com.cydoniancitizen.bingee.data.settings.bingeePreferences
import com.cydoniancitizen.bingee.testutil.TestLocaleRule
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class HistoryNavigationTest {
    @get:Rule(order = 0)
    val locale = TestLocaleRule()

    @get:Rule(order = 1)
    val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var db: BingeeDatabase
    private val ids = mutableListOf<Long>()

    @Before fun seed() = runBlocking {
        context.bingeePreferences.edit { it[booleanPreferencesKey("onboarding_complete")] = true }
        db = Room.databaseBuilder(context, BingeeDatabase::class.java, BingeeDatabase.DATABASE_NAME)
            .addMigrations(*ALL_MIGRATIONS).build()
        val now = Instant.now()
        for (index in 1..23) {
            val ref = (99130000 + index).toString()
            db.detailsDao().storeDetails(
                candidate = MediaEntity(
                    mediaType = MediaType.MOVIE,
                    title = "History navigation movie $index",
                    originalTitle = null,
                    overview = "Synthetic local navigation fixture",
                    posterUrl = null,
                    releaseDate = null,
                    createdAt = now,
                    metadataUpdatedAt = now
                ),
                source = MediaSource.TMDB,
                externalId = ref,
                details = MediaDetailsEntity(0, null, "RELEASED", "en", 90, null, 0, 0, now, "en-US"),
                genres = emptyList()
            )
            ids += db.detailsDao().getCachedDetails(MediaSource.TMDB, MediaType.MOVIE, ref)!!.media.localMediaId
            db.watchProgressDao().markMovieWatched(MediaSource.TMDB, ref, now)
        }
    }

    @After fun clean() {
        if (::db.isInitialized) {
            ids.forEach { id ->
                db.openHelper.writableDatabase.execSQL("DELETE FROM media_entries WHERE local_media_id=?", arrayOf(id))
            }
            db.close()
        }
    }

    @Test fun historyReturnsFromDetailsAndRecreationWithMovieFilterAndScroll() {
        ActivityScenario.launch<MainActivity>(Intent(context, MainActivity::class.java)).use { scenario ->
            compose.onNodeWithText(context.getString(R.string.profile_title_dashboard)).performClick()
            compose.waitUntil(10000) {
                compose.onNodeWithText(context.getString(R.string.history_title)).isDisplayed()
            }
            compose.onNodeWithText(context.getString(R.string.history_title)).performClick()
            compose.waitUntil(15000) { compose.onAllNodesWithTag("history-list").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText(context.getString(R.string.history_movies)).performClick()
            compose.waitUntil(10000) { compose.onAllNodesWithTag("history-list").fetchSemanticsNodes().isNotEmpty() }
            val title = "History navigation movie 23"
            compose.onNodeWithTag("history-list").performScrollToNode(hasText(title))
            compose.onNodeWithText(title).performClick()
            compose.waitUntil(10000) {
                compose.onNodeWithContentDescription(context.getString(R.string.detail_refresh)).isDisplayed()
            }
            compose.waitUntil(10000) { compose.onAllNodesWithTag("history-list").fetchSemanticsNodes().isEmpty() }
            compose.onAllNodesWithText(title).onFirst().assertExists()
            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            compose.waitUntil(10000) {
                compose.onAllNodesWithText(title).fetchSemanticsNodes().size == 1 &&
                    compose.onNodeWithText(title).isDisplayed()
            }
            compose.onNodeWithText(context.getString(R.string.history_movies)).assertIsSelected()
            scenario.recreate()
            compose.waitUntil(10000) {
                compose.onAllNodesWithText(title).fetchSemanticsNodes().size == 1 &&
                    compose.onNodeWithText(title).isDisplayed()
            }
            compose.onNodeWithText(context.getString(R.string.history_movies)).assertIsSelected()
        }
    }
}
