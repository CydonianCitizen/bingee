package com.cydoniancitizen.bingee.feature.settings

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.cydoniancitizen.bingee.MainActivity
import com.cydoniancitizen.bingee.R
import com.cydoniancitizen.bingee.data.settings.DataStoreFontSizePreferences
import com.cydoniancitizen.bingee.data.settings.bingeePreferences
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FontSizeActivityTest {
    @get:Rule val compose = createComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val preferences get() = DataStoreFontSizePreferences(context)

    @Test
    fun sliderPersistsAcrossActivityRecreationAndRelaunchAndChangesHomeText() {
        runBlocking {
            context.bingeePreferences.edit { it[booleanPreferencesKey("onboarding_complete")] = true }
            preferences.setScalePercent(100)
        }
        try {
            var normalFontScale = 0f
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                val home = context.getString(R.string.home_title)
                compose.waitUntil(10000) { compose.onNodeWithText(home).isDisplayed() }
                normalFontScale = textFontScale(home)
                compose.onNodeWithText(context.getString(R.string.profile_title_dashboard)).performClick()
                compose.onNodeWithContentDescription(context.getString(R.string.nav_settings)).performClick()
                compose.onNodeWithText(context.getString(R.string.settings_nav_appearance)).performClick()
                val title = context.getString(R.string.settings_font_size_title)
                compose.onNodeWithContentDescription(title).performScrollTo()
                    .performSemanticsAction(SemanticsActions.SetProgress) { it(150f) }
                compose.waitUntil(5000) {
                    compose.onNodeWithText(context.getString(R.string.settings_font_size_percent, 150)).isDisplayed()
                }
                assertEquals(150, runBlocking { preferences.observeScalePercent().first() })
                scenario.recreate()
                compose.waitUntil(10000) {
                    compose.onNodeWithText(context.getString(R.string.settings_appearance_title)).isDisplayed()
                }
                compose.onNodeWithContentDescription(title).performScrollTo().assertIsDisplayed()
                compose.onNodeWithText(context.getString(R.string.settings_font_size_percent, 150)).assertIsDisplayed()
                capture("appearance-150")
            }
            ActivityScenario.launch(MainActivity::class.java).use {
                val home = context.getString(R.string.home_title)
                compose.waitUntil(10000) { compose.onNodeWithText(home).isDisplayed() }
                compose.waitUntil(5000) { textFontScale(home) > normalFontScale }
                assertEquals(normalFontScale * 1.5f, textFontScale(home), 0.001f)
                capture("home-150")
            }
        } finally {
            runBlocking { preferences.setScalePercent(100) }
        }
    }

    private fun textFontScale(text: String): Float {
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(text, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue(layouts.isNotEmpty())
        return layouts.single().layoutInput.density.fontScale
    }

    private fun capture(name: String) {
        if (InstrumentationRegistry.getArguments().getString("captureFontSize") != "true") return
        File(context.filesDir, "font-size-$name.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
