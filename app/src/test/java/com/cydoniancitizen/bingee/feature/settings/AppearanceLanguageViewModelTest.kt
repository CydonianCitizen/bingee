package com.cydoniancitizen.bingee.feature.settings

import com.cydoniancitizen.bingee.core.result.AppError
import com.cydoniancitizen.bingee.data.settings.AppLanguage
import com.cydoniancitizen.bingee.data.settings.AppTheme
import com.cydoniancitizen.bingee.data.settings.AppearancePreferences
import com.cydoniancitizen.bingee.data.settings.FontSizePreferences
import com.cydoniancitizen.bingee.data.settings.SpoilerPreferences
import com.cydoniancitizen.bingee.data.settings.toTmdbLanguageTag
import com.cydoniancitizen.bingee.testutil.MainDispatcherRule
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppearanceLanguageViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun loadsAndSavesLanguageAndTheme() = runTest(mainDispatcherRule.dispatcher) {
        val preferences = FakeAppearancePreferences(AppTheme.DARK, AppLanguage.ITALIAN)
        val spoilers = FakeSpoilerPreferences()
        val viewModel = AppearanceLanguageViewModel(preferences, spoilers, FakeFontSizePreferences())
        advanceUntilIdle()

        assertEquals(AppTheme.DARK, viewModel.uiState.value.theme)
        assertEquals(AppLanguage.ITALIAN, viewModel.uiState.value.language)

        viewModel.setTheme(AppTheme.LIGHT)
        viewModel.setLanguage(AppLanguage.ENGLISH)
        advanceUntilIdle()

        assertEquals(AppTheme.LIGHT, preferences.theme.value)
        assertEquals(AppLanguage.ENGLISH, preferences.language.value)
        assertTrue(preferences.themeWrites.contains(AppTheme.LIGHT))
        assertTrue(preferences.languageWrites.contains(AppLanguage.ENGLISH))

        assertEquals(false, viewModel.uiState.value.hideSpoilers)
        viewModel.setHideSpoilers(true)
        advanceUntilIdle()
        assertEquals(true, viewModel.uiState.value.hideSpoilers)
    }

    @Test
    fun failedWritesKeepStoredPreferencesAndReportUntilARetrySucceeds() = runTest(mainDispatcherRule.dispatcher) {
        val failure = IOException("synthetic write failure")
        val preferences = FakeAppearancePreferences(AppTheme.DARK, AppLanguage.ITALIAN).apply { this.failure = failure }
        val spoilers = FakeSpoilerPreferences().apply { this.failure = failure }
        val viewModel = AppearanceLanguageViewModel(preferences, spoilers, FakeFontSizePreferences())
        advanceUntilIdle()
        val stored = viewModel.uiState.value

        listOf(
            { viewModel.setTheme(AppTheme.LIGHT) },
            { viewModel.setLanguage(AppLanguage.ENGLISH) },
            { viewModel.setHideSpoilers(true) }
        ).forEach { write ->
            write()
            advanceUntilIdle()
            assertEquals(stored.copy(error = AppError.LocalStorageFailure), viewModel.uiState.value)
            viewModel.clearError()
            assertEquals(stored, viewModel.uiState.value)
        }

        preferences.failure = null
        spoilers.failure = null
        viewModel.setTheme(AppTheme.LIGHT)
        viewModel.setLanguage(AppLanguage.ENGLISH)
        viewModel.setHideSpoilers(true)
        advanceUntilIdle()
        assertEquals(
            AppearanceLanguageUiState(AppTheme.LIGHT, AppLanguage.ENGLISH, hideSpoilers = true),
            viewModel.uiState.value
        )
    }

    @Test
    fun cancelledWriteIsNotReportedAsAStorageFailure() = runTest(mainDispatcherRule.dispatcher) {
        val preferences = FakeAppearancePreferences(AppTheme.DARK, AppLanguage.ITALIAN).apply {
            failure = CancellationException("left the screen")
        }
        val viewModel = AppearanceLanguageViewModel(preferences, FakeSpoilerPreferences(), FakeFontSizePreferences())
        advanceUntilIdle()

        viewModel.setTheme(AppTheme.LIGHT)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.error)
        assertEquals(AppTheme.DARK, viewModel.uiState.value.theme)
    }

    @Test
    fun legacySystemLanguageMapsToEnglishWithoutWriteSideEffect() {
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromPreferenceValue("SYSTEM"))
    }

    @Test
    fun fontSizeEmissionsAndWriteFailureKeepTheStoredValueUntilRetry() = runTest(mainDispatcherRule.dispatcher) {
        val fonts = FakeFontSizePreferences()
        val viewModel = AppearanceLanguageViewModel(
            FakeAppearancePreferences(AppTheme.DARK, AppLanguage.ITALIAN),
            FakeSpoilerPreferences(),
            fonts
        )
        advanceUntilIdle()
        assertEquals(100, viewModel.uiState.value.fontScalePercent)
        fonts.percent.value = 120
        advanceUntilIdle()
        assertEquals(120, viewModel.uiState.value.fontScalePercent)
        fonts.failure = IOException("synthetic write failure")
        viewModel.setFontScalePercent(150)
        advanceUntilIdle()
        assertEquals(120, viewModel.uiState.value.fontScalePercent)
        assertEquals(AppError.LocalStorageFailure, viewModel.uiState.value.error)
        fonts.failure = null
        viewModel.setFontScalePercent(80)
        advanceUntilIdle()
        assertEquals(80, viewModel.uiState.value.fontScalePercent)
        assertNull(viewModel.uiState.value.error)
        assertEquals(AppTheme.DARK, viewModel.uiState.value.theme)
        assertEquals(AppLanguage.ITALIAN, viewModel.uiState.value.language)
    }

    private class FakeFontSizePreferences : FontSizePreferences {
        val percent = MutableStateFlow(100)
        var failure: Exception? = null
        override fun observeScalePercent(): Flow<Int> = percent
        override suspend fun setScalePercent(percent: Int) {
            failure?.let { throw it }
            this.percent.value = percent
        }
    }

    private class FakeSpoilerPreferences : SpoilerPreferences {
        private val hide = MutableStateFlow(false)
        var failure: Exception? = null
        override fun observeHideSpoilers(): Flow<Boolean> = hide
        override suspend fun setHideSpoilers(hide: Boolean) {
            failure?.let { throw it }
            this.hide.value = hide
        }
    }

    private class FakeAppearancePreferences(theme: AppTheme, language: AppLanguage) : AppearancePreferences {
        val theme = MutableStateFlow(theme)
        val language = MutableStateFlow(language)
        val themeWrites = mutableListOf<AppTheme>()
        val languageWrites = mutableListOf<AppLanguage>()
        var failure: Exception? = null

        override fun observeTheme(): Flow<AppTheme> = theme

        override suspend fun setTheme(theme: AppTheme) {
            failure?.let { throw it }
            themeWrites += theme
            this.theme.value = theme
        }

        override fun observeLanguage(): Flow<AppLanguage> = language

        override suspend fun setLanguage(language: AppLanguage) {
            failure?.let { throw it }
            languageWrites += language
            this.language.value = language
        }

        override suspend fun getEffectiveTmdbLanguage(): String = language.value.toTmdbLanguageTag()
    }
}
