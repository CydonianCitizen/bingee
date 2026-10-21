package com.cydoniancitizen.bingee.feature.details

import android.app.UiAutomation
import android.os.Bundle
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.test.platform.app.InstrumentationRegistry
import com.cydoniancitizen.bingee.R
import com.cydoniancitizen.bingee.core.designsystem.theme.BingeeTheme
import com.cydoniancitizen.bingee.core.model.MovieWatchState
import com.cydoniancitizen.bingee.testutil.TestLocaleRule
import com.cydoniancitizen.bingee.testutil.localizedTestContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Native accessibility proof; requireTalkBack=true also requires the real service and exploration. */
class MediaDetailsAccessibilityTest {
    @get:Rule(order = 0)
    val locale = TestLocaleRule()

    @get:Rule(order = 1)
    val compose = createComposeRule()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = localizedTestContext
    private val requireTalkBack get() = InstrumentationRegistry.getArguments().getString("requireTalkBack") == "true"
    private lateinit var automation: UiAutomation

    @Test fun movieActionsExposeNativeRolesStateAndTouchTargets() {
        var state by mutableStateOf(fixture(0).copy(isInLibrary = false))
        var favoriteWrites = 0
        var movieWrites = 0
        compose.setContent {
            BingeeTheme {
                MediaDetailsContent(
                    state, onBack = {}, onRefresh = {}, onRetry = {}, onToggleLibrary = {},
                    onDismissLibraryError = {}, onOpenSettings = {},
                    onToggleFavorite = {
                        favoriteWrites++
                        state = state.copy(isFavorite = !state.isFavorite)
                    },
                    onToggleMovieWatched = {
                        movieWrites++
                        state = state.copy(movieProgress = fixture(1).movieProgress)
                    }
                )
            }
        }
        connect()
        val favorite = action(context.getString(R.string.favorite_add))
        assertTrue(favorite.isCheckable && !favorite.isChecked)
        focus(favorite)
        assertTrue(favorite.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        compose.waitUntil { state.isFavorite }
        compose.waitUntil(5000) { action(context.getString(R.string.favorite_remove)).isChecked }
        assertEquals(1, favoriteWrites)
        compose.onNode(hasScrollAction()).performScrollToKey("personal")
        val membership = action(context.getString(R.string.library_action_add))
        assertButton(context.getString(R.string.library_action_add), membership)
        val watchLabel = context.getString(R.string.detail_mark_watched)
        val watch = action(watchLabel)
        assertButton(watchLabel, watch)
        val ratingTitle = context.getString(R.string.detail_rating_title)
        assertTrue(node { it.text?.toString() == ratingTitle }.isHeading)
        focus(watch)
        assertTrue(action(watchLabel).performAction(AccessibilityNodeInfo.ACTION_CLICK))
        compose.waitUntil { movieWrites == 1 }
        assertTrue((state.movieProgress as MovieProgressState.Ready).state is MovieWatchState.Watched)
        assertEquals(1, movieWrites)
        scrollTo(context.getString(R.string.detail_mark_unwatched))
        action(context.getString(R.string.detail_mark_unwatched))
    }

    @Test fun ratingAndDateEditorsRemainReachableAndCancelWithoutSaving() {
        var state by mutableStateOf(fixture(1))
        var writes = 0
        compose.setContent {
            BingeeTheme {
                MediaDetailsContent(
                    state, onBack = {}, onRefresh = {}, onRetry = {}, onToggleLibrary = {},
                    onDismissLibraryError = {}, onOpenSettings = {},
                    onSelectRating = {
                        state =
                            state.copy(rating = (state.rating as DetailRatingState.Ready).copy(selectedValue = it))
                    },
                    onSaveRating = { writes++ }, onRemoveRating = { writes++ }, onSetWatchedDate = { writes++ }
                )
            }
        }
        connect()
        val rating = context.getString(R.string.detail_rating_value, 8)
        scrollTo(rating)
        assertTrue(action(rating).performAction(AccessibilityNodeInfo.ACTION_CLICK))
        compose.waitUntil { compose.onNodeWithText(context.getString(R.string.detail_rating_set)).isDisplayed() }
        val slider = node { it.rangeInfo != null }
        assertEquals(1f, slider.rangeInfo!!.min, 0f)
        assertEquals(10f, slider.rangeInfo!!.max, 0f)
        assertTrue(slider.stateDescription.toString().contains(context.getString(R.string.detail_rating_slider, 8)))
        val sliderTarget = compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
            .fetchSemanticsNode().touchBoundsInRoot
        val minimumTarget = 48 * compose.density.density
        assertTrue(sliderTarget.width >= minimumTarget)
        // Slider semantics describe the 44 dp thumb. Prove the actual 48 dp touch region instead.
        if (!requireTalkBack) {
            val origin = compose.onRoot().fetchSemanticsNode().boundsInRoot.topLeft
            listOf(0.25f to -1f, 0.75f to 1f).forEach { (fraction, edge) ->
                val previous = (state.rating as DetailRatingState.Ready).selectedValue
                val y = sliderTarget.center.y + edge * (minimumTarget / 2 - 1)
                compose.onRoot().performTouchInput {
                    click(Offset(sliderTarget.left + sliderTarget.width * fraction, y) - origin)
                }
                compose.waitUntil(3000) { (state.rating as DetailRatingState.Ready).selectedValue != previous }
            }
        }
        assertTrue(slider.isVisibleToUser && slider.isScreenReaderFocusable)
        assertTrue(
            slider.performAction(
                AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.id,
                Bundle().apply {
                    putFloat(AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE, 9f)
                }
            )
        )
        compose.waitUntil { (state.rating as DetailRatingState.Ready).selectedValue == 9 }
        scrollTo(context.getString(R.string.action_cancel))
        assertTrue(action(context.getString(R.string.action_cancel)).performAction(AccessibilityNodeInfo.ACTION_CLICK))
        compose.waitForIdle()
        scrollTo(rating)
        focus(action(rating))
        assertEquals(8, (state.rating as DetailRatingState.Ready).rating?.value)
        assertEquals(8, (state.rating as DetailRatingState.Ready).selectedValue)
        val edit = context.getString(R.string.watched_date_edit)
        scrollTo(edit)
        assertTrue(action(edit).performAction(AccessibilityNodeInfo.ACTION_CLICK))
        val cancel = context.getString(R.string.action_cancel)
        compose.waitUntil { compose.onNodeWithText(cancel).isDisplayed() }
        action(context.getString(R.string.action_save))
        assertTrue(action(cancel).performAction(AccessibilityNodeInfo.ACTION_CLICK))
        compose.waitUntil { compose.onAllNodesWithText(cancel).fetchSemanticsNodes().isEmpty() }
        focus(action(edit))
        assertEquals(0, writes)
    }

    @Test fun abandonedSeriesResumeAndSeasonControlsKeepNativeActionsDistinct() {
        var state by mutableStateOf(fixture(4))
        var resumes = 0
        compose.setContent {
            BingeeTheme {
                MediaDetailsContent(
                    state, onBack = {}, onRefresh = {}, onRetry = {}, onToggleLibrary = {},
                    onDismissLibraryError = {}, onOpenSettings = {},
                    onToggleSeriesAbandoned = {
                        resumes++
                        state = state.copy(isAbandoned = false)
                    },
                    onToggleSeasonExpanded = { cached ->
                        state =
                            state.copy(series = state.series.copy(expandedSeasons = setOf(cached.season.externalRef)))
                    }
                )
            }
        }
        connect()
        val resume = context.getString(R.string.series_tracking_restore)
        scrollTo(resume)
        val primary = action(resume)
        assertButton(resume, primary)
        focus(primary)
        assertTrue(primary.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        compose.waitUntil { !state.isAbandoned }
        assertEquals(1, resumes)
        action(context.getString(R.string.library_action_remove))
        val season = (state.series.content as SeriesContentState.Ready).seasons.first()
        val name = season.season.name!!
        val expand = context.getString(R.string.detail_expand_season, name)
        scrollTo(name)
        assertTrue(action(expand).performAction(AccessibilityNodeInfo.ACTION_CLICK))
        compose.waitForIdle()
        action(context.getString(R.string.detail_collapse_season, name))
        val watchedAction = context.getString(R.string.detail_mark_season_watched)
        val bulk = action(watchedAction)
        assertTrue(bulk.isCheckable)
        assertTrue(bulk.stateDescription?.isNotBlank() == true)
        compose.onAllNodesWithText(resume).assertCountEquals(0)
    }

    private fun connect() {
        automation = instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        if (requireTalkBack) {
            val manager = context.getSystemService(AccessibilityManager::class.java)
            compose.waitUntil(10000) {
                manager.isTouchExplorationEnabled && manager.getEnabledAccessibilityServiceList(-1).any {
                    it.resolveInfo.serviceInfo.packageName == "com.google.android.marvin.talkback"
                }
            }
        }
        compose.waitUntil(5000) { automation.rootInActiveWindow?.packageName?.toString() == context.packageName }
    }

    private fun root(): AccessibilityNodeInfo = requireNotNull(automation.rootInActiveWindow)

    private fun node(predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo {
        fun find(value: AccessibilityNodeInfo): AccessibilityNodeInfo? {
            // Compose can update a virtual node before UiAutomation invalidates its cached tree.
            value.refresh()
            if (predicate(value)) return value
            for (index in 0 until value.childCount) {
                value.getChild(index)?.let { child ->
                    find(child)?.let { return it }
                }
            }
            return null
        }
        var found: AccessibilityNodeInfo? = null
        try {
            compose.waitUntil(5000) {
                found = automation.rootInActiveWindow?.let(::find)
                found != null
            }
        } catch (error: Throwable) {
            fun describe(value: AccessibilityNodeInfo): String = buildString {
                append(
                    "${value.className}: text=${value.text}; range=${value.rangeInfo}; state=${value.stateDescription}\n"
                )
                for (index in 0 until value.childCount) value.getChild(index)?.let { append(describe(it)) }
            }
            throw AssertionError(describe(root()).take(7000), error)
        }
        return requireNotNull(found)
    }

    private fun action(label: String): AccessibilityNodeInfo {
        compose.waitForIdle()
        val labelNode = node {
            it.text?.toString() == label || it.contentDescription?.toString() == label ||
                it.actionList.any { action ->
                    action.id == AccessibilityNodeInfo.ACTION_CLICK &&
                        action.label?.toString() == label
                }
        }
        val result = generateSequence(labelNode) { it.parent }.onEach { it.refresh() }.first { it.isClickable }
        assertTrue("Action '$label' must be visible and enabled", result.isVisibleToUser && result.isEnabled)
        val matchingAction = hasText(label) or hasContentDescription(label) or SemanticsMatcher("click label $label") {
            it.config.getOrNull(SemanticsActions.OnClick)?.label == label
        }
        val target = compose.onNode(matchingAction).fetchSemanticsNode().touchBoundsInRoot
        val minimum = 48 * compose.density.density
        assertTrue(
            "Touch target below 48 dp for '$label': $target",
            target.width >= minimum && target.height >= minimum
        )
        return result
    }

    private fun scrollTo(label: String) {
        compose.onNode(hasScrollAction()).performScrollToNode(hasText(label))
        val bounds = compose.onNodeWithText(label).fetchSemanticsNode().touchBoundsInRoot
        val viewport = compose.onRoot().fetchSemanticsNode().boundsInRoot
        compose.onNode(hasScrollAction()).performSemanticsAction(SemanticsActions.ScrollBy) {
            it(0f, bounds.center.y - viewport.center.y)
        }
        compose.onNodeWithText(label).assertIsDisplayed()
    }

    private fun assertButton(label: String, native: AccessibilityNodeInfo) {
        compose.onNodeWithText(label).assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        assertTrue(native.isScreenReaderFocusable && native.isClickable)
    }

    private fun focus(value: AccessibilityNodeInfo) {
        if (requireTalkBack) assertTrue(value.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS))
    }

    private fun fixture(index: Int): MediaDetailsUiState {
        val original = DetailPersonalPreviewProvider().values.elementAt(index)
        val cached = (original.content as DetailContentState.Content).cached
        val ready = original.series.content as? SeriesContentState.Ready
        return original.copy(
            content = DetailContentState.Content(
                cached.copy(details = cached.details.copy(posterUrl = null, backdropUrl = null))
            ),
            series = original.series.copy(
                content = ready?.copy(
                    seasons = ready.seasons.map { season ->
                        season.copy(
                            season = season.season.copy(posterUrl = null),
                            episodes = season.episodes.map { tracked ->
                                tracked.copy(episode = tracked.episode.copy(stillUrl = null))
                            }
                        )
                    }
                ) ?: original.series.content
            )
        )
    }
}
