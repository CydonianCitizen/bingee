package com.cydoniancitizen.bingee.feature.details

import android.app.UiAutomation
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import android.util.DisplayMetrics
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.cydoniancitizen.bingee.R
import com.cydoniancitizen.bingee.core.designsystem.theme.BingeeTheme
import com.cydoniancitizen.bingee.core.model.CacheFreshness
import com.cydoniancitizen.bingee.core.model.CachedMediaDetails
import com.cydoniancitizen.bingee.core.model.CachedSeason
import com.cydoniancitizen.bingee.core.model.Episode
import com.cydoniancitizen.bingee.core.model.EpisodePosition
import com.cydoniancitizen.bingee.core.model.EpisodeWatchState
import com.cydoniancitizen.bingee.core.model.ExternalMediaRef
import com.cydoniancitizen.bingee.core.model.Genre
import com.cydoniancitizen.bingee.core.model.LibraryProgress
import com.cydoniancitizen.bingee.core.model.MediaDetails
import com.cydoniancitizen.bingee.core.model.MediaSource
import com.cydoniancitizen.bingee.core.model.MediaType
import com.cydoniancitizen.bingee.core.model.MovieWatchState
import com.cydoniancitizen.bingee.core.model.PersonalRating
import com.cydoniancitizen.bingee.core.model.ProductionStatus
import com.cydoniancitizen.bingee.core.model.Season
import com.cydoniancitizen.bingee.core.model.SeasonProgress
import com.cydoniancitizen.bingee.core.model.SeriesProgress
import com.cydoniancitizen.bingee.core.model.TrackedEpisode
import com.cydoniancitizen.bingee.core.model.deriveSeasonProgress
import com.cydoniancitizen.bingee.core.model.deriveSeriesProgress
import com.cydoniancitizen.bingee.core.result.AppError
import com.cydoniancitizen.bingee.testutil.TestLocaleRule
import com.cydoniancitizen.bingee.testutil.localizedTestContext
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource

class MediaDetailsScreenTest {
    @get:Rule(order = 0)
    val localeRule = TestLocaleRule()

    private val context get() = localizedTestContext
    private val savedResources = mutableMapOf<android.content.res.Resources, Pair<Configuration, DisplayMetrics>>()

    @get:Rule(order = 1)
    val resourceConfiguration = object : ExternalResource() {
        @Suppress("DEPRECATION")
        override fun after() {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                savedResources.forEach { (resource, original) ->
                    resource.updateConfiguration(original.first, original.second)
                }
                savedResources.clear()
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun configureTestResources(width: Int, font: Float, dark: Boolean) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val resources = (
                ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
                    .map { it.resources } + instrumentation.targetContext.resources
                ).distinct()
            resources.forEach { resource ->
                savedResources.putIfAbsent(
                    resource,
                    Configuration(resource.configuration) to DisplayMetrics().apply { setTo(resource.displayMetrics) }
                )
            }
            resources.forEach { resource ->
                val configuration = Configuration(resource.configuration).apply {
                    screenWidthDp = width
                    densityDpi = (resource.displayMetrics.widthPixels * 160f / width).toInt()
                    fontScale = font
                    uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                        if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
                }
                resource.updateConfiguration(configuration, resource.displayMetrics)
            }
        }
    }

    @get:Rule(order = 2)
    val composeRule = createComposeRule()

    @Test
    fun abandonedSeriesResumeIsPrimaryAndKeepsThePendingActionGuard() {
        val fixture = DetailPersonalPreviewProvider().values.elementAt(4)
        var pending by mutableStateOf(false)
        var abandoned by mutableStateOf(true)
        var primary = Color.Unspecified
        val actions = AtomicInteger()
        composeRule.setContent {
            BingeeTheme {
                primary = MaterialTheme.colorScheme.primary
                MediaDetailsContent(
                    fixture.copy(
                        isAbandoned = abandoned,
                        libraryAction = if (pending) {
                            DetailLibraryActionState.UPDATING
                        } else {
                            DetailLibraryActionState.IDLE
                        }
                    ),
                    onBack = {},
                    onRefresh = {},
                    onRetry = {},
                    onToggleLibrary = {},
                    onDismissLibraryError = {},
                    onOpenSettings = {},
                    onToggleSeriesAbandoned = {
                        actions.incrementAndGet()
                        pending = true
                    }
                )
            }
        }
        val resumeLabel = context.getString(R.string.series_tracking_restore)
        scrollTo(hasText(resumeLabel))
        composeRule.onAllNodesWithText(resumeLabel).assertCountEquals(1)
        val resume = composeRule.onNodeWithText(resumeLabel)
        val resumePixels = resume.captureToImage().toPixelMap()
        assertEquals(
            "Resume must use the primary filled button",
            primary.toArgb(),
            resumePixels[resumePixels.width / 2, (resumePixels.height * 0.15f).toInt()].toArgb()
        )
        resume.performClick()
        resume.assertIsNotEnabled()
        resume.performTouchInput { click() }
        assertEquals(1, actions.get())
        composeRule.runOnIdle {
            pending = false
            abandoned = false
        }
        composeRule.onAllNodesWithText(resumeLabel).assertCountEquals(0)
        val abandonLabel = context.getString(R.string.series_tracking_abandon)
        scrollTo(hasText(abandonLabel))
        val abandon = composeRule.onNodeWithText(abandonLabel)
        val abandonPixels = abandon.captureToImage().toPixelMap()
        assertNotEquals(
            "Abandon must remain secondary",
            primary.toArgb(),
            abandonPixels[abandonPixels.width / 2, (abandonPixels.height * 0.15f).toInt()].toArgb()
        )
        abandon.performClick()
        assertEquals(2, actions.get())
    }

    @Test
    fun membershipReadErrorHasAnExplicitRetryInsteadOfPermanentLoading() {
        val retried = AtomicBoolean(false)
        composeRule.setContent {
            BingeeTheme {
                MediaDetailsContent(
                    state = content(movie()).copy(isInLibrary = null, membershipError = AppError.LocalStorageFailure),
                    onBack = {},
                    onRefresh = {},
                    onRetry = {},
                    onToggleLibrary = {},
                    onDismissLibraryError = {},
                    onOpenSettings = {},
                    onRetryMembership = { retried.set(true) }
                )
            }
        }
        scrollTo(hasText(context.getString(R.string.error_local_storage)))
        composeRule.onNodeWithText(context.getString(R.string.library_action_updating)).assertDoesNotExist()
        composeRule.onNodeWithText(context.getString(R.string.action_retry)).performScrollTo().performClick()
        assertTrue(retried.get())
    }

    @Test
    fun ratingEditorRestoresWithoutSavingOrResettingItsDraft() {
        val restoration = StateRestorationTester(composeRule)
        var rating by mutableStateOf(DetailRatingState.Ready(PersonalRating(8)))
        var saves = 0
        restoration.setContent {
            BingeeTheme {
                RatingSection(rating, { rating = rating.copy(selectedValue = it) }, { saves++ }, {}, {})
            }
        }
        composeRule.onNodeWithText(context.getString(R.string.detail_rating_value, 8)).performClick()
        composeRule.onNode(
            SemanticsMatcher.expectValue(
                SemanticsProperties.StateDescription,
                context.getString(R.string.detail_rating_slider, 8)
            )
        ).performSemanticsAction(SemanticsActions.SetProgress) { it(9f) }
        restoration.emulateSavedInstanceStateRestore()
        composeRule.onNode(
            SemanticsMatcher.expectValue(
                SemanticsProperties.StateDescription,
                context.getString(R.string.detail_rating_slider, 9)
            )
        ).assertIsDisplayed()
        assertEquals(0, saves)
    }

    @Test
    fun ratingEditorStartsCollapsedAndOpensOnExplicitIntent() {
        setDetails(content(movie()).copy(rating = DetailRatingState.Ready(PersonalRating(8))))
        val value = context.getString(R.string.detail_rating_value, 8)
        scrollTo(hasText(value))
        val slider = SemanticsMatcher.expectValue(
            SemanticsProperties.StateDescription,
            context.getString(R.string.detail_rating_slider, 8)
        )
        composeRule.onNode(slider).assertDoesNotExist()
        composeRule.onNodeWithText(value).performClick()
        composeRule.onNode(slider).assertIsDisplayed()
    }

    @Test
    fun ratingFailureKeepsDraftCancelRestoresItAndSuccessCollapsesEditor() {
        var state by mutableStateOf(content(movie()).copy(rating = DetailRatingState.Ready(PersonalRating(8))))
        var failSave = true
        var saves = 0
        setDetailsState({ state }, onSelectRating = { selected ->
            val ready = state.rating as DetailRatingState.Ready
            state = state.copy(rating = ready.copy(selectedValue = selected, error = null))
        }, onSaveRating = {
            saves++
            val ready = state.rating as DetailRatingState.Ready
            state = state.copy(
                rating = if (failSave) {
                    ready.copy(error = AppError.LocalStorageFailure)
                } else {
                    ready.copy(rating = PersonalRating(ready.selectedValue), error = null)
                }
            )
        })
        val original = context.getString(R.string.detail_rating_value, 8)
        scrollTo(hasText(original))
        composeRule.onNodeWithText(original).performClick()
        fun selectNine() {
            composeRule.onNode(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    context.getString(R.string.detail_rating_slider, 8)
                )
            ).performSemanticsAction(SemanticsActions.SetProgress) { it(9f) }
        }
        selectNine()
        composeRule.onNodeWithText(context.getString(R.string.detail_rating_set)).performScrollTo().performClick()
        composeRule.onNodeWithText(
            context.getString(R.string.error_local_storage)
        ).performScrollTo().assertIsDisplayed()
        assertEquals(9, (state.rating as DetailRatingState.Ready).selectedValue)
        composeRule.onNodeWithText(context.getString(R.string.action_cancel)).performScrollTo().performClick()
        assertEquals(8, (state.rating as DetailRatingState.Ready).selectedValue)
        assertEquals(1, saves)
        failSave = false
        composeRule.onNodeWithText(original).performScrollTo().performClick()
        selectNine()
        composeRule.onNodeWithText(context.getString(R.string.detail_rating_set)).performScrollTo().performClick()
        composeRule.waitForIdle()
        scrollTo(hasText(context.getString(R.string.detail_rating_value, 9)))
        composeRule.onNodeWithText(context.getString(R.string.detail_rating_value, 9)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.detail_rating_set)).assertDoesNotExist()
        assertEquals(2, saves)
    }

    @Test
    fun ongoingSeriesUsesAvailableEpisodeCompletionWordingAndKeepsAutomaticDate() {
        val cached = fetchedSeason(1, "Season 1", watched = 2, trackable = 2)
        setDetails(seriesState(listOf(cached)).copy(isInLibrary = true, seriesCompletedAt = Instant.EPOCH))
        val label = context.getString(R.string.available_completion_date_edit)
        scrollTo(hasText(label))
        composeRule.onNodeWithText(label).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.completion_date_edit)).assertDoesNotExist()
        composeRule.onNodeWithText(context.getString(R.string.watched_date_clear)).assertDoesNotExist()
    }

    @Test
    fun ratingPendingWritesRejectExtraTouchesAndRemovalErrorKeepsDraft() {
        var rating by mutableStateOf(DetailRatingState.Ready(PersonalRating(8)))
        var saves = 0
        var removals = 0
        composeRule.setContent {
            BingeeTheme {
                RatingSection(
                    rating,
                    onSelect = { rating = rating.copy(selectedValue = it, error = null) },
                    onSave = {
                        saves++
                        rating = rating.copy(updating = true)
                    },
                    onRemove = {
                        removals++
                        rating = rating.copy(updating = true)
                    },
                    onDismissError = { rating = rating.copy(error = null) }
                )
            }
        }
        val saved = context.getString(R.string.detail_rating_value, 8)
        val remove = context.getString(R.string.detail_rating_remove)
        val cancel = context.getString(R.string.action_cancel)
        composeRule.onNodeWithText(saved).performClick()
        composeRule.onNode(
            SemanticsMatcher.expectValue(
                SemanticsProperties.StateDescription,
                context.getString(R.string.detail_rating_slider, 8)
            )
        ).performSemanticsAction(SemanticsActions.SetProgress) { it(9f) }
        composeRule.onNodeWithText(context.getString(R.string.detail_rating_set)).performClick()
        composeRule.onNodeWithText(context.getString(R.string.detail_rating_updating))
            .assertIsNotEnabled().performTouchInput { click() }
        composeRule.onNodeWithText(remove).assertIsNotEnabled().performTouchInput { click() }
        assertEquals(1, saves)
        assertEquals(0, removals)
        composeRule.runOnIdle { rating = rating.copy(updating = false, error = AppError.LocalStorageFailure) }
        composeRule.onNodeWithText(context.getString(R.string.error_local_storage)).assertIsDisplayed()
        assertEquals(9, rating.selectedValue)
        composeRule.onNodeWithText(remove).performClick()
        composeRule.onNodeWithText(remove).assertIsNotEnabled().performTouchInput { click() }
        composeRule.onNodeWithText(cancel).assertIsNotEnabled()
        assertEquals(1, removals)
        composeRule.runOnIdle { rating = rating.copy(updating = false, error = AppError.LocalStorageFailure) }
        composeRule.onNodeWithText(context.getString(R.string.error_local_storage)).assertIsDisplayed()
        assertEquals(PersonalRating(8), rating.rating)
        assertEquals(9, rating.selectedValue)
        composeRule.onNodeWithText(cancel).performClick()
        composeRule.onNodeWithText(context.getString(R.string.detail_rating_set)).assertDoesNotExist()
        assertEquals(8, rating.selectedValue)
        composeRule.onNodeWithText(saved).performClick()
        composeRule.onNodeWithText(remove).performClick()
        composeRule.runOnIdle { rating = DetailRatingState.Ready(null) }
        composeRule.onNodeWithText(context.getString(R.string.detail_rating_unrated)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.detail_rating_set)).assertDoesNotExist()
        assertEquals(1, saves)
        assertEquals(2, removals)
    }

    @Test
    fun incompleteSeriesDoesNotOfferAnIgnoredCompletionDateSave() {
        setDetails(
            seriesState(listOf(fetchedSeason(1, "Season 1", watched = 1, trackable = 2)))
                .copy(isInLibrary = true)
        )
        assertThrows(AssertionError::class.java) {
            scrollTo(hasText(context.getString(R.string.completion_date_edit)))
        }
    }

    @Test
    fun interruptedEpisodeNavigationDoesNotRestartWhenSeasonDataUpdates() {
        val cached = fetchedSeason(1, "Long season", watched = 49, trackable = 50)
        var state by mutableStateOf(
            seriesState(listOf(cached)).copy(
                isInLibrary = true,
                libraryProgress = LibraryProgress.Series(
                    deriveSeriesProgress(listOf(cached)).copy(nextEpisode = EpisodePosition(1, 50))
                )
            )
        )
        setDetailsState({ state }, onToggleSeasonExpanded = {
            state = state.copy(series = state.series.copy(expandedSeasons = setOf(cached.season.externalRef)))
        })
        scrollTo(hasText(context.getString(R.string.detail_go_to_episode)))
        composeRule.mainClock.autoAdvance = false
        composeRule.onNodeWithText(context.getString(R.string.detail_go_to_episode)).performClick()
        composeRule.mainClock.advanceTimeBy(32)
        composeRule.onNode(hasScrollAction()).performTouchInput { swipeDown() }
        composeRule.mainClock.advanceTimeBy(1000)
        val before = composeRule.onNode(hasScrollAction()).fetchSemanticsNode()
            .config[SemanticsProperties.VerticalScrollAxisRange].value()
        composeRule.runOnIdle {
            val ready = state.series.content as SeriesContentState.Ready
            state = state.copy(
                series = state.series.copy(
                    content = ready.copy(
                        seasons = listOf(cached.copy(metadataUpdatedAt = Instant.ofEpochSecond(100)))
                    )
                )
            )
        }
        composeRule.mainClock.advanceTimeBy(1000)
        val after = composeRule.onNode(hasScrollAction()).fetchSemanticsNode()
            .config[SemanticsProperties.VerticalScrollAxisRange].value()
        assertEquals(before, after, 0.001f)
        composeRule.mainClock.autoAdvance = true
    }

    @Test
    fun nextEpisodeNavigationExpandsOnlyItsSeasonAndReachesTheLazyRow() {
        val cached = fetchedSeason(1, "Long season", watched = 49, trackable = 50)
            .copy(episodeCacheFreshness = CacheFreshness.STALE)
        var state by mutableStateOf(
            seriesState(listOf(cached)).copy(
                isInLibrary = true,
                libraryProgress = LibraryProgress.Series(
                    deriveSeriesProgress(listOf(cached)).copy(
                        nextEpisode = EpisodePosition(1, 50)
                    )
                )
            )
        )
        val toggled = AtomicBoolean(false)
        val expansions = AtomicInteger()
        val retries = AtomicInteger()
        setDetailsState({ state }, onToggleSeasonExpanded = { season ->
            expansions.incrementAndGet()
            state = state.copy(
                series = state.series.copy(
                    expandedSeasons = state.series.expandedSeasons + season.season.externalRef,
                    seasonLoads = mapOf(season.season.externalRef to SeasonLoadState.Refreshing)
                )
            )
        }, onToggleEpisode = { toggled.set(it.episode.episodeNumber == 50) }, onRetrySeason = {
            retries.incrementAndGet()
            state = state.copy(
                series = state.series.copy(seasonLoads = mapOf(cached.season.externalRef to SeasonLoadState.Refreshing))
            )
        })
        scrollTo(hasText(context.getString(R.string.detail_go_to_episode)))
        composeRule.onNodeWithText(context.getString(R.string.detail_go_to_episode)).performClick()
        composeRule.waitForIdle()
        val label = context.getString(R.string.detail_episode_title, 50, "Long season episode 50")
        composeRule.onNodeWithText(label).assertIsDisplayed()
        assertEquals(false, toggled.get())
        composeRule.runOnIdle {
            state = state.copy(
                series = state.series.copy(
                    seasonLoads = mapOf(cached.season.externalRef to SeasonLoadState.Error(AppError.NetworkUnavailable))
                )
            )
        }
        composeRule.onNodeWithText(label).assertIsDisplayed()
        scrollTo(hasText(context.getString(R.string.error_network_unavailable)))
        assertEquals(0, retries.get())
        composeRule.onNodeWithText(context.getString(R.string.action_retry)).performScrollTo().performClick()
        assertEquals(1, retries.get())
        composeRule.runOnIdle { state = state.copy(series = state.series.copy(seasonLoads = emptyMap())) }
        scrollTo(hasText(context.getString(R.string.detail_go_to_episode)))
        // Lazy-list test scrolling can align the button beneath the overlaid toolbar.
        val buttonHeight = composeRule.onNodeWithText(context.getString(R.string.detail_go_to_episode))
            .fetchSemanticsNode().size.height
        composeRule.onNode(hasScrollAction()).performSemanticsAction(SemanticsActions.ScrollBy) {
            it(0f, -buttonHeight * 2f)
        }
        composeRule.onNodeWithText(context.getString(R.string.detail_go_to_episode)).performClick()
        composeRule.waitForIdle()
        assertEquals(1, expansions.get())
        composeRule.onNodeWithText(label).assertIsDisplayed().performClick()
        assertTrue(toggled.get())
        assertEquals(setOf(cached.season.externalRef), state.series.expandedSeasons)
    }

    @Test
    fun oneSeasonLoadingFailureAndRetryKeepAnotherSeasonConsultable() {
        val saved = fetchedSeason(1, "Saved season", watched = 1, trackable = 1)
        val missing = season(2, "Missing season", expanded = false)
        val ref = missing.season.externalRef
        var state by mutableStateOf(
            seriesState(listOf(saved, missing)).let {
                it.copy(series = it.series.copy(expandedSeasons = setOf(saved.season.externalRef, ref)))
            }
        )
        val retries = mutableListOf<Int>()
        val writes = AtomicInteger()
        setDetailsState({ state }, onRetrySeason = {
            retries += it.season.seasonNumber
            state = state.copy(series = state.series.copy(seasonLoads = mapOf(ref to SeasonLoadState.Loading)))
        }, onToggleEpisode = { writes.incrementAndGet() })
        val savedLabel = context.getString(R.string.detail_episode_title, 1, "Saved season episode 1")
        val loading = SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)
        composeRule.runOnIdle {
            state = state.copy(series = state.series.copy(seasonLoads = mapOf(ref to SeasonLoadState.Loading)))
        }
        scrollTo(loading)
        composeRule.onNode(loading).assertIsDisplayed()
        scrollTo(hasText(savedLabel))
        composeRule.onNodeWithText(savedLabel).assertIsDisplayed()
        composeRule.runOnIdle {
            state = state.copy(
                series = state.series.copy(
                    seasonLoads = mapOf(ref to SeasonLoadState.Error(AppError.NetworkUnavailable))
                )
            )
        }
        val error = context.getString(R.string.error_network_unavailable)
        scrollTo(hasText(error))
        composeRule.onNodeWithText(error).assertIsDisplayed()
        assertTrue(retries.isEmpty())
        scrollTo(hasText(savedLabel))
        composeRule.onNodeWithText(savedLabel).assertIsDisplayed()
        val retry = context.getString(R.string.action_retry)
        scrollTo(hasText(retry))
        composeRule.onNodeWithText(retry).performSemanticsAction(SemanticsActions.OnClick) { it() }
        scrollTo(loading)
        composeRule.onNode(loading).assertIsDisplayed()
        assertEquals(listOf(2), retries)
        scrollTo(hasText(savedLabel))
        composeRule.onNodeWithText(savedLabel).assertIsDisplayed()
        composeRule.runOnIdle {
            state = state.copy(series = state.series.copy(seasonLoads = mapOf(ref to SeasonLoadState.Idle)))
        }
        assertEquals(setOf(saved.season.externalRef, ref), state.series.expandedSeasons)
        assertEquals(0, writes.get())
    }

    @Test
    fun missingEpisodeRowsLoadThroughErrorAndRetryBeforeNavigationInAnOpenSeason() {
        val loaded = fetchedSeason(1, "Retry season", watched = 49, trackable = 50)
        val missing = loaded.copy(episodes = emptyList(), progress = SeasonProgress.EMPTY, episodesFetchedAt = null)
        var state by mutableStateOf(seriesState(listOf(missing)).copy(isInLibrary = true))
        val expansions = AtomicInteger()
        val retries = AtomicInteger()
        val writes = AtomicInteger()
        setDetailsState({ state }, onToggleSeasonExpanded = {
            expansions.incrementAndGet()
            state = state.copy(
                series = state.series.copy(
                    expandedSeasons = setOf(missing.season.externalRef),
                    seasonLoads = mapOf(missing.season.externalRef to SeasonLoadState.Loading)
                )
            )
        }, onRetrySeason = {
            retries.incrementAndGet()
            state = state.copy(
                series = state.series.copy(seasonLoads = mapOf(missing.season.externalRef to SeasonLoadState.Loading))
            )
        }, onToggleEpisode = { writes.incrementAndGet() })
        val go = context.getString(R.string.detail_go_to_episode)
        composeRule.onNodeWithText(go).assertDoesNotExist()
        scrollTo(hasText("Retry season"))
        composeRule.onNodeWithText("Retry season").performClick()
        val loading = SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)
        scrollTo(loading)
        composeRule.onNode(loading).assertIsDisplayed()
        composeRule.onNodeWithText(go).assertDoesNotExist()
        composeRule.runOnIdle {
            state = state.copy(
                series = state.series.copy(
                    seasonLoads = mapOf(
                        missing.season.externalRef to SeasonLoadState.Error(AppError.NetworkUnavailable)
                    )
                )
            )
        }
        val error = context.getString(R.string.error_network_unavailable)
        scrollTo(hasText(error))
        composeRule.onNodeWithText(error).assertIsDisplayed()
        assertEquals(0, retries.get())
        composeRule.onNodeWithText(go).assertDoesNotExist()
        composeRule.onNodeWithText(context.getString(R.string.action_retry)).performScrollTo().performClick()
        scrollTo(loading)
        composeRule.onNode(loading).assertIsDisplayed()
        assertEquals(1, retries.get())
        composeRule.runOnIdle {
            val ready = SeriesContentState.Ready(listOf(loaded), deriveSeriesProgress(listOf(loaded)))
            state = state.copy(
                libraryProgress = LibraryProgress.Series(ready.progress.copy(nextEpisode = EpisodePosition(1, 50))),
                series = state.series.copy(content = ready, seasonLoads = emptyMap())
            )
        }
        scrollTo(hasText(go))
        val label = context.getString(R.string.detail_episode_title, 50, "Retry season episode 50")
        composeRule.onNodeWithText(label).assertDoesNotExist()
        composeRule.onNodeWithText(go).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(label).assertIsDisplayed().assertHasClickAction()
        assertEquals(1, expansions.get())
        assertEquals(1, retries.get())
        assertEquals(0, writes.get())
    }

    @Test
    fun disappearedEpisodeTargetDoesNotResumeWhenItsRowsReturn() {
        val cached = fetchedSeason(1, "Disappearing season", watched = 49, trackable = 50)
        val initial = seriesState(listOf(cached)).copy(
            isInLibrary = true,
            libraryProgress = LibraryProgress.Series(
                deriveSeriesProgress(listOf(cached)).copy(nextEpisode = EpisodePosition(1, 50))
            )
        )
        var state by mutableStateOf(initial)
        val expansions = AtomicInteger()
        val writes = AtomicInteger()
        // Delay the observed expansion so the command waits for its row before data changes.
        setDetailsState(
            { state },
            onToggleSeasonExpanded = { expansions.incrementAndGet() },
            onToggleEpisode = { writes.incrementAndGet() }
        )
        val go = context.getString(R.string.detail_go_to_episode)
        val position = context.getString(R.string.detail_next_episode_title, 1, 50, "Disappearing season episode 50")
        scrollTo(hasText(go))
        composeRule.onNodeWithText(go).performClick()
        composeRule.waitForIdle()
        assertEquals(1, expansions.get())
        composeRule.runOnIdle {
            val remaining = cached.episodes.dropLast(1)
            val reduced = cached.copy(episodes = remaining, progress = deriveSeasonProgress(remaining))
            val progress = deriveSeriesProgress(listOf(reduced))
            state = initial.copy(
                libraryProgress = LibraryProgress.Series(progress),
                series = initial.series.copy(content = SeriesContentState.Ready(listOf(reduced), progress))
            )
        }
        composeRule.onNodeWithText(go).assertDoesNotExist()
        composeRule.onNodeWithText(position).assertDoesNotExist()
        val before = composeRule.onNode(hasScrollAction()).fetchSemanticsNode()
            .config[SemanticsProperties.VerticalScrollAxisRange].value()
        composeRule.runOnIdle {
            state = initial.copy(series = initial.series.copy(expandedSeasons = setOf(cached.season.externalRef)))
        }
        composeRule.waitForIdle()
        val label = context.getString(R.string.detail_episode_title, 50, "Disappearing season episode 50")
        composeRule.onNodeWithText(label).assertDoesNotExist()
        val after = composeRule.onNode(hasScrollAction()).fetchSemanticsNode()
            .config[SemanticsProperties.VerticalScrollAxisRange].value()
        assertEquals(before, after, 0.001f)
        assertEquals(0, writes.get())
        scrollTo(hasText(go))
        // Keep the physical click below the overlaid toolbar after scrolling back to the summary.
        val buttonHeight = composeRule.onNodeWithText(go).fetchSemanticsNode().size.height
        composeRule.onNode(hasScrollAction()).performSemanticsAction(SemanticsActions.ScrollBy) {
            it(0f, -buttonHeight * 2f)
        }
        composeRule.onNodeWithText(position).assertIsDisplayed()
        composeRule.onNodeWithText(go).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(label).assertIsDisplayed()
        assertEquals(1, expansions.get())
        assertEquals(0, writes.get())
    }

    @Test
    fun changingTitleCancelsTheWaitingEpisodeCommand() {
        val cached = fetchedSeason(1, "Original season", watched = 49, trackable = 50)
        var state by mutableStateOf(
            seriesState(listOf(cached)).copy(
                isInLibrary = true,
                libraryProgress = LibraryProgress.Series(
                    deriveSeriesProgress(listOf(cached)).copy(nextEpisode = EpisodePosition(1, 50))
                )
            )
        )
        val expansions = AtomicInteger()
        val writes = AtomicInteger()
        setDetailsState(
            { state },
            onToggleSeasonExpanded = { expansions.incrementAndGet() },
            onToggleEpisode = { writes.incrementAndGet() }
        )
        val go = context.getString(R.string.detail_go_to_episode)
        scrollTo(hasText(go))
        composeRule.onNodeWithText(go).performClick()
        composeRule.waitForIdle()
        assertEquals(1, expansions.get())
        val otherRef = ExternalMediaRef(MediaSource.TMDB, "1400")
        val otherSeasonRef = ExternalMediaRef(MediaSource.TMDB, "1901")
        val other = cached.copy(
            season = cached.season.copy(seriesRef = otherRef, externalRef = otherSeasonRef, name = "Other season"),
            episodes = cached.episodes.map {
                it.copy(
                    episode = it.episode.copy(
                        seriesRef = otherRef,
                        seasonRef = otherSeasonRef,
                        externalRef = ExternalMediaRef(MediaSource.TMDB, "other-${it.episode.episodeNumber}"),
                        title = "Other episode ${it.episode.episodeNumber}"
                    )
                )
            }
        )
        composeRule.runOnIdle {
            val original = (state.content as DetailContentState.Content).cached
            val progress = deriveSeriesProgress(listOf(other))
            state = state.copy(
                content = DetailContentState.Content(
                    original.copy(details = original.details.copy(externalRef = otherRef, title = "Other title"))
                ),
                libraryProgress = LibraryProgress.Series(progress.copy(nextEpisode = EpisodePosition(1, 50))),
                series = SeriesDetailUiState(content = SeriesContentState.Ready(listOf(other), progress))
            )
        }
        composeRule.onNodeWithText(go).assertIsDisplayed()
        val before = composeRule.onNode(hasScrollAction()).fetchSemanticsNode()
            .config[SemanticsProperties.VerticalScrollAxisRange].value()
        composeRule.runOnIdle {
            state = state.copy(series = state.series.copy(expandedSeasons = setOf(otherSeasonRef)))
        }
        composeRule.waitForIdle()
        val label = context.getString(R.string.detail_episode_title, 50, "Other episode 50")
        composeRule.onNodeWithText(label).assertDoesNotExist()
        val after = composeRule.onNode(hasScrollAction()).fetchSemanticsNode()
            .config[SemanticsProperties.VerticalScrollAxisRange].value()
        assertEquals(before, after, 0.001f)
        scrollTo(hasText(go))
        val buttonHeight = composeRule.onNodeWithText(go).fetchSemanticsNode().size.height
        composeRule.onNode(hasScrollAction()).performSemanticsAction(SemanticsActions.ScrollBy) {
            it(0f, -buttonHeight * 2f)
        }
        composeRule.onNodeWithText(go).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(label).assertIsDisplayed()
        composeRule.onNodeWithText(
            context.getString(R.string.detail_episode_title, 50, "Original season episode 50")
        ).assertDoesNotExist()
        assertEquals(1, expansions.get())
        assertEquals(0, writes.get())
    }

    @Test
    @OptIn(ExperimentalMaterial3Api::class)
    fun episodeNavigationExposesAnUnobscuredAccessibleRowAndRefreshKeepsPosition() {
        val requireTalkBack = InstrumentationRegistry.getArguments().getString("requireTalkBack") == "true"
        val cached = fetchedSeason(1, "Accessible season", watched = 49, trackable = 50)
        var state by mutableStateOf(
            seriesState(listOf(cached)).copy(
                isInLibrary = true,
                libraryProgress = LibraryProgress.Series(
                    deriveSeriesProgress(listOf(cached)).copy(nextEpisode = EpisodePosition(1, 50))
                )
            )
        )
        val writes = AtomicInteger()
        setDetailsState(
            { state },
            onToggleSeasonExpanded = {
                state = state.copy(series = state.series.copy(expandedSeasons = setOf(cached.season.externalRef)))
            },
            onToggleEpisode = { writes.incrementAndGet() }
        )
        scrollTo(hasText(context.getString(R.string.detail_go_to_episode)))
        val go = composeRule.onNodeWithText(context.getString(R.string.detail_go_to_episode))
        if (requireTalkBack) {
            // A single physical tap explores rather than activates when TalkBack is running.
            go.performSemanticsAction(SemanticsActions.OnClick) { assertTrue(it()) }
        } else {
            go.performClick()
        }
        composeRule.waitForIdle()
        val label = context.getString(R.string.detail_episode_title, 50, "Accessible season episode 50")
        val row = composeRule.onNodeWithText(label)
        row.assertIsDisplayed().assertHasClickAction()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.Off))
        composeRule.onAllNodes(hasText(label) and isToggleable()).assertCountEquals(1)
        val bounds = row.fetchSemanticsNode().boundsInRoot
        val back = composeRule.onNodeWithContentDescription(context.getString(R.string.detail_back))
            .fetchSemanticsNode().touchBoundsInRoot
        val toolbarBottom = back.center.y + with(composeRule.density) {
            TopAppBarDefaults.TopAppBarExpandedHeight.toPx() / 2f
        }
        assertTrue("Episode row overlaps the toolbar", bounds.top >= toolbarBottom)
        assertTrue(
            "Episode row extends below the viewport",
            bounds.bottom <= composeRule.onRoot().fetchSemanticsNode().boundsInRoot.bottom
        )
        val touch = row.fetchSemanticsNode().touchBoundsInRoot
        val minimumTarget = with(composeRule.density) { 48.dp.toPx() }
        assertTrue(touch.height >= minimumTarget && touch.width >= minimumTarget)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val automation = instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        val manager = instrumentation.targetContext.getSystemService(AccessibilityManager::class.java)
        if (requireTalkBack) {
            composeRule.waitUntil(10000) {
                manager.isTouchExplorationEnabled && manager.getEnabledAccessibilityServiceList(-1).any {
                    it.resolveInfo.serviceInfo.packageName == "com.google.android.marvin.talkback"
                }
            }
        }
        // Switching UiAutomation flags reconnects its service; wait for the app window to be exported.
        composeRule.waitUntil(5000) {
            automation.rootInActiveWindow?.packageName == instrumentation.targetContext.packageName
        }
        fun nativeRow(text: String): AccessibilityNodeInfo {
            val root = requireNotNull(automation.rootInActiveWindow)
            fun findTitle(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
                if (node.text?.contains(text) == true) return node
                for (i in 0 until node.childCount) {
                    val found = node.getChild(i)?.let { findTitle(it) }
                    if (found != null) return found
                }
                return null
            }
            val title = requireNotNull(findTitle(root)) {
                "Episode title absent from the Android accessibility tree: $text"
            }
            // Android exports the text as a child of the screen-reader-focusable toggle group.
            return generateSequence(title) { it.parent }.first {
                it.isCheckable && it.isClickable
            }
        }
        val native = nativeRow(label)
        assertTrue(native.isVisibleToUser && native.isScreenReaderFocusable && !native.isChecked)
        assertEquals(context.getString(R.string.detail_episode_unwatched_state), native.stateDescription)
        val previousLabel = context.getString(R.string.detail_episode_title, 49, "Accessible season episode 49")
        if (requireTalkBack) {
            assertTrue(nativeRow(previousLabel).performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS))
            composeRule.waitUntil(5000) {
                automation.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY) == nativeRow(previousLabel)
            }
        }
        val before = composeRule.onNode(hasScrollAction()).fetchSemanticsNode()
            .config[SemanticsProperties.VerticalScrollAxisRange].value()
        composeRule.runOnIdle {
            val ready = state.series.content as SeriesContentState.Ready
            state = state.copy(
                series = state.series.copy(
                    content = ready.copy(
                        seasons = listOf(cached.copy(metadataUpdatedAt = Instant.ofEpochSecond(200)))
                    )
                )
            )
        }
        composeRule.waitForIdle()
        if (requireTalkBack) {
            assertTrue(automation.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY) == nativeRow(previousLabel))
        }
        val after = composeRule.onNode(hasScrollAction()).fetchSemanticsNode()
            .config[SemanticsProperties.VerticalScrollAxisRange].value()
        assertEquals(before, after, 0.001f)
        assertEquals(0, writes.get())
        if (requireTalkBack) {
            assertTrue(nativeRow(label).performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS))
            composeRule.waitUntil(5000) {
                automation.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY) == nativeRow(label)
            }
        }
        if (InstrumentationRegistry.getArguments().getString("captureM3") == "true") {
            val file = java.io.File(instrumentation.targetContext.filesDir, "m3-accessibility.png")
            file.outputStream().use {
                composeRule.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
        assertTrue(nativeRow(label).performAction(AccessibilityNodeInfo.ACTION_CLICK))
        composeRule.runOnIdle { assertEquals(1, writes.get()) }
    }

    @Test
    fun nextEpisodeSummaryKeepsUnwatchedTitlesHiddenWithSpoilersEnabled() {
        val cached = fetchedSeason(1, "Hidden season", watched = 1, trackable = 2)
        setDetails(
            seriesState(listOf(cached)).copy(
                isInLibrary = true,
                libraryProgress = LibraryProgress.Series(
                    deriveSeriesProgress(listOf(cached)).copy(
                        nextEpisode = EpisodePosition(1, 2)
                    )
                ),
                series = SeriesDetailUiState(
                    content = SeriesContentState.Ready(listOf(cached), deriveSeriesProgress(listOf(cached))),
                    hideSpoilers = true
                )
            )
        )
        val position = context.getString(R.string.detail_next_episode_position, 1, 2)
        scrollTo(hasText(position))
        composeRule.onNodeWithText(position).assertIsDisplayed()
        composeRule.onNodeWithText("Hidden season episode 2", substring = true).assertDoesNotExist()
    }

    @Test
    fun restoredEpisodeRowsRemainVisibleWithoutAFetchTimestamp() {
        val cached = fetchedSeason(1, "Restored season", watched = 1, trackable = 2)
            .copy(episodesFetchedAt = null, episodeCacheFreshness = null)
        val original = seriesState(listOf(cached))
        setDetails(original.copy(series = original.series.copy(expandedSeasons = setOf(cached.season.externalRef))))
        val label = context.getString(R.string.detail_episode_title, 2, "Restored season episode 2")
        scrollTo(hasText(label))
        composeRule.onNodeWithText(label).assertIsDisplayed().assertHasClickAction()
    }

    @Test
    fun movieDetailsRenderWatchedActionAndTitleRatingControls() {
        val toggled = AtomicBoolean(false)
        setDetails(
            content(movie()).copy(
                movieProgress = MovieProgressState.Ready(MovieWatchState.Unwatched)
            ),
            onToggleMovie = { toggled.set(true) }
        )

        composeRule.onNodeWithText("Movie title").assertIsDisplayed()
        // The runtime now lives inside the hero's meta line ("Movie · 120 min") instead of its own
        // labelled field, so it is asserted as a substring of that line.
        composeRule.onNode(
            hasText(context.getString(R.string.detail_minutes, 120), substring = true)
        ).assertIsDisplayed()
        // Status and genres are individual chips now, not a status Text plus one joined string.
        scrollTo(hasText(context.getString(R.string.detail_status_released)))
        composeRule.onNodeWithText(context.getString(R.string.detail_status_released)).assertIsDisplayed()
        composeRule.onNodeWithText("Drama").assertIsDisplayed()
        composeRule.onNodeWithText("Thriller").assertIsDisplayed()
        scrollTo(hasText(context.getString(R.string.detail_rating_title)))
        composeRule.onNodeWithText(context.getString(R.string.detail_rating_title)).assertIsDisplayed()
        // The hero meta line only carries the year, so the full date has to survive further down.
        scrollTo(hasText(context.getString(R.string.detail_date)))
        composeRule.onNodeWithText(context.getString(R.string.detail_date)).assertIsDisplayed()
        scrollTo(hasText(context.getString(R.string.detail_mark_watched)))
        composeRule.onNode(
            SemanticsMatcher.expectValue(
                SemanticsProperties.StateDescription,
                context.getString(R.string.detail_movie_unwatched)
            )
        ).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.detail_mark_watched)).performClick()
        assertTrue(toggled.get())
    }

    @Test
    fun favoriteActionExposesItsCurrentToggleState() {
        val toggled = AtomicBoolean(false)
        setDetails(
            content(movie()).copy(isInLibrary = true),
            onToggleFavorite = { toggled.set(true) }
        )

        composeRule.onNodeWithContentDescription(context.getString(R.string.favorite_add))
            .assert(isToggleable())
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ToggleableState,
                    ToggleableState.Off
                )
            )
            .performClick()
        assertTrue(toggled.get())
    }

    @Test
    fun clearDateActionCommunicatesRemovalAndPassesNull() {
        assertDateActions(
            content(movie()).copy(
                isInLibrary = true,
                watchedDate = LocalDate.of(2026, 8, 17),
                movieProgress = MovieProgressState.Ready(MovieWatchState.Watched(Instant.EPOCH))
            ),
            R.string.watched_date_edit
        )
    }

    @Test
    fun completedSeriesDateActionsPreserveEditCancelSaveAndClear() {
        val completed = seriesState(
            listOf(fetchedSeason(1, "Season 1", watched = 2, trackable = 2)),
            ProductionStatus.ENDED
        )
            .copy(isInLibrary = true, watchedDate = LocalDate.of(2026, 8, 17), seriesCompletedAt = Instant.EPOCH)
        assertTrue((completed.series.content as SeriesContentState.Ready).progress.isComplete)
        assertDateActions(completed, R.string.completion_date_edit)
    }

    private fun assertDateActions(initialState: MediaDetailsUiState, editLabelRes: Int) {
        val context = localizedTestContext
        val clearLabel = context.getString(R.string.watched_date_clear)
        val editLabel = context.getString(editLabelRes)
        val cancelLabel = context.getString(R.string.action_cancel)
        val saveLabel = context.getString(R.string.action_save)
        val writes = mutableListOf<LocalDate?>()
        var state by mutableStateOf(initialState)
        setDetailsState({ state }, onSetWatchedDate = { writes.add(it) })

        scrollTo(hasText(editLabel))
        listOf(editLabel, clearLabel).forEach { label ->
            val action = composeRule.onNodeWithText(label)
            action.performScrollTo().assertIsDisplayed()
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            composeRule.onAllNodesWithText(label).assertCountEquals(1)
            val target = action.fetchSemanticsNode().touchBoundsInRoot
            with(composeRule.density) {
                assertTrue(target.width.toDp() >= 48.dp && target.height.toDp() >= 48.dp)
            }
        }
        composeRule.onNodeWithText(editLabel).performScrollTo().performClick()
        composeRule.onNodeWithText(cancelLabel).performClick()
        assertTrue(writes.isEmpty())
        composeRule.onNodeWithText(editLabel).performClick()
        composeRule.onNodeWithText(saveLabel).performClick()
        assertEquals(listOf(initialState.today), writes)
        composeRule.onNodeWithText(clearLabel).performScrollTo().performClick()
        assertEquals(listOf(initialState.today, null), writes)

        composeRule.runOnIdle { state = state.copy(watchedDateUpdating = true) }
        composeRule.onNodeWithText(clearLabel).assertIsNotEnabled()
        composeRule.onNodeWithText(editLabel).performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun adaptiveMovieLongTextAndOpenEditorsRemainReachable() {
        assertAdaptiveEditors(
            content(movie().copy(title = adaptiveLongText(), overview = adaptiveLongText().repeat(4))).copy(
                isInLibrary = true,
                watchedDate = LocalDate.of(2026, 8, 17),
                movieProgress = MovieProgressState.Ready(MovieWatchState.Watched(Instant.EPOCH)),
                rating = DetailRatingState.Ready(PersonalRating(8))
            ),
            R.string.watched_date_edit
        )
    }

    @Test
    fun adaptiveSeriesLongTextAndOpenEditorsRemainReachable() {
        val cached = fetchedSeason(1, adaptiveLongText(), watched = 2, trackable = 2)
        assertAdaptiveEditors(
            seriesState(listOf(cached), ProductionStatus.ENDED).let { state ->
                val content = state.content as DetailContentState.Content
                state.copy(
                    content = content.copy(
                        cached = content.cached.copy(
                            details = content.cached.details.copy(
                                title = adaptiveLongText(),
                                overview = "Synopsis: ${adaptiveLongText()}"
                            )
                        )
                    ),
                    isInLibrary = true,
                    watchedDate = LocalDate.of(2026, 8, 17),
                    seriesCompletedAt = Instant.EPOCH,
                    rating = DetailRatingState.Ready(PersonalRating(8))
                )
            },
            R.string.completion_date_edit
        )
    }

    private fun adaptiveLongText() = if (context.resources.configuration.locales[0].language == "it") {
        "Un titolo molto lungo con un sottotitolo esteso per verificare che testo e azioni restino leggibili e raggiungibili"
    } else {
        "A very long title with an extended subtitle to verify that text and actions remain readable and reachable"
    }

    private fun assertAdaptiveEditors(initial: MediaDetailsUiState, editDateRes: Int) {
        val details = (initial.content as DetailContentState.Content).cached.details
        val ratingWrites = mutableListOf<Int>()
        val dateWrites = mutableListOf<LocalDate?>()
        var state by mutableStateOf(initial.copy(content = DetailContentState.Loading))
        setDetailsState(
            { state },
            onSelectRating = { value ->
                val rating = state.rating as DetailRatingState.Ready
                state = state.copy(rating = rating.copy(selectedValue = value))
            },
            onSaveRating = {
                val rating = state.rating as DetailRatingState.Ready
                ratingWrites += rating.selectedValue
                state = state.copy(rating = DetailRatingState.Ready(PersonalRating(rating.selectedValue)))
            },
            onSetWatchedDate = { dateWrites += it }
        )
        composeRule.onNodeWithText(context.getString(R.string.detail_loading)).assertIsDisplayed()
        composeRule.runOnIdle { state = initial.copy(content = DetailContentState.Error(AppError.NetworkUnavailable)) }
        composeRule.onNodeWithText(context.getString(R.string.action_retry)).assertIsDisplayed().assertHasClickAction()
        composeRule.runOnIdle { state = initial }
        if (details.mediaType == MediaType.SERIES) {
            composeRule.runOnIdle {
                state = initial.copy(
                    libraryProgress = LibraryProgress.Series(SeriesProgress.EMPTY),
                    seriesCompletedAt = null,
                    series = initial.series.copy(content = SeriesContentState.Ready(emptyList(), SeriesProgress.EMPTY))
                )
            }
            scrollTo(hasText(context.getString(R.string.detail_tracking_incomplete)))
            composeRule.onNodeWithText(context.getString(R.string.detail_tracking_incomplete)).assertIsDisplayed()
            composeRule.runOnIdle { state = initial }
            val season = (state.series.content as SeriesContentState.Ready).seasons.single()
            composeRule.runOnIdle {
                state = state.copy(series = state.series.copy(expandedSeasons = setOf(season.season.externalRef)))
            }
            val episode = context.getString(R.string.detail_episode_title, 1, "${adaptiveLongText()} episode 1")
            scrollTo(hasText(episode))
            assertTextFits(episode)
        }
        scrollTo(hasText(requireNotNull(details.overview)))
        assertTextFits(requireNotNull(details.overview))
        val currentRating = context.getString(R.string.detail_rating_value, 8)
        scrollTo(hasText(currentRating))
        composeRule.onNodeWithText(currentRating).performSemanticsAction(SemanticsActions.OnClick) { it() }
        val slider = SemanticsMatcher.expectValue(
            SemanticsProperties.StateDescription,
            context.getString(R.string.detail_rating_slider, 8)
        )
        scrollTo(slider)
        composeRule.onNode(slider).assertIsDisplayed().performSemanticsAction(SemanticsActions.SetProgress) { it(9f) }
        val selected = context.getString(R.string.detail_rating_selected_value, 9)
        scrollTo(hasText(selected))
        captureAdaptiveEditor("${details.mediaType}-rating", dialog = false)
        assertTextFits(selected)
        val cancel = context.getString(R.string.action_cancel)
        scrollTo(hasText(cancel))
        composeRule.onNodeWithText(cancel).performScrollTo().assertIsDisplayed().performClick()
        assertTrue(ratingWrites.isEmpty())
        val dateAction = context.getString(editDateRes)
        // Reset the personal item after collapsing rating; its old offset can put this action under the toolbar.
        composeRule.onNode(hasScrollAction()).performScrollToKey("personal")
        composeRule.onNodeWithText(dateAction).performScrollTo().assertIsDisplayed().performClick()
        val save = context.getString(R.string.action_save)
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(isDialog()).fetchSemanticsNodes().size == 1 &&
                composeRule.onNode(isDialog()).isDisplayed()
        }
        composeRule.onNode(isDialog()).assertIsDisplayed()
        listOf(save, cancel).forEach { label ->
            composeRule.onNodeWithText(label).assertIsDisplayed()
            assertTextFits(label)
        }
        captureAdaptiveEditor("${details.mediaType}-date", dialog = true)
        composeRule.onNodeWithText(cancel).performClick()
        assertTrue(dateWrites.isEmpty())
        composeRule.onNode(hasScrollAction()).performScrollToKey("personal")
        composeRule.onNodeWithText(dateAction).performScrollTo().performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(isDialog()).fetchSemanticsNodes().size == 1 &&
                composeRule.onNode(isDialog()).isDisplayed()
        }
        composeRule.onNodeWithText(save).performClick()
        assertEquals(listOf(initial.today), dateWrites)
        scrollTo(hasText(currentRating))
        composeRule.onNodeWithText(currentRating).performSemanticsAction(SemanticsActions.OnClick) { it() }
        scrollTo(slider)
        composeRule.onNode(slider).performSemanticsAction(SemanticsActions.SetProgress) { it(9f) }
        val saveRating = context.getString(R.string.detail_rating_set)
        scrollTo(hasText(saveRating))
        composeRule.onNodeWithText(saveRating).performScrollTo().assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertEquals(listOf(9), ratingWrites)
    }

    private fun assertTextFits(text: String) {
        val layouts = mutableListOf<TextLayoutResult>()
        composeRule.onNodeWithText(text, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertEquals(1, layouts.size)
        val layout = layouts.single()
        InstrumentationRegistry.getArguments().getString("detailsFontScale")?.let {
            assertEquals("Applied font scale for: $text", it.toFloat(), layout.layoutInput.density.fontScale, 0.001f)
        }
        val right = (0 until layout.lineCount).maxOf(layout::getLineRight)
        // TextLayoutResult can report fractional paragraph overflow after pixel rounding.
        val clipped = layout.didOverflowHeight || right > layout.size.width + 1f
        assertEquals(
            "Text must not overflow: $text; size=${layout.size}, lines=${layout.lineCount}, " +
                "right=$right, paragraph=${layout.multiParagraph.width}, height=${layout.didOverflowHeight}",
            false,
            clipped
        )
    }

    private fun captureAdaptiveEditor(name: String, dialog: Boolean) {
        if (InstrumentationRegistry.getArguments().getString("captureM5Adaptive") != "true") return
        val node = if (dialog) composeRule.onNode(isDialog()) else composeRule.onRoot()
        val target = java.io.File(InstrumentationRegistry.getInstrumentation().targetContext.filesDir, "m5-$name.png")
        target.outputStream().use {
            node.captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test
    fun longSeasonAndEpisodeTitlesRemainReadableDuringSeasonRefresh() {
        val name = "A long localized season title with an extended subtitle that must wrap without hiding the controls"
        val cached = fetchedSeason(1, name, watched = 1, trackable = 2)
            .copy(episodeCacheFreshness = CacheFreshness.STALE)
        var state by mutableStateOf(seriesState(listOf(cached)))
        val refreshes = mutableListOf<ExternalMediaRef>()
        setDetailsState(
            { state },
            onToggleSeasonExpanded = {
                state = state.copy(series = state.series.copy(expandedSeasons = setOf(cached.season.externalRef)))
            },
            onRetrySeason = {
                refreshes += it.season.externalRef
                state = state.copy(
                    series = state.series.copy(seasonLoads = mapOf(it.season.externalRef to SeasonLoadState.Refreshing))
                )
            }
        )
        scrollTo(hasText(name))
        val seasonLayout = mutableListOf<TextLayoutResult>()
        composeRule.onNodeWithText(name, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(seasonLayout) }
        assertEquals(1, seasonLayout.size)
        assertTrue(seasonLayout.single().lineCount > 1)
        assertEquals(false, seasonLayout.single().hasVisualOverflow)
        composeRule.onNodeWithContentDescription(context.getString(R.string.detail_refresh)).assertIsDisplayed()
        composeRule.onNodeWithText(name).performSemanticsAction(SemanticsActions.OnClick) { assertTrue(it()) }
        val title = context.getString(R.string.detail_episode_title, 2, "$name episode 2")
        scrollTo(hasText(title))
        val episodeLayout = mutableListOf<TextLayoutResult>()
        composeRule.onNodeWithText(title, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(episodeLayout) }
        assertEquals(1, episodeLayout.size)
        assertTrue(episodeLayout.single().lineCount > 1)
        assertEquals(false, episodeLayout.single().hasVisualOverflow)
        scrollTo(hasText(name))
        val refreshLabel = context.getString(R.string.detail_refresh_season)
        assertNotEquals(context.getString(R.string.detail_refresh), refreshLabel)
        composeRule.onNodeWithContentDescription(refreshLabel)
            .performSemanticsAction(SemanticsActions.OnClick) { assertTrue(it()) }
        assertEquals(listOf(cached.season.externalRef), refreshes)
        composeRule.onNodeWithContentDescription(refreshLabel).assertIsNotEnabled()
        val refreshing = context.getString(R.string.detail_season_refreshing)
        scrollTo(hasText(refreshing))
        composeRule.onNodeWithText(refreshing).assertIsDisplayed()
        scrollTo(hasText(title))
        composeRule.onNodeWithText(title).assertIsDisplayed().assertHasClickAction()
        val titleText = composeRule.onNodeWithText(title, useUnmergedTree = true)
        titleText.performScrollTo()
        val titleBounds = titleText.fetchSemanticsNode().boundsInRoot
        val backBounds = composeRule.onNodeWithContentDescription(context.getString(R.string.detail_back))
            .fetchSemanticsNode().touchBoundsInRoot
        assertTrue(titleBounds.top >= backBounds.bottom)
        assertTrue(titleBounds.bottom <= composeRule.onRoot().fetchSemanticsNode().boundsInRoot.bottom)
        assertEquals(setOf(cached.season.externalRef), state.series.expandedSeasons)
        assertEquals(listOf(cached), (state.series.content as SeriesContentState.Ready).seasons)
        if (InstrumentationRegistry.getArguments().getString("captureM4") == "true") {
            val file = java.io.File(
                InstrumentationRegistry.getInstrumentation().targetContext.filesDir,
                "m4-long-title.png"
            )
            file.outputStream().use {
                composeRule.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
    }

    @Test
    fun adaptiveEpisodeThumbnailsYieldToTextAndBlurOnlyUnwatchedImages() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val args = InstrumentationRegistry.getArguments()
        val width = args.getString("detailsWidthDp")?.toInt() ?: context.resources.configuration.screenWidthDp
        val font = args.getString("detailsFontScale")?.toFloat() ?: context.resources.configuration.fontScale
        val expectedImage = width >= 400 && font <= 1.2f
        val imageFile = java.io.File(instrumentation.targetContext.cacheDir, "m4-checker.png")
        val pixels = IntArray(160 * 90) { if ((it / 160 / 10 + it % 160 / 10) % 2 == 0) -16777216 else -1 }
        imageFile.outputStream().use {
            Bitmap.createBitmap(pixels, 160, 90, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        val original = fetchedSeason(1, "Photo fixture", watched = 1, trackable = 2)
        val cached = original.copy(
            episodes = original.episodes.map {
                it.copy(episode = it.episode.copy(stillUrl = Uri.fromFile(imageFile).toString()))
            }
        )
        var state by mutableStateOf(
            seriesState(listOf(cached)).let {
                it.copy(series = it.series.copy(expandedSeasons = setOf(cached.season.externalRef)))
            }
        )
        setDetailsState({ state })
        val label = context.getString(R.string.detail_episode_title, 2, "Photo fixture episode 2")
        scrollTo(hasText(label))
        val row = composeRule.onNodeWithText(label)
        row.performScrollTo().assertIsDisplayed()
        val node = row.fetchSemanticsNode()
        val density = node.layoutInfo.density
        val listWidth = composeRule.onNode(hasScrollAction()).fetchSemanticsNode().boundsInRoot.width
        // Density division can put a valid one-dp difference just above 1f.
        assertEquals(width.toFloat(), listWidth / density.density, 1.001f)
        assertEquals(font, density.fontScale, 0.001f)
        val text = composeRule.onNodeWithText(label, useUnmergedTree = true).fetchSemanticsNode()
        val textInset = (text.boundsInRoot.left - node.boundsInRoot.left) / density.density
        if (expectedImage) {
            assertTrue("Expected a thumbnail before the title at 411dp/font1.0", textInset > 100f)
        } else {
            assertEquals("Thumbnail must yield its space to text", 0f, textInset, 1f)
        }
        fun capture(labelText: String): ImageBitmap {
            scrollTo(hasText(labelText))
            return composeRule.onNodeWithText(labelText).performScrollTo().captureToImage()
        }
        fun contrast(bitmap: ImageBitmap): Float {
            val map = bitmap.toPixelMap()
            val imageWidth = with(density) { 120.dp.roundToPx() }
            val halfHeight = with(density) { 12.dp.roundToPx() }
            var total = 0f
            var count = 0
            for (y in bitmap.height / 2 - halfHeight until bitmap.height / 2 + halfHeight) {
                for (x in imageWidth / 6 until imageWidth * 5 / 6 - 1) {
                    total += kotlin.math.abs(map[x + 1, y].red - map[x, y].red)
                    count++
                }
            }
            return total / count
        }
        fun save(bitmap: ImageBitmap, name: String) {
            if (args.getString("captureM4Adaptive") == "true") {
                java.io.File(instrumentation.targetContext.filesDir, name).outputStream().use {
                    bitmap.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
                }
            }
        }
        val sharp = if (expectedImage) {
            composeRule.waitUntil(10000) { contrast(capture(label)) > 0.03f }
            capture(label).also { save(it, "m4-photo-clear.png") }
        } else {
            null
        }
        composeRule.runOnIdle { state = state.copy(series = state.series.copy(hideSpoilers = true)) }
        val hidden = context.getString(R.string.detail_episode_title_hidden, 2)
        scrollTo(hasText(hidden))
        composeRule.onNodeWithText(hidden).assertIsDisplayed()
        composeRule.onNodeWithText("Photo fixture episode 2", substring = true, useUnmergedTree = true)
            .assertDoesNotExist()
        composeRule.onAllNodes(hasText(hidden) and isToggleable()).assertCountEquals(1)
        if (sharp != null) {
            val blurred = capture(hidden)
            save(blurred, "m4-photo-blurred.png")
            val sharpContrast = contrast(sharp)
            assertTrue("Unwatched still must lose high-frequency detail", contrast(blurred) < sharpContrast * 0.5f)
            val watched = capture(context.getString(R.string.detail_episode_title, 1, "Photo fixture episode 1"))
            save(watched, "m4-photo-watched.png")
            assertTrue("Watched still must remain sharp", contrast(watched) > sharpContrast * 0.8f)
        }
    }

    @Test
    fun spoilersKeepWatchedTitlesButHideUnwatchedFutureAndSpecialTitles() {
        val regular = season(1, "Regular season", expanded = true)
        val specialEpisode = tracked(
            regular.season.seriesRef,
            ExternalMediaRef(MediaSource.TMDB, "900"),
            0,
            7,
            "Special reveal",
            EpisodeWatchState.Unwatched
        )
        val specials = season(0, "Behind the scenes", expanded = false).copy(
            season = Season(
                regular.season.seriesRef,
                specialEpisode.episode.seasonRef,
                0,
                name = "Behind the scenes",
                episodeCount = 1
            ),
            episodesFetchedAt = Instant.EPOCH,
            episodes = listOf(specialEpisode),
            progress = deriveSeasonProgress(listOf(specialEpisode))
        )
        var state by mutableStateOf(
            seriesState(listOf(specials, regular)).let {
                it.copy(
                    series = it.series.copy(
                        expandedSeasons = setOf(regular.season.externalRef),
                        hideSpoilers = true
                    )
                )
            }
        )
        val toggles = mutableListOf<Pair<Int, Int>>()
        setDetailsState(
            { state },
            onToggleEpisode = { toggles += it.episode.seasonNumber to it.episode.episodeNumber },
            onToggleSeasonExpanded = {
                state = state.copy(
                    series = state.series.copy(expandedSeasons = state.series.expandedSeasons + it.season.externalRef)
                )
            }
        )
        val watched = context.getString(R.string.detail_episode_title, 1, "Watched episode")
        scrollTo(hasText(watched))
        composeRule.onNodeWithText(watched).assertIsDisplayed()
        val hidden = context.getString(R.string.detail_episode_title_hidden, 2)
        scrollTo(hasText(hidden))
        composeRule.onNodeWithText(hidden).assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.OnClick) { assertTrue(it()) }
        composeRule.onNodeWithText("Unwatched episode", substring = true, useUnmergedTree = true).assertDoesNotExist()
        val future = context.getString(R.string.detail_episode_title_hidden, 3)
        scrollTo(hasText(future))
        composeRule.onNodeWithText(future).assertIsDisplayed().assertIsNotEnabled().performTouchInput { click() }
        composeRule.onNodeWithText("Future episode", substring = true, useUnmergedTree = true).assertDoesNotExist()
        assertEquals(listOf(1 to 2), toggles)
        composeRule.runOnIdle { state = state.copy(series = state.series.copy(hideSpoilers = false)) }
        val revealed = context.getString(R.string.detail_episode_title, 2, "Unwatched episode")
        scrollTo(hasText(revealed))
        composeRule.onNodeWithText(revealed).assertIsDisplayed()
        val revealedFuture = context.getString(R.string.detail_episode_title, 3, "Future episode")
        scrollTo(hasText(revealedFuture))
        composeRule.onNodeWithText(revealedFuture).assertIsDisplayed().assertIsNotEnabled()
        composeRule.runOnIdle { state = state.copy(series = state.series.copy(hideSpoilers = true)) }
        scrollTo(hasText(context.getString(R.string.detail_specials_title)))
        composeRule.onNodeWithText(context.getString(R.string.detail_specials_title)).assertIsDisplayed()
        val specialHidden = context.getString(R.string.detail_episode_title_hidden, 7)
        composeRule.onNodeWithText(specialHidden).assertDoesNotExist()
        scrollTo(hasText("Behind the scenes"))
        composeRule.onNodeWithText("Behind the scenes")
            .performSemanticsAction(SemanticsActions.OnClick) { assertTrue(it()) }
        scrollTo(hasText(specialHidden))
        composeRule.onNodeWithText(specialHidden).assertIsDisplayed()
        composeRule.onNodeWithText("Special reveal", substring = true, useUnmergedTree = true).assertDoesNotExist()
        composeRule.runOnIdle { state = state.copy(series = state.series.copy(hideSpoilers = false)) }
        composeRule.onNodeWithText(context.getString(R.string.detail_episode_title, 7, "Special reveal"))
            .assertIsDisplayed().performSemanticsAction(SemanticsActions.OnClick) { assertTrue(it()) }
        assertEquals(listOf(1 to 2, 0 to 7), toggles)
        assertEquals(setOf(regular.season.externalRef, specials.season.externalRef), state.series.expandedSeasons)
    }

    @Test
    fun tvDetailsShowSeasonsSpecialsEpisodesAndDisableFutureEpisode() {
        val regular = season(1, "Season 1", expanded = true)
        val specials = season(0, "Specials", expanded = false)
        setDetails(
            content(
                movie().copy(
                    mediaType = MediaType.SERIES,
                    runtime = null,
                    episodeRuntime = Duration.ofMinutes(50),
                    numberOfSeasons = 3,
                    numberOfEpisodes = 24,
                    productionStatus = ProductionStatus.RETURNING_SERIES
                )
            ).copy(
                series = SeriesDetailUiState(
                    content = SeriesContentState.Ready(
                        listOf(specials, regular),
                        SeriesProgress(1, 2, 0, 1, false)
                    ),
                    expandedSeasons = setOf(regular.season.externalRef)
                )
            )
        )

        // The hero meta line reads "TV Series · 3 seasons", so the type is a substring of it.
        composeRule.onNode(hasText(context.getString(R.string.library_type_tv), substring = true)).assertIsDisplayed()
        // Each season is its own lazy item, so every assertion first scrolls its target into composition.
        scrollTo(hasText("Season 1"))
        composeRule.onNodeWithText("Season 1").assertIsDisplayed()
        scrollTo(hasText(context.getString(R.string.detail_episode_title, 1, "Watched episode")))
        composeRule.onNodeWithText(
            context.getString(R.string.detail_episode_title, 1, "Watched episode")
        ).performScrollTo().assertIsDisplayed()
        scrollTo(hasText(context.getString(R.string.detail_episode_title, 3, "Future episode")))
        composeRule.onNodeWithText(
            context.getString(R.string.detail_episode_title, 3, "Future episode")
        ).performScrollTo().assertIsDisplayed()
        composeRule.onNode(
            SemanticsMatcher.expectValue(
                SemanticsProperties.StateDescription,
                context.getString(R.string.detail_episode_future)
            )
        ).assertIsNotEnabled()
        scrollTo(hasText(context.getString(R.string.detail_specials_title)))
        composeRule.onAllNodesWithText(
            context.getString(R.string.detail_specials_title)
        )[0].performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Rate episode").assertDoesNotExist()
        composeRule.onNodeWithText("Rate season").assertDoesNotExist()
    }

    @Test
    fun episodeRowTogglesFromItsBodyAndExposesOneTargetPerEpisode() {
        val toggled = AtomicBoolean(false)
        val regular = season(1, "Season 1", expanded = true)
        setDetails(
            content(
                movie().copy(
                    mediaType = MediaType.SERIES,
                    runtime = null,
                    episodeRuntime = Duration.ofMinutes(50),
                    numberOfSeasons = 1,
                    numberOfEpisodes = 3,
                    productionStatus = ProductionStatus.RETURNING_SERIES
                )
            ).copy(
                series = SeriesDetailUiState(
                    content = SeriesContentState.Ready(listOf(regular), SeriesProgress(1, 2, 0, 1, false)),
                    expandedSeasons = setOf(regular.season.externalRef)
                )
            ),
            onToggleEpisode = { toggled.set(true) }
        )

        scrollTo(hasText("Season 1"))
        // Offscreen episodes are lazy; each composed row still exposes exactly one checkbox target.
        val title = context.getString(R.string.detail_episode_title, 2, "Unwatched episode")
        scrollTo(hasText(title))
        composeRule.onAllNodes(hasText(title) and isToggleable()).assertCountEquals(1)
        // The tap lands on the title, not on the checkbox: the whole row is the target.
        composeRule.onNodeWithText(
            context.getString(R.string.detail_episode_title, 2, "Unwatched episode")
        ).performScrollTo().performClick()
        assertTrue(toggled.get())
    }

    @Test
    fun largeExpandedSeasonKeepsOffscreenEpisodesLazyAndPreservesToggleAfterScroll() {
        val base = season(1, "Long season", expanded = true)
        val episodes = (1..300).map { number ->
            tracked(
                base.season.seriesRef,
                base.season.externalRef,
                1,
                number,
                "Episode $number",
                EpisodeWatchState.Unwatched
            )
        }
        val long = base.copy(season = base.season.copy(episodeCount = 300), episodes = episodes)
        val toggled = AtomicBoolean()
        setDetails(
            seriesState(listOf(long)).let {
                it.copy(series = it.series.copy(expandedSeasons = setOf(long.season.externalRef)))
            },
            onToggleEpisode = { toggled.set(it.episode.episodeNumber == 300) }
        )
        scrollTo(hasText("Long season"))
        val lastTitle = context.getString(R.string.detail_episode_title, 300, "Episode 300")
        composeRule.onNodeWithText(lastTitle).assertDoesNotExist()
        // The viewport and prefetch, rather than all 300 episodes, bound the semantics tree.
        assertTrue(composeRule.onAllNodes(isToggleable()).fetchSemanticsNodes().size < 20)
        scrollTo(hasText(lastTitle))
        composeRule.onAllNodes(hasText(lastTitle) and isToggleable()).assertCountEquals(1)
        composeRule.onNodeWithText(lastTitle).performClick()
        assertTrue(toggled.get())
    }

    @Test
    fun seasonHeaderCheckboxReportsEveryStateAndTogglesTheSeason() {
        val toggled = AtomicInteger()
        val expandTaps = AtomicInteger()
        val complete = fetchedSeason(1, "Season 1", watched = 2, trackable = 2)
        val partial = fetchedSeason(2, "Season 2", watched = 1, trackable = 2)
        val untouched = fetchedSeason(3, "Season 3", watched = 0, trackable = 2)
        setDetails(
            seriesState(listOf(complete, partial, untouched)),
            onToggleSeasonWatched = { toggled.incrementAndGet() },
            onToggleSeasonExpanded = { expandTaps.incrementAndGet() }
        )

        scrollTo(hasText("Season 1"))
        // The season action left the overflow, which went with its last item.
        composeRule.onNodeWithContentDescription("Actions for Season 1").assertDoesNotExist()
        assertSeasonState("Season 1", context.getString(R.string.detail_season_watched_state))
        assertSeasonState("Season 2", context.getString(R.string.detail_season_partial_state))
        assertSeasonState("Season 3", context.getString(R.string.detail_movie_unwatched))

        // A complete season offers the inverse action, and the checkbox is its only home now.
        scrollTo(hasText("Season 1"))
        composeRule.onNodeWithContentDescription(
            context.getString(R.string.detail_mark_season_unwatched)
        ).performClick()
        assertEquals(1, toggled.get())
        // A press the checkbox handled must not also expand the card behind it.
        assertEquals(0, expandTaps.get())
    }

    @Test
    fun seasonCheckboxIsDisabledWhilePendingAndWhenNothingIsTrackable() {
        val pending = fetchedSeason(1, "Season 1", watched = 1, trackable = 2)
        val untrackable = fetchedSeason(2, "Season 2", watched = 0, trackable = 0, unaired = 2)
        val state = seriesState(listOf(pending, untrackable))
        setDetails(
            state.copy(series = state.series.copy(pendingSeasons = setOf(pending.season.externalRef))),
            onToggleSeasonWatched = { throw AssertionError("A disabled checkbox must not toggle the season") }
        )

        // The checkbox cannot say why it is disabled, so the card body still carries the reason.
        scrollTo(hasText(context.getString(R.string.detail_progress_updating)))
        composeRule.onNodeWithText(context.getString(R.string.detail_progress_updating)).assertIsDisplayed()
        seasonCheckbox(context.getString(R.string.detail_season_partial_state)).assertIsNotEnabled().performClick()
        scrollTo(hasText("Season 2"))
        seasonCheckbox(context.getString(R.string.detail_movie_unwatched)).assertIsNotEnabled().performClick()
    }

    @Test
    fun seasonHeaderTargetsAreSeparateAndAcceptTouchesOnEveryEdge() {
        val toggled = AtomicInteger()
        val refreshed = AtomicInteger()
        val expandTaps = AtomicInteger()
        setDetails(
            seriesState(listOf(fetchedSeason(1, "Season 1", watched = 1, trackable = 2))),
            onToggleSeasonWatched = { toggled.incrementAndGet() },
            onRetrySeason = { refreshed.incrementAndGet() },
            onToggleSeasonExpanded = { expandTaps.incrementAndGet() }
        )
        scrollTo(hasText("Season 1"))

        // A checkbox lays out at 24dp and gets its 48dp target from minimumInteractiveComponentSize,
        // which widens the touch bounds without widening the layout bounds. Only touchBoundsInRoot
        // describes what a finger can actually hit.
        val checkboxTouch = composeRule.onNodeWithContentDescription(
            context.getString(R.string.detail_mark_season_watched)
        )
            .fetchSemanticsNode().touchBoundsInRoot
        val refreshTouch = composeRule.onNodeWithContentDescription(context.getString(R.string.detail_refresh_season))
            .fetchSemanticsNode().touchBoundsInRoot
        with(composeRule.density) {
            assertTrue(
                "Checkbox target is ${checkboxTouch.width.toDp()} by ${checkboxTouch.height.toDp()}",
                checkboxTouch.width.toDp() >= 48.dp && checkboxTouch.height.toDp() >= 48.dp
            )
            assertTrue(
                "Refresh target is ${refreshTouch.width.toDp()} by ${refreshTouch.height.toDp()}",
                refreshTouch.width.toDp() >= 48.dp && refreshTouch.height.toDp() >= 48.dp
            )
            // Overlapping targets let the later sibling swallow presses meant for its neighbour's
            // edge, and the hit test gives no warning when it happens.
            assertTrue(
                "Refresh and checkbox targets overlap by ${(refreshTouch.right - checkboxTouch.left).toDp()}",
                refreshTouch.right <= checkboxTouch.left
            )
        }

        // Every edge, right and bottom included: a target clipped by a neighbour loses the side
        // that faces it, which a press at the top left corner would never reveal. The presses are
        // injected in root coordinates because the touch bounds reach past the node's own bounds.
        listOf(checkboxTouch, refreshTouch).forEach { target ->
            listOf(
                Offset(target.left + 1f, target.center.y),
                Offset(target.center.x, target.top + 1f),
                Offset(target.right - 1f, target.center.y),
                Offset(target.center.x, target.bottom - 1f)
            ).forEach { point -> composeRule.onRoot().performTouchInput { click(point) } }
        }
        assertEquals(4, toggled.get())
        assertEquals(4, refreshed.get())
        // Neither control may let its press fall through to the header's expand action.
        assertEquals(0, expandTaps.get())
    }

    @Test
    fun ratingControlShowsCurrentValueAndExposesSaveAndRemoveActions() {
        val saved = AtomicBoolean(false)
        val removed = AtomicBoolean(false)
        setDetails(
            content(movie()).copy(
                rating = DetailRatingState.Ready(PersonalRating(10), selectedValue = 10)
            ),
            onSaveRating = { saved.set(true) },
            onRemoveRating = { removed.set(true) }
        )

        scrollTo(hasText(context.getString(R.string.detail_rating_value, 10)))
        composeRule.onNodeWithText(
            context.getString(R.string.detail_rating_value, 10)
        ).assertIsDisplayed().performClick()
        composeRule.onNode(
            SemanticsMatcher.expectValue(
                SemanticsProperties.StateDescription,
                context.getString(R.string.detail_rating_slider, 10)
            )
        ).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.detail_rating_set)).performScrollTo().performClick()
        composeRule.onNodeWithText(context.getString(R.string.detail_rating_remove)).performScrollTo().performClick()
        assertTrue(saved.get())
        assertTrue(removed.get())
    }

    @Test
    fun localProgressRatingAndSeasonReadStatesKeepMembershipAvailable() {
        val preview = DetailPersonalPreviewProvider().values.toList()
        val movie = preview[0]
        val series = preview[2]
        val cases = listOf(
            movie.copy(movieProgress = MovieProgressState.Loading) to R.string.detail_progress_loading,
            movie.copy(movieProgress = MovieProgressState.Error(AppError.LocalStorageFailure)) to
                R.string.error_local_storage,
            movie.copy(rating = DetailRatingState.Loading) to R.string.detail_rating_loading,
            movie.copy(rating = DetailRatingState.Error(AppError.LocalStorageFailure)) to R.string.error_local_storage,
            series.copy(series = series.series.copy(content = SeriesContentState.Loading)) to
                R.string.detail_seasons_loading,
            series.copy(
                series = series.series.copy(content = SeriesContentState.Error(AppError.LocalStorageFailure))
            ) to
                R.string.error_local_storage
        )
        var state by mutableStateOf(cases.first().first)
        setDetailsState({ state })
        cases.forEach { (next, status) ->
            composeRule.runOnIdle { state = next }
            val message = context.getString(status)
            scrollTo(hasText(message))
            composeRule.onNodeWithText(message).assertIsDisplayed()
            val membership = context.getString(R.string.library_action_remove)
            scrollTo(hasText(membership))
            composeRule.onNodeWithText(membership).assertIsDisplayed().assertHasClickAction()
            val ratingTitle = context.getString(R.string.detail_rating_title)
            scrollTo(hasText(ratingTitle))
            composeRule.onNodeWithText(ratingTitle).assertIsDisplayed()
        }
    }

    @Test
    fun loadingFullErrorRetryAndUnauthorizedSettingsAreActionable() {
        val retried = AtomicBoolean(false)
        val settings = AtomicBoolean(false)
        var state by mutableStateOf(
            MediaDetailsUiState(
                today = LocalDate.of(2026, 8, 18),
                content = DetailContentState.Loading
            )
        )
        setDetailsState(
            state = { state },
            onRetry = { retried.set(true) },
            onOpenSettings = { settings.set(true) }
        )
        composeRule.onNodeWithText(context.getString(R.string.detail_loading)).assertIsDisplayed()

        composeRule.runOnIdle {
            state = MediaDetailsUiState(
                today = LocalDate.of(2026, 8, 18),
                content = DetailContentState.Error(AppError.NetworkUnavailable)
            )
        }
        composeRule.onNodeWithText(context.getString(R.string.action_retry)).performClick()
        assertTrue(retried.get())

        composeRule.runOnIdle {
            state = MediaDetailsUiState(
                today = LocalDate.of(2026, 8, 18),
                content = DetailContentState.Error(AppError.Unauthorized)
            )
        }
        composeRule.onNodeWithText(context.getString(R.string.search_open_settings)).performClick()
        assertTrue(settings.get())
    }

    @Test
    fun staleRefreshErrorAndRefreshIndicatorKeepCachedContentVisible() {
        var state by mutableStateOf(
            content(movie()).copy(refresh = DetailRefreshState.Error(AppError.NetworkUnavailable))
        )
        setDetailsState({ state })
        composeRule.onNodeWithText("Movie title").assertIsDisplayed()
        composeRule.onNodeWithText(
            context.getString(R.string.detail_refresh_failed, context.getString(R.string.error_network_unavailable))
        ).assertIsDisplayed()

        composeRule.runOnIdle {
            state = content(movie()).copy(refresh = DetailRefreshState.Refreshing)
        }
        composeRule.onNodeWithText("Movie title").assertIsDisplayed()
    }

    @Test
    fun movieHeroKeepsTitleHeadingAndDecorativeArtwork() {
        assertHeroSemantics(movie())
    }

    @Test
    fun seriesHeroKeepsTitleHeadingAndDecorativeArtwork() {
        assertHeroSemantics(movie().copy(mediaType = MediaType.SERIES, runtime = null, numberOfSeasons = 3))
    }

    private fun assertHeroSemantics(media: MediaDetails) {
        val context = localizedTestContext
        val details = media.copy(
            title = "A very long localized title that exceeds the hero's two visible lines",
            originalTitle = "Distinct original title"
        )
        var state by mutableStateOf(content(details))
        setDetailsState({ state })

        // Both the missing-artwork Image and the loaded AsyncImage path keep identity in the Text.
        listOf(null, "android.resource://${context.packageName}/${R.drawable.poster_placeholder}").forEach { url ->
            composeRule.runOnIdle { state = content(details.copy(posterUrl = url, backdropUrl = url)) }
            composeRule.onAllNodes(hasText(details.title) and isHeading()).assertCountEquals(1)
            composeRule.onNodeWithText(details.title).assertIsDisplayed().assert(isHeading())
            composeRule.onAllNodes(
                hasContentDescription(details.title, substring = true),
                useUnmergedTree = true
            ).assertCountEquals(0)
            composeRule.onNodeWithText(context.getString(R.string.detail_original_title, details.originalTitle))
                .assertIsDisplayed()
            val typeLabel = context.getString(
                if (details.mediaType == MediaType.MOVIE) R.string.library_type_movie else R.string.library_type_tv
            )
            composeRule.onNode(hasText(typeLabel, substring = true) and hasText("2024", substring = true))
                .assertIsDisplayed()

            listOf(R.string.detail_back, R.string.detail_refresh).forEach { label ->
                composeRule.onNodeWithContentDescription(context.getString(label))
                    .assertIsDisplayed().assertHasClickAction()
                    .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            }
            composeRule.onNodeWithContentDescription(context.getString(R.string.favorite_add))
                .assertIsDisplayed().assertHasClickAction().assert(isToggleable())
        }
    }

    @Test
    fun libraryActionRemainsAccessibleWithMissingArtwork() {
        val toggled = AtomicBoolean(false)
        setDetails(content(movie().copy(posterUrl = null, backdropUrl = null)), onToggle = { toggled.set(true) })

        composeRule.onNodeWithText("Movie title").assertIsDisplayed().assert(isHeading())
        composeRule.onNodeWithContentDescription(context.getString(R.string.detail_back)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(context.getString(R.string.detail_refresh)).assertIsDisplayed()
        scrollTo(hasText(context.getString(R.string.library_action_add)))
        composeRule.onNodeWithText(context.getString(R.string.library_action_add)).performClick()
        assertTrue(toggled.get())
    }

    /**
     * Scrolls the screen's single lazy list until [matcher] matches. Items below the hero are not
     * composed until scrolled to, so [performScrollTo] alone cannot reach them.
     */
    private fun scrollTo(matcher: SemanticsMatcher) {
        composeRule.onNode(hasScrollAction()).performScrollToNode(matcher)
    }

    private fun setDetails(
        state: MediaDetailsUiState,
        onRetry: () -> Unit = {},
        onToggle: () -> Unit = {},
        onOpenSettings: () -> Unit = {},
        onToggleMovie: () -> Unit = {},
        onSaveRating: () -> Unit = {},
        onRemoveRating: () -> Unit = {},
        onToggleEpisode: (TrackedEpisode) -> Unit = {},
        onToggleSeasonWatched: (CachedSeason) -> Unit = {},
        onToggleSeasonExpanded: (CachedSeason) -> Unit = {},
        onRetrySeason: (CachedSeason) -> Unit = {},
        onToggleFavorite: () -> Unit = {},
        onSetWatchedDate: (LocalDate?) -> Unit = {},
        onSelectRating: (Int) -> Unit = {}
    ) = setDetailsState(
        { state },
        onRetry,
        onToggle,
        onOpenSettings,
        onToggleMovie,
        onSaveRating,
        onRemoveRating,
        onToggleEpisode,
        onToggleSeasonWatched,
        onToggleSeasonExpanded,
        onRetrySeason,
        onToggleFavorite,
        onSetWatchedDate,
        onSelectRating
    )

    private fun setDetailsState(
        state: () -> MediaDetailsUiState,
        onRetry: () -> Unit = {},
        onToggle: () -> Unit = {},
        onOpenSettings: () -> Unit = {},
        onToggleMovie: () -> Unit = {},
        onSaveRating: () -> Unit = {},
        onRemoveRating: () -> Unit = {},
        onToggleEpisode: (TrackedEpisode) -> Unit = {},
        onToggleSeasonWatched: (CachedSeason) -> Unit = {},
        onToggleSeasonExpanded: (CachedSeason) -> Unit = {},
        onRetrySeason: (CachedSeason) -> Unit = {},
        onToggleFavorite: () -> Unit = {},
        onSetWatchedDate: (LocalDate?) -> Unit = {},
        onSelectRating: (Int) -> Unit = {}
    ) {
        val requested = InstrumentationRegistry.getArguments()
        if (requested.getString("detailsWidthDp") != null) {
            configureTestResources(
                requireNotNull(requested.getString("detailsWidthDp")).toInt(),
                requested.getString("detailsFontScale")?.toFloat() ?: 1f,
                requested.getString("detailsTheme") == "dark"
            )
        }
        composeRule.setContent {
            val args = InstrumentationRegistry.getArguments()
            val base = LocalConfiguration.current
            val window = LocalWindowInfo.current.containerSize
            val width = args.getString("detailsWidthDp")?.toInt()
                ?: (window.width / LocalDensity.current.density).toInt()
            val font = args.getString("detailsFontScale")?.toFloat() ?: LocalDensity.current.fontScale
            val dark = when (args.getString("detailsTheme")) {
                "light" -> false
                "dark" -> true
                null -> isSystemInDarkTheme()
                else -> error("detailsTheme must be light or dark")
            }
            val density = window.width.toFloat() / width
            val configuration = Configuration(base).apply {
                screenWidthDp = width
                screenHeightDp = (window.height / density).toInt()
                fontScale = font
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                    if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
            }
            CompositionLocalProvider(
                LocalConfiguration provides configuration,
                LocalDensity provides Density(density, font)
            ) {
                BingeeTheme(darkTheme = dark) {
                    MediaDetailsContent(
                        state = state(),
                        onBack = {},
                        onRefresh = {},
                        onRetry = onRetry,
                        onToggleLibrary = onToggle,
                        onToggleFavorite = onToggleFavorite,
                        onSetWatchedDate = onSetWatchedDate,
                        onSelectRating = onSelectRating,
                        onToggleMovieWatched = onToggleMovie,
                        onSaveRating = onSaveRating,
                        onRemoveRating = onRemoveRating,
                        onToggleEpisode = onToggleEpisode,
                        onToggleSeasonWatched = onToggleSeasonWatched,
                        onToggleSeasonExpanded = onToggleSeasonExpanded,
                        onRetrySeason = onRetrySeason,
                        onDismissLibraryError = {},
                        onOpenSettings = onOpenSettings
                    )
                }
            }
        }
    }

    private fun content(details: MediaDetails) = MediaDetailsUiState(
        today = LocalDate.of(2026, 8, 18),
        content = DetailContentState.Content(
            CachedMediaDetails(details, Instant.parse("2026-08-01T10:00:00Z"), CacheFreshness.STALE)
        ),
        isInLibrary = false
    )

    private fun movie() = MediaDetails(
        externalRef = ExternalMediaRef(MediaSource.TMDB, "550"),
        mediaType = MediaType.MOVIE,
        title = "Movie title",
        overview = "Overview",
        releaseDate = LocalDate.of(2024, 1, 15),
        runtime = Duration.ofMinutes(120),
        productionStatus = ProductionStatus.RELEASED,
        genres = listOf(Genre("Drama"), Genre("Thriller"))
    )

    private fun season(number: Int, name: String, expanded: Boolean): CachedSeason {
        val seriesRef = ExternalMediaRef(MediaSource.TMDB, "1399")
        val seasonRef = ExternalMediaRef(MediaSource.TMDB, (900 + number).toString())
        val episodes = if (!expanded) {
            emptyList()
        } else {
            listOf(
                tracked(seriesRef, seasonRef, number, 1, "Watched episode", EpisodeWatchState.Watched(Instant.EPOCH)),
                tracked(seriesRef, seasonRef, number, 2, "Unwatched episode", EpisodeWatchState.Unwatched),
                tracked(seriesRef, seasonRef, number, 3, "Future episode", EpisodeWatchState.Unavailable)
            )
        }
        return CachedSeason(
            season = Season(seriesRef, seasonRef, number, name = name, episodeCount = episodes.size),
            metadataUpdatedAt = Instant.EPOCH,
            episodesFetchedAt = if (expanded) Instant.EPOCH else null,
            episodes = episodes,
            progress = if (expanded) SeasonProgress(1, 2, false) else SeasonProgress.EMPTY,
            episodeCacheFreshness = if (expanded) CacheFreshness.STALE else null
        )
    }

    /**
     * A series whose seasons are all already fetched, so every season header renders its refresh
     * and mark-watched controls while the cards stay collapsed.
     */
    private fun seriesState(seasons: List<CachedSeason>, status: ProductionStatus = ProductionStatus.RETURNING_SERIES) =
        content(
            movie().copy(
                mediaType = MediaType.SERIES,
                runtime = null,
                episodeRuntime = Duration.ofMinutes(50),
                numberOfSeasons = seasons.size,
                numberOfEpisodes = seasons.sumOf { it.season.episodeCount },
                productionStatus = status,
                externalRef = ExternalMediaRef(MediaSource.TMDB, "1399")
            )
        ).copy(
            libraryProgress = LibraryProgress.Series(deriveSeriesProgress(seasons)),
            series = SeriesDetailUiState(content = SeriesContentState.Ready(seasons, deriveSeriesProgress(seasons)))
        )

    private fun fetchedSeason(number: Int, name: String, watched: Int, trackable: Int, unaired: Int = 0): CachedSeason {
        val seriesRef = ExternalMediaRef(MediaSource.TMDB, "1399")
        val seasonRef = ExternalMediaRef(MediaSource.TMDB, (900 + number).toString())
        val episodes = (1..trackable).map { index ->
            tracked(
                seriesRef,
                seasonRef,
                number,
                index,
                "$name episode $index",
                if (index <= watched) EpisodeWatchState.Watched(Instant.EPOCH) else EpisodeWatchState.Unwatched
            )
        } + (1..unaired).map { index ->
            tracked(
                seriesRef,
                seasonRef,
                number,
                trackable + index,
                "$name future $index",
                EpisodeWatchState.Unavailable
            )
        }
        return CachedSeason(
            season = Season(seriesRef, seasonRef, number, name = name, episodeCount = episodes.size),
            metadataUpdatedAt = Instant.EPOCH,
            episodesFetchedAt = Instant.EPOCH,
            episodes = episodes,
            progress = deriveSeasonProgress(episodes),
            episodeCacheFreshness = CacheFreshness.FRESH
        )
    }

    private fun seasonCheckbox(watchState: String) =
        composeRule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, watchState))

    private fun assertSeasonState(seasonTitle: String, watchState: String) {
        scrollTo(hasText(seasonTitle))
        seasonCheckbox(watchState).assertIsDisplayed()
    }

    private fun tracked(
        seriesRef: ExternalMediaRef,
        seasonRef: ExternalMediaRef,
        seasonNumber: Int,
        number: Int,
        title: String,
        state: EpisodeWatchState
    ) = TrackedEpisode(
        Episode(
            seriesRef,
            seasonRef,
            ExternalMediaRef(MediaSource.TMDB, (1000 + number).toString()),
            seasonNumber,
            number,
            title,
            airDate = if (state == EpisodeWatchState.Unavailable) LocalDate.of(2027, 1, 1) else null
        ),
        state
    )
}
