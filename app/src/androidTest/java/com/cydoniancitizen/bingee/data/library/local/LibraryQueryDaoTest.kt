package com.cydoniancitizen.bingee.data.library.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cydoniancitizen.bingee.core.model.MediaType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibraryQueryDaoTest {
    private lateinit var database: BingeeDatabase

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, BingeeDatabase::class.java).build()
        insert(1, "MOVIE", "Amélie", "Le Fabuleux Destin", active = true)
        insert(2, "SERIES", "100% Real_Story", "Director's Cut", active = true)
        insert(3, "MOVIE", "Retained inactive title", null, active = false)
    }

    @After
    fun closeDatabase() = database.close()

    @Test
    fun membershipAndFavoritesStayVisibleButInactiveCachedTitlesDoNot() = runBlocking {
        insert(4, "MOVIE", "Favorite outside collection", null, active = false)
        database.openHelper.writableDatabase.execSQL(
            "UPDATE media_entries SET is_favorite = 1 WHERE local_media_id = 4"
        )
        val rows = database.libraryDao().observeLibraryItems().first()
        assertEquals(
            listOf("100% Real_Story", "Amélie", "Favorite outside collection"),
            rows.map { it.media.title }
        )
    }

    @Test
    fun mediaTypeRestrictionKeepsOnlyRequestedActiveTitles() = runBlocking {
        val dao = database.libraryDao()
        assertEquals(
            listOf("100% Real_Story"),
            dao.observeLibraryItems(MediaType.SERIES).first().map {
                it.media.title
            }
        )
        assertEquals(listOf("Amélie"), dao.observeLibraryItems(MediaType.MOVIE).first().map { it.media.title })
    }

    private fun insert(id: Long, type: String, title: String, original: String?, active: Boolean) {
        val escapedTitle = title.replace("'", "''")
        val originalValue = original?.replace("'", "''")?.let { "'$it'" } ?: "NULL"
        val db = database.openHelper.writableDatabase
        db.execSQL(
            "INSERT INTO media_entries(local_media_id, media_type, title, original_title, overview, poster_url, " +
                "release_date, created_at, metadata_updated_at, is_favorite) VALUES" +
                "($id, '$type', '$escapedTitle', $originalValue, NULL, NULL, NULL, " +
                "'2026-08-03T09:00:00Z', '2026-08-03T09:00:00Z', 0)"
        )
        db.execSQL(
            "INSERT INTO external_refs(local_media_id, source, media_type, external_id) " +
                "SELECT $id, 'TMDB', media_type, '$id' FROM media_entries WHERE local_media_id = $id"
        )
        if (active) {
            db.execSQL("INSERT INTO library_entries(local_media_id, added_at) VALUES($id, '2026-08-03T09:00:00Z')")
        }
    }
}
