package com.cydoniancitizen.bingee.feature.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.cydoniancitizen.bingee.R
import com.cydoniancitizen.bingee.core.designsystem.theme.BingeeTheme
import com.cydoniancitizen.bingee.core.model.ExternalMediaRef
import com.cydoniancitizen.bingee.core.model.Genre
import com.cydoniancitizen.bingee.core.model.MediaSource
import com.cydoniancitizen.bingee.core.model.MediaType
import com.cydoniancitizen.bingee.core.model.PersonalRating
import com.cydoniancitizen.bingee.core.model.PersonalViewingEntry
import com.cydoniancitizen.bingee.domain.model.GenreStatistic
import com.cydoniancitizen.bingee.domain.model.MonthlyViewingData
import com.cydoniancitizen.bingee.domain.model.MonthlyViewingStatistics
import com.cydoniancitizen.bingee.domain.model.PersonalRatingStatistics
import com.cydoniancitizen.bingee.domain.model.RatingHistogramBucket
import com.cydoniancitizen.bingee.domain.model.StatisticsMediaScope
import com.cydoniancitizen.bingee.domain.model.TasteStatistics
import com.cydoniancitizen.bingee.domain.model.WatchedStatistics
import com.cydoniancitizen.bingee.domain.model.calculateTasteStatistics
import com.cydoniancitizen.bingee.domain.model.calculateWatchedStatistics
import com.cydoniancitizen.bingee.testutil.STATISTICS_RATINGS_ITEM
import com.cydoniancitizen.bingee.testutil.TestLocaleRule
import com.cydoniancitizen.bingee.testutil.localizedTestContext
import com.cydoniancitizen.bingee.testutil.scrollListTo
import com.cydoniancitizen.bingee.testutil.scrollListToItem
import com.cydoniancitizen.bingee.testutil.scrollRowTo
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class StatisticsScreenTest {
    @get:Rule(order = 0)
    val localeRule = TestLocaleRule()

    private val context get() = localizedTestContext

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    @Test
    fun scopeRetainsPressCancellationAndKeyboardFeedbackInLightTheme() = assertScopeFeedback(darkTheme = false)

    @Test
    fun scopeRetainsPressCancellationAndKeyboardFeedbackInDarkTheme() = assertScopeFeedback(darkTheme = true)

    @Test
    fun rapidScopeChangesKeepOneSelectionInLightTheme() = assertRapidScopeChanges(darkTheme = false)

    @Test
    fun rapidScopeChangesKeepOneSelectionInDarkTheme() = assertRapidScopeChanges(darkTheme = true)

    private fun assertRapidScopeChanges(darkTheme: Boolean) {
        val changes = mutableListOf<StatisticsMediaScope>()
        val entries = listOf(
            viewing(
                "movie",
                MediaType.MOVIE,
                listOf(genre("Drama", 18), genre("Comedy", 35), genre("Thriller", 53))
            ),
            viewing(
                "series",
                MediaType.SERIES,
                listOf(genre("Horror", 27), genre("Crime", 80), genre("Romance", 10749))
            )
        )
        composeRule.setContent {
            var scope by remember { mutableStateOf(StatisticsMediaScope.ALL) }
            BingeeTheme(darkTheme = darkTheme) {
                Surface {
                    StatisticsContent(
                        tasteStatistics = calculateTasteStatistics(entries, scope),
                        onScopeChanged = {
                            changes += it
                            scope = it
                        }
                    )
                }
            }
        }
        val labels = mapOf(
            StatisticsMediaScope.ALL to context.getString(R.string.statistics_filter_all),
            StatisticsMediaScope.MOVIES to context.getString(R.string.statistics_filter_movies),
            StatisticsMediaScope.SERIES to context.getString(R.string.statistics_filter_series)
        )
        val scopeNodes = labels.values.map(::scopeSelector).reduce { first, next -> first or next }
        val radarGenres = mapOf(
            StatisticsMediaScope.ALL to "Drama, Horror, Comedy, Thriller, Crime, Romance",
            StatisticsMediaScope.MOVIES to "Drama, Comedy, Thriller",
            StatisticsMediaScope.SERIES to "Horror, Crime, Romance"
        )
        fun radar(scope: StatisticsMediaScope) = composeRule.onNodeWithContentDescription(
            context.getString(R.string.statistics_radar_description, radarGenres.getValue(scope))
        )
        val initialRadar = radar(StatisticsMediaScope.ALL).captureToImage().toPixelMap()
        fun changedRadarPixels(scope: StatisticsMediaScope): Int {
            val current = radar(scope).captureToImage().toPixelMap()
            assertEquals(initialRadar.width, current.width)
            assertEquals(initialRadar.height, current.height)
            return (0 until initialRadar.height).sumOf { y ->
                (0 until initialRadar.width).count { x -> initialRadar[x, y] != current[x, y] }
            }
        }
        val requests = listOf(
            StatisticsMediaScope.MOVIES,
            StatisticsMediaScope.SERIES,
            StatisticsMediaScope.MOVIES,
            StatisticsMediaScope.ALL,
            StatisticsMediaScope.SERIES,
            StatisticsMediaScope.ALL
        )
        composeRule.mainClock.autoAdvance = false
        requests.forEach { scope ->
            composeRule.onNode(scopeSelector(labels.getValue(scope))).performClick()
            composeRule.mainClock.advanceTimeByFrame()
            composeRule.onNode(scopeSelector(labels.getValue(scope))).assertIsSelected()
            composeRule.onAllNodes(scopeNodes and isSelected()).assertCountEquals(1)
            radar(scope).assertIsDisplayed()
            if (scope == StatisticsMediaScope.MOVIES) {
                assertTrue("The rendered radar must follow the new filter", changedRadarPixels(scope) > 0)
            }
        }
        composeRule.mainClock.advanceTimeBy(1_000)
        composeRule.onAllNodes(scopeNodes and isSelected()).assertCountEquals(1)
        assertEquals(0, changedRadarPixels(StatisticsMediaScope.ALL))
        composeRule.runOnIdle { assertEquals(requests, changes) }
    }

    private fun assertScopeFeedback(darkTheme: Boolean) {
        val changes = mutableListOf<StatisticsMediaScope>()
        lateinit var inputModeManager: InputModeManager
        composeRule.setContent {
            inputModeManager = LocalInputModeManager.current
            var scope by remember { mutableStateOf(StatisticsMediaScope.ALL) }
            BingeeTheme(darkTheme = darkTheme) {
                Surface {
                    StatisticsContent(
                        tasteStatistics = TasteStatistics(scope = scope),
                        onScopeChanged = {
                            changes += it
                            scope = it
                        }
                    )
                }
            }
        }
        val movies = composeRule.onNode(scopeSelector(context.getString(R.string.statistics_filter_movies)))
        val resting = movies.captureToImage()
        movies.performTouchInput { down(center) }
        composeRule.mainClock.advanceTimeBy(500)
        assertFeedbackVisible(resting, movies.captureToImage())
        movies.performTouchInput { cancel() }
        composeRule.waitForIdle()
        composeRule.runOnIdle { assertTrue(changes.isEmpty()) }
        composeRule.runOnIdle { assertTrue(inputModeManager.requestInputMode(InputMode.Keyboard)) }
        movies.performSemanticsAction(SemanticsActions.RequestFocus) { it() }.assertIsFocused()
        composeRule.mainClock.advanceTimeBy(500)
        assertFeedbackVisible(resting, movies.captureToImage())
        movies.performKeyInput {
            keyDown(Key.Enter)
            keyUp(Key.Enter)
        }.assertIsSelected()
        composeRule.runOnIdle { assertEquals(listOf(StatisticsMediaScope.MOVIES), changes) }
    }

    private fun assertFeedbackVisible(resting: ImageBitmap, interacting: ImageBitmap) {
        val before = resting.toPixelMap()
        val after = interacting.toPixelMap()
        assertEquals(before.width, after.width)
        assertEquals(before.height, after.height)
        val changed = (0 until before.height).sumOf { y ->
            (0 until before.width).count { x -> before[x, y] != after[x, y] }
        }
        assertTrue(
            "The filter must keep visible feedback while pressed or focused",
            changed > before.width * before.height / 3
        )
    }

    @Test
    fun yearsCanBeSelectedInPlaceIncludingYearsOutsideTheVisibleRange() {
        var selectedYear = 2026
        composeRule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(320.dp, 900.dp))) {
                DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(1.5f)) {
                    var year by remember { mutableStateOf(2026) }
                    BingeeTheme(darkTheme = false) {
                        Surface {
                            StatisticsContent(
                                statistics = WatchedStatistics(
                                    monthlyViewing = MonthlyViewingStatistics(
                                        currentYear = 2026,
                                        currentMonth = 8,
                                        selectedYear = year,
                                        availableYears = (1980..2026).toList().asReversed(),
                                        months = (1..12).map { MonthlyViewingData(year = year, month = it) }
                                    )
                                ),
                                tasteStatistics = TasteStatistics(),
                                onScopeChanged = {},
                                onYearChanged = {
                                    year = it
                                    selectedYear = it
                                }
                            )
                        }
                    }
                }
            }
        }

        val chooseYear = context.getString(R.string.statistics_viewing_choose_year)
        composeRule.scrollListTo(hasContentDescription(chooseYear))
        composeRule.onNodeWithText("2025").performClick().assertIsSelected()
        composeRule.runOnIdle { assertEquals(2025, selectedYear) }
        composeRule.onNodeWithContentDescription(chooseYear).performScrollToNode(hasText("1980"))
        composeRule.onNodeWithText("1980").performClick().assertIsSelected()
        composeRule.runOnIdle { assertEquals(1980, selectedYear) }
        composeRule.onAllNodes(isPopup()).assertCountEquals(0)
    }

    @Test
    fun radarMatchesExistingHeightPolicyInTheNativePhoneWindow() {
        val fontScale = mutableStateOf(1f)
        var expectedHeight = 260.dp
        var expectedWidth = 0.dp
        composeRule.setContent {
            val density = LocalDensity.current
            expectedWidth = with(density) { LocalWindowInfo.current.containerSize.width.toDp() } - 48.dp
            expectedHeight = (260.dp * fontScale.value).coerceAtMost(expectedWidth.coerceAtLeast(260.dp))
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale.value)) {
                BingeeTheme(darkTheme = false) {
                    Surface(color = MaterialTheme.colorScheme.background) {
                        StatisticsContent(
                            tasteStatistics = TasteStatistics(rankedGenres = radarGenres()),
                            onScopeChanged = {}
                        )
                    }
                }
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription(radarDescription())
            .assertWidthIsEqualTo(expectedWidth)
            .assertHeightIsEqualTo(expectedHeight)
        composeRule.runOnIdle { fontScale.value = 1.5f }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription(radarDescription())
            .assertWidthIsEqualTo(expectedWidth)
            .assertHeightIsEqualTo(expectedHeight)
    }

    @Test
    fun radarFollowsItsContainerInsideA600DpWindow() {
        val containerWidth = mutableStateOf(600.dp)
        val genres = mutableStateOf(radarGenres())
        composeRule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(600.dp, 900.dp))) {
                DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(1.5f)) {
                    BingeeTheme(darkTheme = true) {
                        Surface(color = MaterialTheme.colorScheme.background) {
                            Box {
                                StatisticsContent(
                                    tasteStatistics = TasteStatistics(rankedGenres = genres.value),
                                    onScopeChanged = {},
                                    modifier = Modifier.width(containerWidth.value)
                                )
                            }
                        }
                    }
                }
            }
        }

        composeRule.onNodeWithContentDescription(radarDescription())
            .assertWidthIsEqualTo(552.dp)
            .assertHeightIsEqualTo(390.dp)
        composeRule.runOnIdle { containerWidth.value = 320.dp }
        composeRule.onNodeWithContentDescription(radarDescription())
            .assertWidthIsEqualTo(272.dp)
            .assertHeightIsEqualTo(272.dp)
        composeRule.runOnIdle { genres.value = radarGenres().take(1) }
        composeRule.onNodeWithContentDescription(context.getString(R.string.statistics_radar_empty_description))
            .assertHeightIsEqualTo(272.dp)
            .assertIsDisplayed()
        composeRule.runOnIdle {
            genres.value = radarGenres()
            containerWidth.value = 600.dp
        }
        composeRule.onNodeWithContentDescription(radarDescription()).assertHeightIsEqualTo(390.dp)
    }

    @Test
    fun radarFitsPhoneContainersAtNormalAndLargeFonts() {
        val windowWidth = mutableStateOf(411.dp)
        val fontScale = mutableStateOf(1f)
        composeRule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.ForcedSize(DpSize(windowWidth.value, 900.dp))
            ) {
                DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(fontScale.value)) {
                    BingeeTheme(darkTheme = false) {
                        Surface(color = MaterialTheme.colorScheme.background) {
                            StatisticsContent(
                                tasteStatistics = TasteStatistics(rankedGenres = radarGenres()),
                                onScopeChanged = {}
                            )
                        }
                    }
                }
            }
        }

        val radar = composeRule.onNodeWithContentDescription(radarDescription())
        radar.assertHeightIsEqualTo(260.dp)
        composeRule.runOnIdle { fontScale.value = 1.5f }
        // ForcedSize can use fractional density: the cap follows the measured width, not nominal dp.
        radar.assertHeightIsEqualTo(radar.getUnclippedBoundsInRoot().run { right - left })
        composeRule.runOnIdle {
            windowWidth.value = 360.dp
            fontScale.value = 1.3f
        }
        radar.assertHeightIsEqualTo(radar.getUnclippedBoundsInRoot().run { right - left })
        composeRule.runOnIdle {
            windowWidth.value = 320.dp
            fontScale.value = 1.5f
        }
        radar.assertHeightIsEqualTo(radar.getUnclippedBoundsInRoot().run { right - left })
            .assertIsDisplayed()
    }

    private fun radarGenres() = listOf(
        GenreStatistic(MediaSource.TMDB, 18, "Drama", 18),
        GenreStatistic(MediaSource.TMDB, 35, "Comedy", 10),
        GenreStatistic(MediaSource.TMDB, 53, "Thriller", 8),
        GenreStatistic(MediaSource.TMDB, 878, "Science Fiction", 6),
        GenreStatistic(MediaSource.TMDB, 27, "Horror", 4),
        GenreStatistic(MediaSource.TMDB, 10749, "Romance", 2)
    )

    private fun radarDescription() = context.getString(
        R.string.statistics_radar_description,
        radarGenres().joinToString { it.name }
    )

    @Test
    fun tasteAndCompleteGenreRankingAreVisible() {
        val genres = listOf(
            GenreStatistic(MediaSource.TMDB, 18, "Drama", 18),
            GenreStatistic(MediaSource.TMDB, 35, "Comedy", 10),
            GenreStatistic(MediaSource.TMDB, 53, "Thriller", 8),
            GenreStatistic(MediaSource.TMDB, 878, "Science Fiction", 6),
            GenreStatistic(MediaSource.TMDB, 27, "Horror", 4),
            GenreStatistic(MediaSource.TMDB, 10749, "Romance", 2),
            GenreStatistic(MediaSource.TMDB, 80, "Crime", 1)
        )

        composeRule.setContent {
            BingeeTheme {
                StatisticsContent(
                    tasteStatistics = TasteStatistics(rankedGenres = genres),
                    onScopeChanged = {}
                )
            }
        }

        composeRule.onNodeWithText(context.getString(R.string.statistics_taste)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.statistics_genres)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Crime").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            context.resources.getQuantityString(R.plurals.statistics_genre_row_description, 18, "Drama", 18)
        ).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            context.getString(
                R.string.statistics_radar_description,
                "Drama, Comedy, Thriller, Science Fiction, Horror, Romance"
            )
        ).assertIsDisplayed()
    }

    @Test
    fun changingScopeUpdatesRadarAndCompleteRankingTogether() {
        val entries = listOf(
            viewing(
                "movie",
                MediaType.MOVIE,
                listOf(genre("Drama", 18), genre("Comedy", 35), genre("Thriller", 53))
            ),
            viewing(
                "series",
                MediaType.SERIES,
                listOf(genre("Horror", 27), genre("Crime", 80), genre("Romance", 10749))
            )
        )

        composeRule.setContent {
            var scope by remember { mutableStateOf(StatisticsMediaScope.ALL) }
            BingeeTheme {
                StatisticsContent(
                    tasteStatistics = calculateTasteStatistics(entries, scope),
                    onScopeChanged = { scope = it }
                )
            }
        }

        composeRule.scrollListTo(
            hasContentDescription(
                context.getString(
                    R.string.statistics_radar_description,
                    "Drama, Horror, Comedy, Thriller, Crime, Romance"
                )
            )
        ).assertIsDisplayed()

        composeRule.onNodeWithText(context.getString(R.string.statistics_filter_movies)).performClick()

        // Radar and complete ranking are driven by the same scoped ranking, so both drop the series
        // genres. The ranking row reports the title count, not the TMDB genre id.
        composeRule.scrollListTo(
            hasContentDescription(context.getString(R.string.statistics_radar_description, "Drama, Comedy, Thriller"))
        )
            .assertIsDisplayed()
        composeRule.scrollListTo(
            hasContentDescription(
                context.resources.getQuantityString(R.plurals.statistics_genre_row_description, 1, "Drama", 1)
            )
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Horror").assertDoesNotExist()
    }

    @Test
    fun insufficientDataKeepsRadarFrameAndShowsEmptyMessage() {
        composeRule.setContent {
            BingeeTheme {
                StatisticsContent(
                    tasteStatistics = TasteStatistics(
                        rankedGenres = listOf(GenreStatistic(MediaSource.TMDB, 18, "Drama", 1))
                    ),
                    onScopeChanged = {}
                )
            }
        }

        composeRule.onNodeWithText(context.getString(R.string.statistics_not_enough_data)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(context.getString(R.string.statistics_radar_empty_description))
            .assertIsDisplayed()
    }

    @Test
    fun viewingChartShowsTwelveMonthsAndSelectedMonthDetail() {
        val months = (1..12).map { month ->
            MonthlyViewingData(
                year = 2026,
                month = month,
                movieMinutes = if (month == 3) 120 else 0,
                seriesMinutes = if (month == 3) 60 else 0
            )
        }
        val statistics = WatchedStatistics(
            moviesWatchedCount = 1,
            tvSeriesCompletedCount = 1,
            episodesWatchedCount = 2,
            movieWatchTimeMinutes = 120,
            seriesWatchTimeMinutes = 60,
            monthlyViewing = MonthlyViewingStatistics(
                currentYear = 2026,
                currentMonth = 8,
                selectedYear = 2026,
                availableYears = listOf(2026, 2025),
                months = months
            )
        )

        composeRule.setContent {
            var selectedMonth by remember { mutableStateOf<Int?>(null) }
            BingeeTheme {
                StatisticsContent(
                    statistics = statistics,
                    tasteStatistics = TasteStatistics(),
                    onScopeChanged = {},
                    selectedMonth = selectedMonth,
                    onMonthSelected = { selectedMonth = it }
                )
            }
        }

        composeRule.onNodeWithText(context.getString(R.string.statistics_viewing)).assertIsDisplayed()
        composeRule.scrollListTo(hasText(context.getString(R.string.statistics_viewing_time))).assertIsDisplayed()
        // Bring the chart vertically into view before its row handles horizontal scrolling.
        composeRule.scrollListTo(
            hasContentDescription(context.getString(R.string.statistics_viewing_chart_description, 2026))
        )
        composeRule.onNodeWithContentDescription(
            context.getString(
                R.string.statistics_viewing_month_accessibility,
                context.resources.getStringArray(R.array.statistics_months_full)[2],
                2026,
                "2" + context.getString(R.string.statistics_duration_hour_short),
                "1" + context.getString(R.string.statistics_duration_hour_short),
                "3" + context.getString(R.string.statistics_duration_hour_short)
            )
        )
            .performScrollTo()
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        composeRule.onNodeWithContentDescription(
            context.getString(
                R.string.statistics_viewing_month_accessibility,
                context.resources.getStringArray(R.array.statistics_months_full)[2],
                2026,
                "2" + context.getString(R.string.statistics_duration_hour_short),
                "1" + context.getString(R.string.statistics_duration_hour_short),
                "3" + context.getString(R.string.statistics_duration_hour_short)
            )
        )
            .assertIsSelected()
        composeRule.onNodeWithContentDescription(
            context.getString(
                R.string.statistics_viewing_month_accessibility,
                context.resources.getStringArray(R.array.statistics_months_full)[8],
                2026,
                "0" + context.getString(R.string.statistics_duration_minute_short),
                "0" + context.getString(R.string.statistics_duration_minute_short),
                "0" + context.getString(R.string.statistics_duration_minute_short)
            )
        )
            .performScrollTo()
            .assertIsNotEnabled()
        // The detail renders below the chart, so the list has to scroll to it in its own right.
        composeRule.scrollListTo(
            hasText(
                context.getString(
                    R.string.statistics_viewing_month_title,
                    context.resources.getStringArray(R.array.statistics_months_full)[2],
                    2026
                )
            )
        ).assertIsDisplayed()
    }

    @Test
    fun compactChartsKeepMinimumTargetsAndReachEndItems() {
        val months = (1..12).map { month -> MonthlyViewingData(year = 2026, month = month) }
        val topRated = rated("top", MediaType.MOVIE, 10, LocalDate.of(2024, 3, 1))
        val statistics = WatchedStatistics(
            personalRatingStatistics = PersonalRatingStatistics(
                averageRating = 10.0,
                histogram = (1..10).map { rating ->
                    RatingHistogramBucket(rating, if (rating == 10) 1 else 0)
                },
                ratedTitles = listOf(topRated)
            ),
            monthlyViewing = MonthlyViewingStatistics(
                currentYear = 2026,
                currentMonth = 12,
                selectedYear = 2026,
                availableYears = listOf(2026),
                months = months
            )
        )

        composeRule.setContent {
            var selectedMonth by remember { mutableStateOf<Int?>(null) }
            BingeeTheme {
                Box(Modifier.width(320.dp).fillMaxHeight()) {
                    StatisticsContent(
                        statistics = statistics,
                        tasteStatistics = TasteStatistics(),
                        onScopeChanged = {},
                        selectedMonth = selectedMonth,
                        onMonthSelected = { selectedMonth = it }
                    )
                }
            }
        }

        composeRule.scrollListTo(hasText(context.getString(R.string.statistics_viewing_time)))
        composeRule.scrollListTo(
            hasContentDescription(context.getString(R.string.statistics_viewing_chart_description, 2026))
        )
        composeRule.onNodeWithContentDescription(
            context.getString(
                R.string.statistics_viewing_month_accessibility,
                context.resources.getStringArray(R.array.statistics_months_full)[0],
                2026,
                "0" + context.getString(R.string.statistics_duration_minute_short),
                "0" + context.getString(R.string.statistics_duration_minute_short),
                "0" + context.getString(R.string.statistics_duration_minute_short)
            )
        )
            .assertIsDisplayed()
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
        // December is the last slot behind the end padding; it must scroll fully into view and,
        // because the current month is December, still be selectable rather than future-disabled.
        composeRule.onNodeWithContentDescription(
            context.getString(
                R.string.statistics_viewing_month_accessibility,
                context.resources.getStringArray(R.array.statistics_months_full)[11],
                2026,
                "0" + context.getString(R.string.statistics_duration_minute_short),
                "0" + context.getString(R.string.statistics_duration_minute_short),
                "0" + context.getString(R.string.statistics_duration_minute_short)
            )
        )
            .performScrollTo()
            .assertIsDisplayed()
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        composeRule.onNodeWithContentDescription(
            context.getString(
                R.string.statistics_viewing_month_accessibility,
                context.resources.getStringArray(R.array.statistics_months_full)[11],
                2026,
                "0" + context.getString(R.string.statistics_duration_minute_short),
                "0" + context.getString(R.string.statistics_duration_minute_short),
                "0" + context.getString(R.string.statistics_duration_minute_short)
            )
        )
            .assertIsSelected()

        composeRule.scrollListTo(hasText(context.getString(R.string.statistics_ratings)))
        (1..9).forEach { rating ->
            composeRule.onNodeWithContentDescription(
                context.resources.getQuantityString(R.plurals.statistics_rating_bar_description, 0, rating, 0, "")
            ).assertIsNotEnabled()
        }
        composeRule.onNodeWithContentDescription(
            context.resources.getQuantityString(R.plurals.statistics_rating_bar_description, 0, 1, 0, "")
        )
            .assertIsDisplayed()
            .assertWidthIsAtLeast(200.dp)
            .assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithContentDescription(
            context.resources.getQuantityString(R.plurals.statistics_rating_bar_description, 1, 10, 1, "")
        )
            .performScrollTo()
            .assertIsDisplayed()
            .assertWidthIsAtLeast(200.dp)
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        composeRule.onNodeWithContentDescription(
            context.resources.getQuantityString(
                R.plurals.statistics_rating_bar_description,
                1,
                10,
                1,
                context.getString(R.string.statistics_rating_selected_suffix)
            )
        ).assertIsSelected()
    }

    @Test
    fun ratingsShowCombinedSummaryAndExpandAllMatchingShelves() {
        val entries = listOf(
            rated("movie", MediaType.MOVIE, 8, LocalDate.of(2024, 3, 1)),
            rated("series", MediaType.SERIES, 8, LocalDate.of(2022, 9, 1)),
            rated("other", MediaType.MOVIE, 9, LocalDate.of(2025, 1, 1))
        )
        val statistics = calculateWatchedStatistics(entries)

        composeRule.setContent {
            var scope by remember { mutableStateOf(StatisticsMediaScope.ALL) }
            BingeeTheme {
                StatisticsContent(
                    statistics = statistics,
                    tasteStatistics = calculateTasteStatistics(entries, scope),
                    onScopeChanged = { scope = it }
                )
            }
        }

        composeRule.scrollListTo(hasText(context.getString(R.string.statistics_ratings))).assertIsDisplayed()
        composeRule.scrollListTo(hasText(context.getString(R.string.statistics_rating_average))).assertIsDisplayed()
        composeRule.scrollListTo(hasText(context.getString(R.string.statistics_rating_count))).assertIsDisplayed()
        composeRule.onNodeWithText("movie").assertDoesNotExist()
        // "Movies" is also the viewing legend and a rating shelf title, so target the scope control.
        composeRule.scrollListTo(scopeSelector(context.getString(R.string.statistics_filter_movies))).performClick()

        composeRule.scrollListTo(
            hasContentDescription(
                context.resources.getQuantityString(R.plurals.statistics_rating_bar_description, 2, 8, 2, "")
            )
        ).performClick()
        composeRule.onNodeWithContentDescription(
            context.resources.getQuantityString(
                R.plurals.statistics_rating_bar_description,
                2,
                8,
                2,
                context.getString(R.string.statistics_rating_selected_suffix)
            )
        ).assertIsSelected()
        composeRule.scrollListToItem(STATISTICS_RATINGS_ITEM)
        composeRule.scrollListTo(
            hasText(context.getString(R.string.statistics_rating_movies)) and isHeading()
        ).assertIsDisplayed()
        composeRule.scrollListTo(hasText("movie")).assertIsDisplayed()
        composeRule.scrollListTo(
            hasText(context.getString(R.string.statistics_rating_series)) and isHeading()
        ).assertIsDisplayed()
        composeRule.scrollListTo(hasText("series")).assertIsDisplayed()
        composeRule.scrollListTo(
            hasText(context.getString(R.string.profile_media_type_movie, 2024))
        ).assertIsDisplayed()
        composeRule.scrollListTo(
            hasText(context.getString(R.string.profile_media_type_series, 2022))
        ).assertIsDisplayed()

        composeRule.scrollListTo(
            hasContentDescription(
                context.resources.getQuantityString(R.plurals.statistics_rating_bar_description, 1, 9, 1, "")
            )
        ).performClick()
        composeRule.onNodeWithContentDescription(
            context.resources.getQuantityString(
                R.plurals.statistics_rating_bar_description,
                1,
                9,
                1,
                context.getString(R.string.statistics_rating_selected_suffix)
            )
        ).assertIsSelected()
        composeRule.scrollListToItem(STATISTICS_RATINGS_ITEM)
        composeRule.scrollListTo(hasText("other")).assertIsDisplayed()
        composeRule.onNodeWithText("movie").assertDoesNotExist()
        composeRule.onNodeWithText("series").assertDoesNotExist()

        composeRule.scrollListTo(
            hasContentDescription(
                context.resources.getQuantityString(
                    R.plurals.statistics_rating_bar_description,
                    1,
                    9,
                    1,
                    context.getString(R.string.statistics_rating_selected_suffix)
                )
            )
        ).performClick()
        composeRule.onNodeWithText("other").assertDoesNotExist()
    }

    @Test
    fun ratingShelfKeepsEveryMatchingTitleBeyondAPreviewSizedRow() {
        // Poster rows elsewhere in the app show a preview slice; the rating shelf is the full result
        // set, so a selection of more than seven titles must keep the last one reachable.
        val entries = (1..9).map { index ->
            rated("title-$index", MediaType.MOVIE, 8, LocalDate.of(2024, 3, 1))
        }

        composeRule.setContent {
            BingeeTheme {
                StatisticsContent(
                    statistics = calculateWatchedStatistics(entries),
                    tasteStatistics = TasteStatistics(),
                    onScopeChanged = {}
                )
            }
        }

        composeRule.scrollListTo(
            hasContentDescription(
                context.resources.getQuantityString(R.plurals.statistics_rating_bar_description, 9, 8, 9, "")
            )
        ).performClick()
        composeRule.scrollListToItem(STATISTICS_RATINGS_ITEM)
        composeRule.scrollListTo(hasText("title-1")).assertIsDisplayed()
        composeRule.scrollRowTo(
            hasContentDescription(
                context.getString(
                    R.string.statistics_rating_poster_accessibility,
                    "title-9",
                    context.getString(R.string.profile_media_type_movie, 2024)
                )
            )
        ).assertIsDisplayed()
    }

    @Test
    fun ratingChartKeepsAllBucketsAndEmptyState() {
        composeRule.setContent {
            BingeeTheme {
                StatisticsContent(
                    tasteStatistics = TasteStatistics(),
                    onScopeChanged = {}
                )
            }
        }

        composeRule.scrollListTo(hasText(context.getString(R.string.statistics_ratings))).assertIsDisplayed()
        composeRule.scrollListTo(hasText(context.getString(R.string.statistics_rating_empty))).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            context.resources.getQuantityString(R.plurals.statistics_rating_bar_description, 0, 10, 0, "")
        ).assertExists()
        composeRule.onNodeWithContentDescription(
            context.resources.getQuantityString(R.plurals.statistics_rating_bar_description, 0, 1, 0, "")
        ).assertIsNotEnabled()
    }

    @Test
    fun ratingPosterUsesExistingDetailsCallback() {
        // Details navigation is TMDB-first and only a positive numeric TMDB id is navigable, so the
        // poster needs a real provider id even though the title stays "movie".
        val entry = rated("movie", MediaType.MOVIE, 8, LocalDate.of(2024, 3, 1))
            .copy(mediaRef = ExternalMediaRef(MediaSource.TMDB, "550"))
        var opened: Pair<ExternalMediaRef, MediaType>? = null

        composeRule.setContent {
            BingeeTheme {
                StatisticsContent(
                    statistics = calculateWatchedStatistics(listOf(entry)),
                    tasteStatistics = TasteStatistics(),
                    onScopeChanged = {},
                    onOpenDetails = { reference, mediaType -> opened = reference to mediaType }
                )
            }
        }

        composeRule.scrollListTo(
            hasContentDescription(
                context.resources.getQuantityString(R.plurals.statistics_rating_bar_description, 1, 8, 1, "")
            )
        ).performClick()
        composeRule.scrollListToItem(STATISTICS_RATINGS_ITEM)
        composeRule.scrollListTo(
            hasContentDescription(
                context.getString(
                    R.string.statistics_rating_poster_accessibility,
                    "movie",
                    context.getString(R.string.profile_media_type_movie, 2024)
                )
            )
        ).performClick()

        assertEquals(entry.mediaRef, opened?.first)
        assertEquals(MediaType.MOVIE, opened?.second)
    }

    @Test
    fun imdbOnlyRatingPosterIsNotClickable() {
        val entry = rated("legacy", MediaType.MOVIE, 8, LocalDate.of(2024, 3, 1)).copy(
            mediaRef = ExternalMediaRef(MediaSource.IMDB, "tt1234567")
        )

        composeRule.setContent {
            BingeeTheme {
                StatisticsContent(
                    statistics = calculateWatchedStatistics(listOf(entry)),
                    tasteStatistics = TasteStatistics(),
                    onScopeChanged = {}
                )
            }
        }

        composeRule.scrollListTo(
            hasContentDescription(
                context.resources.getQuantityString(R.plurals.statistics_rating_bar_description, 1, 8, 1, "")
            )
        ).performClick()
        composeRule.scrollListToItem(STATISTICS_RATINGS_ITEM)
        composeRule.scrollListTo(
            hasContentDescription(
                context.getString(
                    R.string.statistics_rating_poster_accessibility,
                    "legacy",
                    context.getString(R.string.profile_media_type_movie, 2024)
                )
            )
        ).assertIsNotEnabled()
    }

    /** The scope control is the only "All / Movies / Series" node carrying a selected state. */
    private fun scopeSelector(label: String) = hasText(label) and isSelectable()

    private fun genre(name: String, genreId: Long) = Genre(name, MediaSource.TMDB, genreId)

    private fun viewing(id: String, type: MediaType, genres: List<Genre>) = PersonalViewingEntry(
        mediaRef = ExternalMediaRef(MediaSource.TMDB, id),
        mediaType = type,
        title = id,
        addedAt = Instant.EPOCH,
        inLibrary = false,
        isFavorite = false,
        movieWatchedAt = if (type == MediaType.MOVIE) Instant.EPOCH else null,
        watchedRegularEpisodes = if (type == MediaType.SERIES) 1 else 0,
        genres = genres
    )

    private fun rated(id: String, type: MediaType, rating: Int, releaseDate: LocalDate) = PersonalViewingEntry(
        mediaRef = ExternalMediaRef(MediaSource.TMDB, id),
        mediaType = type,
        title = id,
        addedAt = Instant.EPOCH,
        inLibrary = false,
        isFavorite = false,
        personalRating = PersonalRating(rating),
        personalRatingUpdatedAt = Instant.parse("2026-08-01T00:00:00Z"),
        movieWatchedAt = if (type == MediaType.MOVIE) Instant.EPOCH else null,
        watchedRegularEpisodes = if (type == MediaType.SERIES) 1 else 0,
        releaseDate = releaseDate
    )
}
