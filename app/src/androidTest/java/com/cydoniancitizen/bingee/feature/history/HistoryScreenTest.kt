package com.cydoniancitizen.bingee.feature.history

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import com.cydoniancitizen.bingee.R
import com.cydoniancitizen.bingee.core.designsystem.theme.BingeeTheme
import com.cydoniancitizen.bingee.core.model.ExternalMediaRef
import com.cydoniancitizen.bingee.core.model.HistoryEpisode
import com.cydoniancitizen.bingee.core.model.HistoryEpisodePage
import com.cydoniancitizen.bingee.core.model.HistoryFilter
import com.cydoniancitizen.bingee.core.model.HistoryItem
import com.cydoniancitizen.bingee.core.model.HistoryOverview
import com.cydoniancitizen.bingee.core.model.HistoryPage
import com.cydoniancitizen.bingee.core.model.MediaSource
import com.cydoniancitizen.bingee.core.model.MediaType
import com.cydoniancitizen.bingee.core.result.AppError
import com.cydoniancitizen.bingee.core.ui.formatLocalized
import com.cydoniancitizen.bingee.testutil.TestLocaleRule
import com.cydoniancitizen.bingee.testutil.localizedTestContext
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HistoryScreenTest {
    @get:Rule(order = 0)
    val locale = TestLocaleRule()

    @get:Rule(order = 1)
    val compose = createComposeRule()
    private val context get() = localizedTestContext
    private val day = LocalDate.of(2026, 10, 2)

    @Test fun compactCardsLeaveTheNeighbourVisibleAndOmitTheRepeatedType() {
        val row =
            HistoryItem(day, 1, MediaType.MOVIE, "Compact movie", null, ExternalMediaRef(MediaSource.TMDB, "1"), 0)
        var opened = false
        compose.setContent {
            NativeVariant {
                HistoryContent(
                    HistoryUiState(
                        loading = false,
                        page = HistoryPage(listOf(row, row.copy(mediaId = 2, title = "Neighbour")))
                    ),
                    onBack = {},
                    onOpenDetails = { _, _ -> opened = true }
                )
            }
        }
        val card = compose.onNodeWithTag("history-card-${row.key}")
        val bounds = card.fetchSemanticsNode().boundsInRoot
        val list = compose.onNodeWithTag("history-list").fetchSemanticsNode().boundsInRoot
        assertTrue(bounds.width < list.width * 0.7f)
        assertTrue(bounds.height < list.height * 0.3f)
        val oldType = if (context.resources.configuration.locales[0].language == "it") "Film" else "Movie"
        compose.onAllNodes(
            hasText(oldType) and
                hasAnyAncestor(hasTestTag("history-card-${row.key}"))
        ).assertCountEquals(0)
        compose.onNodeWithText(row.title).performClick()
        compose.runOnIdle { assertTrue(opened) }
    }

    @Test fun datesAreCentredCloseToTheirPointsOnBothSides() {
        val rows = (1L..2L).map { HistoryItem(day.plusDays(it), it, MediaType.MOVIE, "Point $it", null, null, 0) }
        compose.setContent {
            NativeVariant {
                HistoryContent(
                    HistoryUiState(loading = false, page = HistoryPage(rows)),
                    onBack = {},
                    onOpenDetails = { _, _ -> }
                )
            }
        }
        rows.forEachIndexed { index, row ->
            compose.onNodeWithTag("history-list").performScrollToIndex(index)
            val rail = compose.onNodeWithTag("history-rail-${row.key}").fetchSemanticsNode().boundsInRoot
            val date = compose.onNodeWithText(row.day.formatLocalized(context.resources.configuration.locales[0]))
            val bounds = date.fetchSemanticsNode().boundsInRoot
            val layouts = mutableListOf<TextLayoutResult>()
            date.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            val layout = layouts.single()
            val centreX = bounds.left + (layout.getLineLeft(0) + layout.getLineRight(0)) / 2
            assertEquals(rail.center.x, centreX, 2f)
            val textY =
                bounds.top + if (index == 0) layout.getLineBottom(layout.lineCount - 1) else layout.getLineTop(0)
            val distance = if (index == 0) rail.center.y - textY else textY - rail.center.y
            assertTrue(distance >= 0 && distance <= rail.height)
        }
    }

    @Test fun expandedGroupOffersToHideEpisodesAndCanBeCollapsedAgain() {
        val row = HistoryItem(day, 1, MediaType.SERIES, "Series", null, null, 2)
        var state by mutableStateOf(
            HistoryUiState(
                filter = HistoryFilter.EPISODES,
                loading = false,
                page = HistoryPage(listOf(row))
            )
        )
        compose.setContent {
            BingeeTheme {
                HistoryContent(state, onBack = {}, onOpenDetails = { _, _ -> }, onToggleGroup = {
                    state =
                        state.copy(
                            groups = if (state.groups.isEmpty()) {
                                mapOf(
                                    row.key to HistoryGroupState(false)
                                )
                            } else {
                                emptyMap()
                            }
                        )
                })
            }
        }
        val show = context.resources.getQuantityString(R.plurals.history_episode_count, 2, 2)
        val hide = context.getString(R.string.history_hide_episodes)
        compose.onNodeWithText(show).performScrollTo().performClick()
        compose.onNodeWithText(hide).performScrollTo().performClick()
        compose.onNodeWithText(show).assertIsDisplayed()
    }

    @Test fun emptyCopyDescribesOnlyTheSelectedMediaType() {
        var state by mutableStateOf(HistoryUiState(loading = false))
        compose.setContent { BingeeTheme { HistoryContent(state, onBack = {}, onOpenDetails = { _, _ -> }) } }
        compose.onNodeWithText(context.getString(R.string.history_empty_movies)).assertIsDisplayed()
        compose.runOnIdle { state = state.copy(filter = HistoryFilter.EPISODES) }
        compose.onNodeWithText(context.getString(R.string.history_empty_episodes)).assertIsDisplayed()
    }

    @Test fun onlyMoviesAndSeriesFiltersAreAvailable() {
        var state by mutableStateOf(HistoryUiState(loading = false))
        val selected = mutableListOf<HistoryFilter>()
        compose.setContent {
            BingeeTheme {
                HistoryContent(state, onBack = {}, onOpenDetails = { _, _ -> }, onFilter = {
                    selected += it
                    state = state.copy(filter = it)
                })
            }
        }
        compose.onAllNodes(isSelectable()).assertCountEquals(2)
        compose.onNodeWithText(context.getString(R.string.history_movies)).assertIsSelected()
        compose.onNodeWithText(context.getString(R.string.history_episodes)).performClick().assertIsSelected()
        compose.onNodeWithText(context.getString(R.string.history_movies)).performClick().assertIsSelected()
        compose.runOnIdle { assertEquals(listOf(HistoryFilter.EPISODES, HistoryFilter.MOVIES), selected) }
    }

    @Test fun consecutiveItemsAlternateAroundOneHorizontalRail() {
        val rows = (1L..3L).map { HistoryItem(day.plusDays(it), it, MediaType.MOVIE, "Point $it", null, null, 0) }
        compose.setContent {
            NativeVariant {
                HistoryContent(
                    HistoryUiState(loading = false, page = HistoryPage(rows)),
                    onBack = {},
                    onOpenDetails = { _, _ -> }
                )
            }
        }
        var railY: Float? = null
        rows.forEachIndexed { index, row ->
            compose.onNodeWithTag("history-list").performScrollToIndex(index)
            val y = assertPointSide(row, above = index % 2 == 0)
            railY?.let { assertEquals(it, y, 1f) }
            railY = y
            compose.onNodeWithText(row.title).assertIsDisplayed()
        }
        val target = InstrumentationRegistry.getInstrumentation().targetContext
        File(target.filesDir, "history-alternating.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    private fun assertPointSide(row: HistoryItem, above: Boolean): Float {
        val rail = compose.onNodeWithTag("history-rail-${row.key}").fetchSemanticsNode().boundsInRoot
        val card = compose.onNodeWithTag("history-card-${row.key}").fetchSemanticsNode().boundsInRoot
        if (above) assertTrue(card.bottom <= rail.top) else assertTrue(card.top >= rail.bottom)
        return rail.center.y
    }

    @Test fun retainedActivityScrollsHorizontallyWithoutPrimaryPageControls() {
        val row = HistoryItem(day, 1, MediaType.MOVIE, "First retained movie", null, null, 0)
        compose.setContent {
            BingeeTheme {
                HistoryContent(
                    HistoryUiState(loading = false, page = HistoryPage(listOf(row), hasNext = true)),
                    onBack = {},
                    onOpenDetails = { _, _ -> }
                )
            }
        }
        compose.onNodeWithTag("history-list").assert(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.HorizontalScrollAxisRange)
        )
        compose.onNodeWithText(context.getString(R.string.history_next_page)).assertDoesNotExist()
    }

    @Test fun readyAfterLoadingPrefetchesAtTheUnchangedViewportEdge() {
        val row = HistoryItem(day, 1, MediaType.MOVIE, "Retained movie", null, null, 0)
        var state by mutableStateOf(
            HistoryUiState(
                loading = false,
                loadingMore = true,
                page = HistoryPage(listOf(row), hasNext = true)
            )
        )
        val requests = mutableListOf<Boolean>()
        var anchor: String? = null
        compose.setContent {
            BingeeTheme {
                HistoryContent(
                    state,
                    onBack = {},
                    onOpenDetails = { _, _ -> },
                    onLoadWindow = { requests += it },
                    onAnchor = { key, _ -> anchor = key }
                )
            }
        }
        compose.runOnIdle {
            assertTrue(requests.isEmpty())
            state = state.copy(loadingMore = false)
        }
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(listOf(false), requests)
            assertEquals(row.key, anchor)
        }
    }

    @Test fun automaticForwardAndReverseWindowsKeepTheVisiblePoint() {
        val rows = (1L..120L).map { HistoryItem(day, it, MediaType.MOVIE, "Point $it", null, null, 0) }
        var state by mutableStateOf(HistoryUiState(loading = false, page = HistoryPage(rows.take(90), hasNext = true)))
        val requests = mutableListOf<Boolean>()
        compose.setContent {
            BingeeTheme {
                HistoryContent(state, onBack = {}, onOpenDetails = { _, _ -> }, onLoadWindow = { backwards ->
                    requests += backwards
                    state = state.copy(
                        page = if (backwards) {
                            HistoryPage(rows.take(90), hasNext = true)
                        } else {
                            HistoryPage(rows.drop(30), hasPrevious = true)
                        }
                    )
                })
            }
        }
        compose.onNodeWithTag("history-list").performScrollToIndex(85)
        compose.onNodeWithText("Point 86").assertIsDisplayed()
        val railY = assertPointSide(rows[85], above = false)
        compose.runOnIdle { assertEquals(listOf(false), requests) }
        compose.onNodeWithTag("history-list").performScrollToIndex(0)
        compose.onNodeWithText("Point 31").assertIsDisplayed()
        assertEquals(railY, assertPointSide(rows[30], above = true), 1f)
        compose.runOnIdle { assertEquals(listOf(false, true), requests) }
    }

    @Test fun newlyRetainedActivityPrefetchesFromTheIdleEndOfAFullWindow() {
        val rows = (1L..90L).map { HistoryItem(day, it, MediaType.MOVIE, "Point $it", null, null, 0) }
        var state by mutableStateOf(HistoryUiState(loading = false, page = HistoryPage(rows)))
        val requests = mutableListOf<Boolean>()
        compose.setContent {
            BingeeTheme {
                HistoryContent(state, onBack = {}, onOpenDetails = { _, _ -> }, onLoadWindow = { requests += it })
            }
        }
        compose.onNodeWithTag("history-list").performScrollToIndex(89)
        compose.runOnIdle {
            assertTrue(requests.isEmpty())
            state = state.copy(page = state.page.copy(hasNext = true))
        }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(listOf(false), requests) }
    }

    @Test fun groupExpansionNavigationAndInformationAreReachable() {
        val row =
            HistoryItem(day, 1, MediaType.SERIES, "Series long title", null, ExternalMediaRef(MediaSource.TMDB, "1"), 2)
        var state by mutableStateOf(
            HistoryUiState(
                filter = HistoryFilter.EPISODES,
                loading = false,
                page = HistoryPage(listOf(row)),
                overview = HistoryOverview(day, day)
            )
        )
        var opened = false
        compose.setContent {
            BingeeTheme {
                HistoryContent(state = state, onBack = {}, onOpenDetails = { _, _ -> opened = true }, onToggleGroup = {
                    state =
                        state.copy(
                            groups = mapOf(
                                row.key to
                                    HistoryGroupState(
                                        false,
                                        HistoryEpisodePage(
                                            listOf(
                                                HistoryEpisode(1, 0, 1, "Special title"),
                                                HistoryEpisode(2, 1, 1, "Regular title")
                                            )
                                        )
                                    )
                            )
                        )
                })
            }
        }
        compose.onNodeWithText("Series long title").performClick()
        compose.runOnIdle { assertTrue(opened) }
        compose.onNodeWithTag("history-group-${row.key}").performScrollTo().performClick()
        compose.onNodeWithText("Special title", substring = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription(context.getString(R.string.history_information)).performClick()
        compose.onNodeWithText(context.getString(R.string.history_information_body)).assertIsDisplayed()
    }

    @Test
    fun adaptiveLongTitlesOpenGroupsAndPaginationRemainReachable() {
        val row = HistoryItem(
            day,
            1,
            MediaType.SERIES,
            "Una serie dal titolo molto lungo per verificare leggibilità e gerarchia sul dispositivo",
            null,
            ExternalMediaRef(MediaSource.TMDB, "1"),
            100000
        )
        val child = HistoryEpisode(1, 0, 1, "Un titolo molto lungo anche per lo speciale e per le traduzioni")
        val rows = listOf(row, row.copy(day = day.plusDays(1), mediaId = 2))
        val state = HistoryUiState(
            filter = HistoryFilter.EPISODES,
            loading = false,
            page = HistoryPage(rows, hasNext = true),
            overview = HistoryOverview(day, day),
            groups = rows.associate {
                it.key to
                    HistoryGroupState(false, HistoryEpisodePage(listOf(child), hasNext = true))
            }
        )
        var paged = 0
        compose.setContent {
            NativeVariant {
                HistoryContent(state, onBack = {}, onOpenDetails = { _, _ -> }, onGroupPage = { _, _ -> paged++ })
            }
        }
        rows.forEachIndexed { index, point ->
            compose.onNodeWithTag("history-list").performScrollToIndex(index)
            assertPointSide(point, above = index == 0)
            val inCard = hasAnyAncestor(hasTestTag("history-card-${point.key}"))
            compose.onNode(hasText(child.title, substring = true) and inCard).performScrollTo().assertIsDisplayed()
            compose.onNode(hasText(context.getString(R.string.history_next_episodes)) and inCard)
                .performScrollTo().performClick()
        }
        compose.runOnIdle { assertEquals(2, paged) }
        val target = InstrumentationRegistry.getInstrumentation().targetContext
        File(target.filesDir, "history-adaptive.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        compose.onNodeWithText(context.getString(R.string.history_next_page)).assertDoesNotExist()
        compose.onNodeWithContentDescription(context.getString(R.string.history_information)).performClick()
        compose.onNodeWithText(context.getString(R.string.action_dismiss)).assertIsDisplayed().performClick()
    }

    @Composable
    private fun NativeVariant(content: @Composable () -> Unit) {
        val args = InstrumentationRegistry.getArguments()
        val width = args.getString("historyWidthDp")?.toInt() ?: 411
        val font = args.getString("historyFontScale")?.toFloat() ?: 1f
        val dark = args.getString("historyTheme") == "dark"
        val window = LocalWindowInfo.current.containerSize
        val density = window.width.toFloat() / width
        val config = Configuration(LocalConfiguration.current).apply {
            screenWidthDp = width
            screenHeightDp = (window.height / density).toInt()
            fontScale = font
        }
        CompositionLocalProvider(LocalConfiguration provides config, LocalDensity provides Density(density, font)) {
            BingeeTheme(darkTheme = dark) { content() }
        }
    }

    @Test fun loadingEmptyErrorsAndRetryRemainDistinct() {
        var state by mutableStateOf(HistoryUiState())
        var retries = 0
        compose.setContent {
            BingeeTheme { HistoryContent(state, onBack = {}, onOpenDetails = { _, _ -> }, onRetry = { retries++ }) }
        }
        compose.onNodeWithTag("history-loading").assertExists()
        compose.runOnIdle { state = state.copy(loading = false) }
        compose.onNodeWithText(context.getString(R.string.history_empty_movies)).assertIsDisplayed()
        compose.runOnIdle { state = state.copy(error = AppError.LocalStorageFailure) }
        compose.onNodeWithText(context.getString(R.string.action_retry)).performClick()
        compose.runOnIdle { assertEquals(1, retries) }
    }
}
