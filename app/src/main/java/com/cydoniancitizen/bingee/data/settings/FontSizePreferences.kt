package com.cydoniancitizen.bingee.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface FontSizePreferences {
    fun observeScalePercent(): Flow<Int>
    suspend fun setScalePercent(percent: Int)

    companion object {
        const val DEFAULT_PERCENT = 100
        val RANGE = 80..150
    }
}

/** Text size is relative to system accessibility settings and stays local to this device. */
@Singleton
internal class DataStoreFontSizePreferences @Inject constructor(
    @param:ApplicationContext private val context: Context
) : FontSizePreferences {
    override fun observeScalePercent(): Flow<Int> = context.bingeePreferenceData.map { preferences ->
        preferences[FONT_SCALE_PERCENT]?.takeIf { it in FontSizePreferences.RANGE }
            ?: FontSizePreferences.DEFAULT_PERCENT
    }

    override suspend fun setScalePercent(percent: Int) {
        require(percent in FontSizePreferences.RANGE)
        context.bingeePreferences.edit { it[FONT_SCALE_PERCENT] = percent }
    }

    private companion object {
        val FONT_SCALE_PERCENT = intPreferencesKey("font_scale_percent")
    }
}
