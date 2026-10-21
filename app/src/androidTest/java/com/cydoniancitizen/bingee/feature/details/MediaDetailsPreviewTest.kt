package com.cydoniancitizen.bingee.feature.details

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performScrollToKey
import com.cydoniancitizen.bingee.R
import com.cydoniancitizen.bingee.core.designsystem.theme.BingeeTheme
import com.cydoniancitizen.bingee.testutil.TestLocaleRule
import com.cydoniancitizen.bingee.testutil.localizedTestContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class MediaDetailsPreviewTest(private val caseIndex: Int) {
    @get:Rule(order = 0)
    val localeRule = TestLocaleRule()

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    @Test
    fun personalActionsPrecedeSynopsisWithOneCollapsedRatingEditor() {
        val state = DetailPersonalPreviewProvider().values.elementAt(caseIndex)
        composeRule.setContent {
            BingeeTheme {
                MediaDetailsContent(
                    state,
                    onBack = {},
                    onRefresh = {},
                    onRetry = {},
                    onToggleLibrary = {},
                    onDismissLibraryError = {},
                    onOpenSettings = {}
                )
            }
        }
        val list = composeRule.onNode(hasScrollAction())
        val indexForKey = list.fetchSemanticsNode().config[SemanticsProperties.IndexForKey]
        assertEquals(0, indexForKey("hero"))
        assertTrue(indexForKey("personal") > indexForKey("hero"))
        assertTrue(indexForKey("overview") > indexForKey("personal"))
        assertTrue(indexForKey("chips") > indexForKey("overview"))
        list.performScrollToKey("personal")
        val context = localizedTestContext
        composeRule.onAllNodesWithText(context.getString(R.string.detail_rating_title)).assertCountEquals(1)
        composeRule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
            .assertCountEquals(0)
        composeRule.onAllNodesWithText(context.getString(R.string.detail_rating_set)).assertCountEquals(0)
        composeRule.onAllNodesWithText(
            context.getString(
                if (state.isInLibrary ==
                    true
                ) {
                    R.string.library_action_remove
                } else {
                    R.string.library_action_add
                }
            )
        ).assertCountEquals(1)
        val relationship = when (caseIndex) {
            2, 6 -> R.string.detail_tracking_watching
            3 -> R.string.detail_tracking_caught_up
            4 -> R.string.detail_tracking_abandoned
            5 -> R.string.detail_tracking_outside
            else -> null
        }
        relationship?.let { composeRule.onAllNodesWithText(context.getString(it)).assertCountEquals(1) }
        composeRule.onAllNodesWithContentDescription(context.getString(R.string.favorite_add)).assertCountEquals(1)
        composeRule.onAllNodesWithText(context.getString(R.string.detail_go_to_episode))
            .assertCountEquals(if (caseIndex == 2) 1 else 0)
        if (caseIndex >= 2) {
            composeRule.onAllNodesWithText(context.getString(R.string.series_tracking_restore))
                .assertCountEquals(if (state.isAbandoned) 1 else 0)
            composeRule.onAllNodesWithText(context.getString(R.string.series_tracking_abandon))
                .assertCountEquals(if (state.isInLibrary == true && !state.isAbandoned) 1 else 0)
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "personal state {0}")
        fun cases() = (0..6).map { arrayOf(it) }
    }
}
