package com.cydoniancitizen.bingee.feature.details

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.cydoniancitizen.bingee.R
import com.cydoniancitizen.bingee.core.designsystem.theme.BingeeTheme
import com.cydoniancitizen.bingee.core.model.CacheFreshness
import com.cydoniancitizen.bingee.core.model.CachedSeason
import com.cydoniancitizen.bingee.core.model.Episode
import com.cydoniancitizen.bingee.core.model.EpisodeWatchState
import com.cydoniancitizen.bingee.core.model.ExternalMediaRef
import com.cydoniancitizen.bingee.core.model.MediaSource
import com.cydoniancitizen.bingee.core.model.Season
import com.cydoniancitizen.bingee.core.model.SeasonProgress
import com.cydoniancitizen.bingee.core.model.SeriesProgress
import com.cydoniancitizen.bingee.core.model.TrackedEpisode
import com.cydoniancitizen.bingee.core.result.AppError
import com.cydoniancitizen.bingee.testutil.TestLocaleRule
import com.cydoniancitizen.bingee.testutil.localizedTestContext
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class EpisodeDetailsScreenTest {
    @get:Rule(order = 0)
    val locale = TestLocaleRule()

    @get:Rule(order = 1)
    val compose = createComposeRule()
    private val context get() = localizedTestContext
    private fun text(id: Int) = context.getString(id)
    private val original = Instant.parse("2026-08-02T12:00:00Z")

    @Test fun largeFontKeepsDateActionsAndWriteErrorReachable() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.5f)) {
                Box(Modifier.width(320.dp)) {
                    BingeeTheme(darkTheme = true) {
                        EpisodeDetailsContent(
                            state(
                                EpisodeWatchState.Watched(original)
                            ).copy(progressError = AppError.LocalStorageFailure),
                            "101",
                            {},
                            {},
                            { _, _ -> }
                        )
                    }
                }
            }
        }
        compose.onNodeWithText(text(R.string.watched_date_edit)).performScrollTo().performClick()
        compose.onNodeWithText(text(R.string.action_cancel)).performClick()
        compose.onNodeWithText(text(R.string.error_local_storage)).performScrollTo().assertIsDisplayed()
    }

    @Test fun episodeDateDraftSurvivesRecreationBeforeSave() {
        var saved: LocalDate? = null
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            BingeeTheme {
                EpisodeDetailsContent(
                    state(EpisodeWatchState.Watched(original)),
                    "101",
                    {},
                    {},
                    onSetWatchedDate = { _, date -> saved = date }
                )
            }
        }
        compose.onNodeWithText(text(R.string.watched_date_edit)).performScrollTo().performClick()
        compose.onNodeWithText(text(R.string.watched_date_today)).performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText(text(R.string.action_save)).performClick()
        compose.runOnIdle { assertEquals(LocalDate.of(2026, 8, 3), saved) }
    }

    @Test fun episodeShowsSavedDateAndCancelDoesNotWrite() {
        var writes = 0
        compose.setContent {
            BingeeTheme {
                EpisodeDetailsContent(
                    state(EpisodeWatchState.Watched(original)),
                    "101",
                    {},
                    {},
                    onSetWatchedDate = { _, _ -> writes++ }
                )
            }
        }
        compose.onNodeWithText("Episode plot").assertIsDisplayed()
        compose.onNodeWithText(text(R.string.watched_date_edit)).performScrollTo().performClick()
        compose.onNodeWithText(text(R.string.action_cancel)).performClick()
        compose.runOnIdle { assertEquals(0, writes) }
        compose.onNodeWithText(text(R.string.watched_date_edit)).performScrollTo().performClick()
        compose.onNodeWithText(text(R.string.watched_date_today)).performClick()
        compose.onNodeWithText(text(R.string.action_save)).performClick()
        compose.runOnIdle { assertEquals(1, writes) }
    }

    @Test fun futureEpisodeKeepsSpoilersHiddenAndOffersNoDateEdit() {
        compose.setContent {
            BingeeTheme {
                EpisodeDetailsContent(state(EpisodeWatchState.Unavailable, hide = true), "101", {}, {}, { _, _ -> })
            }
        }
        compose.onNodeWithText("Secret title").assertDoesNotExist()
        compose.onNodeWithText("Episode plot").assertDoesNotExist()
        compose.onNodeWithText(text(R.string.watched_date_edit)).assertDoesNotExist()
        compose.onNodeWithText(text(R.string.detail_mark_episode_watched)).assertIsNotEnabled()
    }

    private fun state(watch: EpisodeWatchState, hide: Boolean = false): MediaDetailsUiState {
        val series = ExternalMediaRef(MediaSource.TMDB, "100")
        val season = ExternalMediaRef(MediaSource.TMDB, "11")
        val episode = TrackedEpisode(
            Episode(series, season, ExternalMediaRef(MediaSource.TMDB, "101"), 1, 1, "Secret title", "Episode plot"),
            watch
        )
        return MediaDetailsUiState(
            today = LocalDate.of(2026, 8, 3),
            series = SeriesDetailUiState(
                content = SeriesContentState.Ready(
                    listOf(
                        CachedSeason(
                            Season(series, season, 1, episodeCount = 1),
                            original,
                            original,
                            listOf(episode),
                            SeasonProgress(1, 1, true),
                            CacheFreshness.FRESH
                        )
                    ),
                    SeriesProgress(1, 1, 1, 1, true)
                ),
                hideSpoilers = hide
            )
        )
    }
}
