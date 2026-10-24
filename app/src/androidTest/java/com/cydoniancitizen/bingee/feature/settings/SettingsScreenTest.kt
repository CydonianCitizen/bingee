package com.cydoniancitizen.bingee.feature.settings

import android.graphics.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import androidx.test.platform.app.InstrumentationRegistry
import com.cydoniancitizen.bingee.R
import com.cydoniancitizen.bingee.core.designsystem.theme.BingeeTheme
import com.cydoniancitizen.bingee.data.settings.AppLanguage
import com.cydoniancitizen.bingee.data.settings.AppTheme
import com.cydoniancitizen.bingee.testutil.TestLocaleRule
import com.cydoniancitizen.bingee.testutil.localizedTestContext
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {
    @get:Rule(order = 0)
    val localeRule = TestLocaleRule()

    @get:Rule(order = 1)
    val composeRule = createComposeRule()
    private val context = localizedTestContext

    @Test
    fun fontSizeSliderIsAvailableInAppearanceSettings() {
        var percent by mutableStateOf(100)
        val writes = mutableListOf<Int>()
        composeRule.setContent {
            BingeeTheme(darkTheme = true, fontScaleMultiplier = percent / 100f) {
                androidx.compose.material3.Surface {
                    AppearanceLanguageSettingsContent(
                        theme = AppTheme.SYSTEM_DEFAULT,
                        language = AppLanguage.ENGLISH,
                        onSetTheme = {},
                        onSetLanguage = {},
                        onBack = {},
                        fontScalePercent = percent,
                        onSetFontScalePercent = {
                            writes += it
                            percent = it
                        }
                    )
                }
            }
        }
        val slider = composeRule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
        slider.performScrollTo().assertIsDisplayed()
        listOf(150, 80, 100).forEach { value ->
            slider.performSemanticsAction(SemanticsActions.SetProgress) { it(value.toFloat()) }
            assertEquals(value, percent)
            composeRule.onNodeWithText(context.getString(R.string.settings_font_size_percent, value))
                .performScrollTo().assertIsDisplayed()
            val range = slider.fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo]
            assertEquals(value.toFloat(), range.current, 0f)
            assertEquals(80f..150f, range.range)
            val title = context.getString(R.string.settings_font_size_title)
            composeRule.onNodeWithText(title).performScrollTo()
            listOf(title, context.getString(R.string.settings_font_size_percent, value)).forEach { label ->
                val layouts = mutableListOf<TextLayoutResult>()
                composeRule.onNodeWithText(label, useUnmergedTree = true)
                    .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
                val layout = layouts.single()
                val lineWidth = (0 until layout.lineCount).maxOf { layout.getLineRight(it) - layout.getLineLeft(it) }
                // Paragraph reconstruction can round a line up by a fraction of a pixel.
                assertTrue(
                    "$label at $value%: size=${layout.size}, lineWidth=$lineWidth, height=${layout.didOverflowHeight}",
                    !layout.didOverflowHeight && lineWidth <= layout.size.width + 1f
                )
            }
            if (value == 150 && InstrumentationRegistry.getArguments().getString("captureFontSize") == "true") {
                File(InstrumentationRegistry.getInstrumentation().targetContext.filesDir, "font-size-slider-150.png")
                    .outputStream().use {
                        composeRule.onRoot().captureToImage().asAndroidBitmap().compress(
                            Bitmap.CompressFormat.PNG,
                            100,
                            it
                        )
                    }
            }
        }
        assertEquals(listOf(150, 80, 100), writes)
    }

    @Test
    fun fontSizeDragSavesOnlyOnRelease() {
        val writes = mutableListOf<Int>()
        composeRule.setContent {
            BingeeTheme {
                AppearanceLanguageSettingsContent(
                    AppTheme.SYSTEM_DEFAULT,
                    AppLanguage.ENGLISH,
                    {},
                    {},
                    {},
                    onSetFontScalePercent = { writes += it }
                )
            }
        }
        val slider = composeRule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
        slider.performScrollTo().performTouchInput {
            down(center)
            moveTo(centerRight)
        }
        assertTrue(writes.isEmpty())
        slider.performTouchInput { up() }
        assertEquals(listOf(150), writes)
        // A callback that does not emit a stored value represents a failed save: discard the draft.
        composeRule.onNodeWithText(context.getString(R.string.settings_font_size_percent, 100)).assertIsDisplayed()
    }

    @Test
    fun fontSizeKeepsSystemScalingAndDoesNotChangeDisplayDensity() {
        val systemDensity = Density(2f, 1.3f)
        var multiplier by mutableStateOf(1f)
        var actual: Density = systemDensity
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides systemDensity) {
                BingeeTheme(fontScaleMultiplier = multiplier) {
                    actual = LocalDensity.current
                    androidx.compose.material3.Text("Font sample", fontSize = 20.sp)
                }
            }
        }
        composeRule.runOnIdle { assertSame(systemDensity, actual) }
        val normal = with(actual) { 20.sp.toPx() }
        composeRule.runOnIdle { multiplier = 1.5f }
        composeRule.runOnIdle {
            assertEquals(2f, actual.density, 0f)
            assertEquals(1.95f, actual.fontScale, 0.001f)
            assertTrue(with(actual) { 20.sp.toPx() } > normal)
        }
        composeRule.runOnIdle { multiplier = 0.8f }
        composeRule.runOnIdle {
            assertEquals(1.04f, actual.fontScale, 0.001f)
            assertEquals(2f, actual.density, 0f)
        }
    }

    @Test
    fun languageDropdownShowsOnlyEnglishAndItalianoAndNoSystemDefault() {
        var selectedLanguage by mutableStateOf(AppLanguage.ENGLISH)
        composeRule.setContent {
            BingeeTheme {
                AppearanceLanguageSettingsContent(
                    theme = AppTheme.SYSTEM_DEFAULT,
                    language = selectedLanguage,
                    onSetTheme = {},
                    onSetLanguage = { selectedLanguage = it },
                    onBack = {}
                )
            }
        }

        composeRule.onNodeWithText(context.getString(R.string.settings_appearance_title)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.settings_language_title)).assertIsDisplayed()
        composeRule.onNodeWithText(
            context.getString(R.string.settings_language_en)
        ).performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithText(context.getString(R.string.settings_language_en)).performClick()
        composeRule.onNodeWithText(context.getString(R.string.settings_language_it)).assertIsDisplayed()
        composeRule.onNode(
            hasText(context.getString(R.string.settings_theme_system)) and
                hasAnyAncestor(hasText(context.getString(R.string.settings_language_title)))
        ).assertDoesNotExist()

        composeRule.onNodeWithText(context.getString(R.string.settings_language_it)).performClick()
        assertEquals(AppLanguage.ITALIAN, selectedLanguage)
    }

    @Test
    fun themeDropdownShowsSystemDefaultLightAndDarkOptions() {
        var selectedTheme by mutableStateOf(AppTheme.SYSTEM_DEFAULT)
        composeRule.setContent {
            BingeeTheme {
                AppearanceLanguageSettingsContent(
                    theme = selectedTheme,
                    language = AppLanguage.ENGLISH,
                    onSetTheme = { selectedTheme = it },
                    onSetLanguage = {},
                    onBack = {}
                )
            }
        }

        composeRule.onNodeWithText(context.getString(R.string.settings_appearance_title)).assertIsDisplayed()
        composeRule.onNodeWithText(
            context.getString(R.string.settings_theme_title)
        ).performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithText(context.getString(R.string.settings_theme_system)).performClick()
        composeRule.onNodeWithText(context.getString(R.string.settings_theme_light)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.settings_theme_dark)).assertIsDisplayed()

        composeRule.onNodeWithText(context.getString(R.string.settings_theme_dark)).performClick()
        assertEquals(AppTheme.DARK, selectedTheme)
    }
}
