package com.cydoniancitizen.bingee.data.settings

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FontSizePreferencesTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val key = intPreferencesKey("font_scale_percent")
    private val unrelated = booleanPreferencesKey("font_size_test_marker")
    private val repository = DataStoreFontSizePreferences(context)

    @After
    fun removeTestPreferences(): Unit = runBlocking {
        context.bingeePreferences.edit {
            it.remove(key)
            it.remove(unrelated)
        }
    }

    @Test
    fun missingValueDefaultsToSystemAndSavedSizeSurvivesANewRepository() = runBlocking {
        context.bingeePreferences.edit {
            it.remove(key)
            it[unrelated] = true
        }
        assertEquals(100, repository.observeScalePercent().first())
        for (percent in listOf(150, 80, 100)) {
            repository.setScalePercent(percent)
            assertEquals(percent, DataStoreFontSizePreferences(context).observeScalePercent().first())
            assertEquals(percent, context.bingeePreferences.data.first()[key])
            assertEquals(true, context.bingeePreferences.data.first()[unrelated])
        }
    }

    @Test
    fun invalidStoredSizeFallsBackAndInvalidWritesKeepLastSavedValue() = runBlocking {
        context.bingeePreferences.edit { it[key] = 0 }
        assertEquals(100, repository.observeScalePercent().first())
        repository.setScalePercent(120)
        for (invalid in listOf(79, 151)) {
            val result = runCatching { repository.setScalePercent(invalid) }
            assertTrue(result.exceptionOrNull() is IllegalArgumentException)
            assertEquals(120, repository.observeScalePercent().first())
        }
    }
}
