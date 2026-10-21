package com.cydoniancitizen.bingee.feature.details

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.test.performScrollToNode
import androidx.test.platform.app.InstrumentationRegistry
import com.cydoniancitizen.bingee.R
import com.cydoniancitizen.bingee.core.designsystem.theme.BingeeTheme
import com.cydoniancitizen.bingee.core.model.CacheFreshness
import com.cydoniancitizen.bingee.core.model.CachedSeason
import com.cydoniancitizen.bingee.core.model.EpisodeWatchState
import com.cydoniancitizen.bingee.core.model.LibraryProgress
import com.cydoniancitizen.bingee.core.model.MediaType
import com.cydoniancitizen.bingee.core.model.PersonalRating
import com.cydoniancitizen.bingee.core.model.ProductionStatus
import com.cydoniancitizen.bingee.core.model.deriveSeasonProgress
import com.cydoniancitizen.bingee.core.model.deriveSeriesProgress
import com.cydoniancitizen.bingee.core.result.AppError
import com.cydoniancitizen.bingee.debug.FakeMediaData
import com.cydoniancitizen.bingee.testutil.TestLocaleRule
import com.cydoniancitizen.bingee.testutil.localizedTestContext
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class MediaDetailsStateMatrixTest(private val caseIndex: Int, private val caseName: String) {
    @get:Rule(order = 0)
    val localeRule = TestLocaleRule()

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    @Test
    fun personalStateAndActionsMatchTheStoredFacts() {
        val case = matrix[caseIndex]
        val state = case.state
        val context = localizedTestContext
        val unsolicitedActions = AtomicInteger()
        composeRule.setContent {
            BingeeTheme(darkTheme = false) {
                MediaDetailsContent(
                    state,
                    onBack = {},
                    onRefresh = { unsolicitedActions.incrementAndGet() },
                    onRetry = { unsolicitedActions.incrementAndGet() },
                    onToggleLibrary = { unsolicitedActions.incrementAndGet() },
                    onToggleFavorite = { unsolicitedActions.incrementAndGet() },
                    onToggleMovieWatched = { unsolicitedActions.incrementAndGet() },
                    onToggleSeriesAbandoned = { unsolicitedActions.incrementAndGet() },
                    onSetWatchedDate = { unsolicitedActions.incrementAndGet() },
                    onSelectRating = { unsolicitedActions.incrementAndGet() },
                    onSaveRating = { unsolicitedActions.incrementAndGet() },
                    onRemoveRating = { unsolicitedActions.incrementAndGet() },
                    onDismissLibraryError = {},
                    onOpenSettings = {}
                )
            }
        }
        val favorite = context.getString(if (state.isFavorite) R.string.favorite_remove else R.string.favorite_add)
        composeRule.onNodeWithContentDescription(favorite).assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.ToggleableState,
                if (state.isFavorite) ToggleableState.On else ToggleableState.Off
            )
        )
        fun reach(text: String) {
            composeRule.onNode(hasScrollAction()).performScrollToNode(hasText(text))
            composeRule.onNodeWithText(text).assertIsDisplayed()
        }
        val member = state.isInLibrary == true
        reach(context.getString(if (member) R.string.library_action_remove else R.string.library_action_add))
        reach(context.getString(case.label))
        val args = InstrumentationRegistry.getArguments()
        if (args.getString("captureStates") == "true" && caseIndex % 8 == 0) {
            val target = InstrumentationRegistry.getInstrumentation().targetContext
            File(target.filesDir, "m5-state-$caseIndex.png").outputStream().use {
                composeRule.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
        val rating = state.rating as DetailRatingState.Ready
        reach(
            rating.rating?.let { context.getString(R.string.detail_rating_value, it.value) }
                ?: context.getString(R.string.detail_rating_unrated)
        )
        composeRule.onAllNodesWithText(context.getString(R.string.detail_rating_set)).assertCountEquals(0)
        composeRule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
            .assertCountEquals(if (state.refresh == DetailRefreshState.Refreshing) 1 else 0)
        val details = (state.content as DetailContentState.Content).cached.details
        if (details.mediaType == MediaType.MOVIE) {
            val watched = case.label == R.string.detail_movie_watched
            reach(context.getString(if (watched) R.string.detail_mark_unwatched else R.string.detail_mark_watched))
        } else {
            val action = context.getString(
                if (state.isAbandoned) R.string.series_tracking_restore else R.string.series_tracking_abandon
            )
            if (member) reach(action) else composeRule.onAllNodesWithText(action).assertCountEquals(0)
            if (case.incomplete) {
                reach(context.getString(R.string.detail_tracking_incomplete))
            } else {
                composeRule.onAllNodesWithText(
                    context.getString(R.string.detail_tracking_incomplete)
                ).assertCountEquals(0)
            }
        }
        if (case.banner) {
            composeRule.onNode(hasScrollAction()).performScrollToKey("banner")
            val message = if (state.refresh is DetailRefreshState.Error) {
                context.getString(R.string.detail_refresh_failed, context.getString(R.string.error_network_unavailable))
            } else {
                context.getString(R.string.detail_stale_data)
            }
            composeRule.onNodeWithText(message).assertIsDisplayed()
        }
        assertEquals("No writes or requests while reading $caseName", 0, unsolicitedActions.get())
    }

    private data class Case(
        val name: String,
        val state: MediaDetailsUiState,
        val label: Int,
        val incomplete: Boolean = false,
        val banner: Boolean = false
    )

    companion object {
        private fun withSeasons(
            base: MediaDetailsUiState,
            seasons: List<CachedSeason>,
            status: ProductionStatus = ProductionStatus.RETURNING_SERIES
        ): MediaDetailsUiState {
            val cached = (base.content as DetailContentState.Content).cached
            val progress = deriveSeriesProgress(seasons)
            return base.copy(
                content = DetailContentState.Content(
                    cached.copy(
                        details = cached.details.copy(productionStatus = status),
                        freshness = CacheFreshness.FRESH
                    )
                ),
                libraryProgress = LibraryProgress.Series(progress),
                series = base.series.copy(content = SeriesContentState.Ready(seasons, progress))
            )
        }

        private val matrix: List<Case> = buildList {
            val preview = DetailPersonalPreviewProvider().values.toList()
            val watching = preview[2]
            val regular = (watching.series.content as SeriesContentState.Ready).seasons
            val unwatched = regular.map { season ->
                val episodes = season.episodes.map {
                    if (it.watchState is EpisodeWatchState.Watched) {
                        it.copy(watchState = EpisodeWatchState.Unwatched)
                    } else {
                        it
                    }
                }
                season.copy(episodes = episodes, progress = deriveSeasonProgress(episodes))
            }
            val completed = (preview[3].series.content as SeriesContentState.Ready).seasons
            val ended = completed.map { season ->
                val episodes = season.episodes.filter { it.watchState != EpisodeWatchState.Unavailable }
                season.copy(
                    season = season.season.copy(episodeCount = episodes.size),
                    episodes = episodes,
                    progress = deriveSeasonProgress(episodes)
                )
            }
            val core = listOf(
                Case("movie-unwatched", preview[0], R.string.detail_movie_unwatched),
                Case("movie-watched", preview[1], R.string.detail_movie_watched),
                Case("series-watch-later", withSeasons(watching, unwatched), R.string.detail_tracking_watch_later),
                Case("series-watching", withSeasons(watching, regular), R.string.detail_tracking_watching),
                Case("series-caught-up", withSeasons(preview[3], completed), R.string.detail_tracking_caught_up),
                Case(
                    "series-watched",
                    withSeasons(preview[3], ended, ProductionStatus.ENDED),
                    R.string.detail_tracking_watched
                ),
                Case("series-abandoned", withSeasons(preview[4], regular), R.string.detail_tracking_abandoned)
            )
            core.forEach { case ->
                val series =
                    (case.state.content as DetailContentState.Content).cached.details.mediaType == MediaType.SERIES
                listOf(false, true).filter { it || !case.state.isAbandoned }.forEach { member ->
                    listOf(false, true).forEach { rated ->
                        listOf(false, true).forEach { favorite ->
                            add(
                                case.copy(
                                    name = "${case.name}/member=$member/rated=$rated/favorite=$favorite",
                                    state = case.state.copy(
                                        isInLibrary = member,
                                        isFavorite = favorite,
                                        rating = DetailRatingState.Ready(if (rated) PersonalRating(8) else null)
                                    ),
                                    label = if (series && !member) R.string.detail_tracking_outside else case.label
                                )
                            )
                        }
                    }
                }
            }
            add(
                Case(
                    "canceled-watched",
                    withSeasons(preview[3], ended, ProductionStatus.CANCELED),
                    R.string.detail_tracking_watched
                )
            )
            listOf(ProductionStatus.ENDED, ProductionStatus.CANCELED, ProductionStatus.UNKNOWN).forEach { status ->
                add(
                    Case(
                        "neutral-completion-$status",
                        withSeasons(preview[3], completed, status),
                        R.string.detail_tracking_available_watched
                    )
                )
            }
            val partial = (preview[6].series.content as SeriesContentState.Ready).seasons
            listOf(false, true).forEach { member ->
                add(
                    Case(
                        "incomplete/member=$member",
                        withSeasons(preview[6], partial).copy(isInLibrary = member),
                        if (member) R.string.detail_tracking_watching else R.string.detail_tracking_outside,
                        incomplete = true
                    )
                )
            }
            add(
                Case(
                    "missing-season-rows",
                    withSeasons(watching, emptyList()),
                    R.string.detail_tracking_watch_later,
                    true
                )
            )
            val futureOnly = regular.map { season ->
                val episodes = season.episodes.filter { it.watchState == EpisodeWatchState.Unavailable }
                season.copy(
                    season = season.season.copy(episodeCount = episodes.size),
                    episodes = episodes,
                    progress = deriveSeasonProgress(episodes)
                )
            }
            add(Case("future-only", withSeasons(watching, futureOnly), R.string.detail_tracking_watch_later))
            val special = FakeMediaData.previewSeasons.first { it.season.seasonNumber == 0 }
            add(
                Case(
                    "unwatched-special-keeps-regular-completion",
                    withSeasons(preview[3], ended + special, ProductionStatus.ENDED),
                    R.string.detail_tracking_watched
                )
            )
            listOf(preview[0], watching).forEach { base ->
                listOf(
                    DetailRefreshState.Idle,
                    DetailRefreshState.Refreshing,
                    DetailRefreshState.Error(AppError.NetworkUnavailable)
                ).forEach { refresh ->
                    val cached = (base.content as DetailContentState.Content).cached
                    add(
                        Case(
                            "stale-${cached.details.mediaType}-$refresh",
                            base.copy(
                                content = DetailContentState.Content(cached.copy(freshness = CacheFreshness.STALE)),
                                refresh = refresh
                            ),
                            if (cached.details.mediaType == MediaType.MOVIE) {
                                R.string.detail_movie_unwatched
                            } else {
                                R.string.detail_tracking_watching
                            },
                            banner = true
                        )
                    )
                }
            }
        }

        @JvmStatic
        @Parameterized.Parameters(name = "{0}: {1}")
        fun cases() = matrix.mapIndexed { index, case -> arrayOf<Any>(index, case.name) }
    }
}
