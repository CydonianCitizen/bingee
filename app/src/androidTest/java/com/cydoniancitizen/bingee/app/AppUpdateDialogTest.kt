package com.cydoniancitizen.bingee.app

import android.graphics.Bitmap
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.cydoniancitizen.bingee.R
import com.cydoniancitizen.bingee.core.designsystem.theme.BingeeTheme
import com.cydoniancitizen.bingee.domain.repository.AppUpdateResult
import com.cydoniancitizen.bingee.testutil.TestLocaleRule
import com.cydoniancitizen.bingee.testutil.localizedTestContext
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AppUpdateDialogTest {
    @get:Rule(order = 0)
    val localeRule = TestLocaleRule()

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    private val context get() = localizedTestContext
    private val available = AppUpdateResult.UpdateAvailable(
        "1.3.0",
        "2.0.0",
        "https://github.com/CydonianCitizen/Bingee/releases/tag/v2.0.0"
    )

    @Test
    fun noUpdateLeavesContentAvailableWithoutPopup() {
        composeRule.setContent {
            BingeeTheme {
                Text("Local content")
                AppUpdateDialog(null, {}, {})
            }
        }
        composeRule.onNodeWithText("Local content").assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.update_available)).assertDoesNotExist()
        composeRule.onNodeWithText(context.getString(R.string.update_checking)).assertDoesNotExist()
        composeRule.onNodeWithText(context.getString(R.string.update_up_to_date)).assertDoesNotExist()
    }

    @Test
    fun newerVersionShowsLocalizedPopupAndLaterDismissesIt() {
        var update by mutableStateOf<AppUpdateResult.UpdateAvailable?>(available)
        composeRule.setContent {
            BingeeTheme {
                AppUpdateDialog(update, { update = null }, { error("Later must not open a browser") })
            }
        }
        composeRule.onNodeWithText(context.getString(R.string.update_available)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.update_available_message, "2.0.0")).assertIsDisplayed()
        capture("standard")
        composeRule.onNodeWithText(context.getString(R.string.update_later)).performClick()
        composeRule.onNodeWithText(context.getString(R.string.update_available)).assertDoesNotExist()
    }

    @Test
    fun viewReleaseDismissesPopupAndPassesOfficialUrlOnce() {
        var update by mutableStateOf<AppUpdateResult.UpdateAvailable?>(available)
        val opened = mutableListOf<String>()
        composeRule.setContent {
            BingeeTheme {
                AppUpdateDialog(update, { update = null }, opened::add)
            }
        }
        composeRule.onNodeWithText(context.getString(R.string.update_view_release)).performClick()
        composeRule.runOnIdle { assertEquals(listOf(available.releaseUrl), opened) }
        composeRule.onNodeWithText(context.getString(R.string.update_available)).assertDoesNotExist()
    }

    @Test
    fun popupActionsRemainReachableWithLargeTextAndDarkTheme() {
        composeRule.setContent {
            BingeeTheme(darkTheme = true, fontScaleMultiplier = 1.5f) {
                AppUpdateDialog(available, {}, {})
            }
        }
        composeRule.onNodeWithText(context.getString(R.string.update_available_message, "2.0.0")).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.update_view_release)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.update_later)).assertIsDisplayed()
        capture("large-dark")
    }

    private fun capture(name: String) {
        if (InstrumentationRegistry.getArguments().getString("captureUpdateDialog") != "true") return
        val locale = context.resources.configuration.locales[0].language
        File(context.filesDir, "update-$locale-$name.png").outputStream().use {
            composeRule.onNode(isDialog()).captureToImage().asAndroidBitmap()
                .compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
