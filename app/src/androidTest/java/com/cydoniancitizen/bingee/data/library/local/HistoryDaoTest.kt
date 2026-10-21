package com.cydoniancitizen.bingee.data.library.local

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cydoniancitizen.bingee.core.model.Episode
import com.cydoniancitizen.bingee.core.model.HistoryContext
import com.cydoniancitizen.bingee.core.model.HistoryCursor
import com.cydoniancitizen.bingee.core.model.HistoryEpisodeQuery
import com.cydoniancitizen.bingee.core.model.HistoryFilter
import com.cydoniancitizen.bingee.core.model.HistoryPage
import com.cydoniancitizen.bingee.core.model.HistoryPageQuery
import com.cydoniancitizen.bingee.core.model.HistorySnapshotQuery
import com.cydoniancitizen.bingee.core.model.Season
import com.cydoniancitizen.bingee.core.result.AppResult
import com.cydoniancitizen.bingee.data.library.DefaultLibraryRepository
import com.cydoniancitizen.bingee.data.library.HistoryQueries
import com.cydoniancitizen.bingee.domain.model.calculateWatchedStatistics
import com.cydoniancitizen.bingee.testutil.TestCalendarDateSource
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HistoryDaoTest {
    private lateinit var db: BingeeDatabase
    private val context = HistoryContext(LocalDate.of(2026, 10, 20), ZoneId.of("UTC"))
    private val month = YearMonth.of(2026, 10)

    @Before fun setup() {
        db =
            Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext<Context>(),
                BingeeDatabase::class.java
            ).build()
    }

    @After fun close() = db.close()

    @Test fun groupsIncludeSpecialsAndOutsideCollectionWithoutDuplicateRefsOrCompletion() = runBlocking {
        media(1, "MOVIE")
        media(2, "SERIES")
        sql("INSERT INTO external_refs VALUES (1,'IMDB','MOVIE','tt1')")
        sql("INSERT INTO movie_watch_progress VALUES (1,'2026-11-01T00:00:00Z','2026-10-02')")
        season(1, 2, 0)
        season(2, 2, 1)
        episode(1, 1, 1)
        episode(2, 2, 1)
        sql("INSERT INTO series_watch_progress VALUES (2,NULL,'2026-10-02T00:00:00Z')")
        val rows = db.historyDao().readRows(HistoryQueries.page(HistoryPageQuery(month, context)))
        assertEquals(2, rows.size)
        assertEquals(listOf(0, 1), rows.map { it.kind })
        assertEquals(2, rows.last().episodeCount)
        val children = db.historyDao().readEpisodes(
            HistoryQueries.episodes(HistoryEpisodeQuery(context, LocalDate.of(2026, 10, 2), 2))
        )
        assertEquals(listOf(0, 1), children.map { it.seasonNumber })
        sql("DELETE FROM episode_watch_progress WHERE local_episode_id=1")
        assertEquals(
            1,
            db.historyDao().readRows(
                HistoryQueries.page(HistoryPageQuery(month, context))
            ).last().episodeCount
        )
    }

    @Test fun datesAdvanceChronologicallyTowardTheRight() = runBlocking {
        media(1, "MOVIE")
        media(2, "MOVIE")
        sql("INSERT INTO movie_watch_progress VALUES (1,'2026-10-02T00:00:00Z',NULL)")
        sql("INSERT INTO movie_watch_progress VALUES (2,'2026-10-03T00:00:00Z',NULL)")
        val rows = db.historyDao().readRows(HistoryQueries.page(HistoryPageQuery(month, context)))
        assertEquals(listOf(1L, 2L), rows.map { it.mediaId })
    }

    @Test fun continuousWindowsCrossSparseYearsInBothDirectionsWithoutDuplicates() = runBlocking {
        (1..155).forEach { id ->
            media(id, "MOVIE")
            val date = if (id <= 75) "2016-02-01" else "2026-10-02"
            sql("INSERT INTO movie_watch_progress VALUES ($id,'${date}T00:00:00Z',NULL)")
        }
        val repository = repository()
        suspend fun read(cursor: HistoryCursor? = null, backwards: Boolean = false): HistoryPage = (
            repository.observeHistory(
                HistorySnapshotQuery(HistoryPageQuery(null, context, cursor = cursor, backwards = backwards))
            )
                .first() as AppResult.Success
            ).value.page
        val first = read()
        assertEquals((1L..90L).toList(), first.items.map { it.mediaId })
        assertEquals(LocalDate.of(2016, 2, 1), first.items.first().day)
        assertFalse(first.hasPrevious)
        assertTrue(first.hasNext)
        val second = read(first.items[29].cursor)
        assertEquals((31L..120L).toList(), second.items.map { it.mediaId })
        val previous = read(second.items[60].cursor, backwards = true)
        assertEquals(first.items, previous.items)
        assertFalse(previous.hasPrevious)
        val visited = mutableListOf<Long>()
        var page = first
        while (true) {
            assertTrue(page.items.size <= 90)
            visited += page.items.map { it.mediaId }
            if (!page.hasNext) break
            page = read(page.items.last().cursor)
        }
        assertEquals((1L..155L).toList(), visited)
    }

    @Test fun firstPointUsesRetainedMovieOrEpisodeDateAndManualDatesKeepPrecedence() = runBlocking {
        media(1, "MOVIE")
        media(2, "SERIES")
        media(3, "MOVIE")
        sql("INSERT INTO movie_watch_progress VALUES (1,'2030-01-01T00:00:00Z','2020-02-03')")
        sql("INSERT INTO movie_watch_progress VALUES (3,'2020-01-01T00:00:00Z','2030-01-01')")
        season(1, 2, 0)
        episode(1, 1, 1, "2016-12-31T23:30:00Z")
        val repository = repository()
        suspend fun read(filter: HistoryFilter) = (
            repository.observeHistory(
                HistorySnapshotQuery(HistoryPageQuery(null, context, filter))
            ).first() as AppResult.Success
            ).value
        val all = read(HistoryFilter.ALL)
        assertEquals(listOf(2L, 1L), all.page.items.map { it.mediaId })
        assertEquals(LocalDate.of(2016, 12, 31), all.overview.firstDay)
        assertEquals(LocalDate.of(2020, 2, 3), all.overview.lastDay)
        assertEquals(listOf(1L), read(HistoryFilter.MOVIES).page.items.map { it.mediaId })
        assertEquals(listOf(2L), read(HistoryFilter.EPISODES).page.items.map { it.mediaId })
    }

    @Test fun continuousQueryMatchesExactDayWindowsAcrossShortAndLongDstDays() = runBlocking {
        media(2, "SERIES")
        season(1, 2, 1)
        listOf(
            "2026-03-28T22:59:59.999999999Z",
            "2026-03-28T23:00:00Z",
            "2026-03-29T00:59:59.999999999Z",
            "2026-03-29T01:00:00Z",
            "2026-03-29T21:59:59.999999999Z",
            "2026-03-29T22:00:00Z",
            "2026-10-24T21:59:59.999999999Z",
            "2026-10-24T22:00:00Z",
            "2026-10-25T00:59:59.999999999Z",
            "2026-10-25T01:00:00Z",
            "2026-10-25T22:59:59.999999999Z",
            "2026-10-25T23:00:00Z"
        ).forEachIndexed { index, time -> episode(index + 1, 1, index + 1, time) }
        val rome = HistoryContext(LocalDate.of(2026, 10, 31), ZoneId.of("Europe/Rome"))
        val page = timeline(rome)
        assertEquals(
            listOf(
                "2026-03-28" to 1,
                "2026-03-29" to 4,
                "2026-03-30" to 1,
                "2026-10-24" to 1,
                "2026-10-25" to 4,
                "2026-10-26" to 1
            ),
            page.items.map { it.day.toString() to it.episodeCount }
        )
        val monthly = listOf(3, 10).flatMap { number ->
            db.historyDao().readRows(HistoryQueries.page(HistoryPageQuery(YearMonth.of(2026, number), rome)))
        }
        assertEquals(monthly.map { it.day to it.episodeCount }, page.items.map { it.day to it.episodeCount })
    }

    @Test fun continuousQueryKeepsNanosecondMidnightsAndSecondBasedHistoricalOffsets() = runBlocking {
        media(2, "SERIES")
        season(1, 2, 1)
        episode(1, 1, 1, "1890-01-01T18:18:43.999999999Z")
        episode(2, 1, 2, "1890-01-01T18:18:44Z")
        episode(3, 1, 3, "1890-01-01T18:18:44.000000001Z")
        val kathmandu = HistoryContext(context.today, ZoneId.of("Asia/Kathmandu"))
        val page = timeline(kathmandu)
        assertEquals(
            listOf("1890-01-01" to 1, "1890-01-02" to 2),
            page.items.map {
                it.day.toString() to it.episodeCount
            }
        )
        val monthly = db.historyDao().readRows(HistoryQueries.page(HistoryPageQuery(YearMonth.of(1890, 1), kathmandu)))
        assertEquals(monthly.map { it.day to it.episodeCount }, page.items.map { it.day to it.episodeCount })
    }

    @Test fun continuousQueryUsesTheNewOffsetAtTheExactDateLineTransition() = runBlocking {
        media(2, "SERIES")
        season(1, 2, 1)
        episode(1, 1, 1, "2011-12-30T09:59:59.999999999Z")
        episode(2, 1, 2, "2011-12-30T10:00:00Z")
        episode(3, 1, 3, "2011-12-30T10:00:00.000000001Z")
        val apia = HistoryContext(context.today, ZoneId.of("Pacific/Apia"))
        val page = timeline(apia)
        assertEquals(
            listOf("2011-12-29" to 1, "2011-12-31" to 2),
            page.items.map {
                it.day.toString() to it.episodeCount
            }
        )
    }

    @Test fun outsideFastPathCalendarGuardRetainsProgressUsingExactDayWindows() = runBlocking {
        media(1, "MOVIE")
        sql("INSERT INTO movie_watch_progress VALUES (1,'2026-10-02T00:00:00Z','0000-01-02')")
        val page = timeline(context)
        assertEquals(listOf(LocalDate.of(0, 1, 2)), page.items.map { it.day })
    }

    @Test fun continuousWindowDoesNotRescanProgressForEveryOccupiedMonth() = runBlocking {
        db.close()
        val capturing = AtomicBoolean(false)
        val reads = AtomicInteger(0)
        db =
            Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext<Context>(),
                BingeeDatabase::class.java
            )
                .setQueryCallback({ statement, _ ->
                    if (capturing.get() && statement.contains("episode_watch_progress") &&
                        (statement.trimStart().startsWith("SELECT") || statement.trimStart().startsWith("WITH"))
                    ) {
                        reads.incrementAndGet()
                    }
                }, Executor { it.run() }).build()
        media(2, "SERIES")
        season(1, 2, 1)
        repeat(120) { index ->
            val date = YearMonth.of(2010, 1).plusMonths(index.toLong()).atDay(2)
            episode(index + 1, 1, index + 1, "${date}T00:00:00Z")
        }
        capturing.set(true)
        val page = try {
            timeline(context)
        } finally {
            capturing.set(false)
        }
        assertEquals(90, page.items.size)
        assertEquals(LocalDate.of(2010, 1, 2), page.items.first().day)
        assertTrue("Progress reads must stay bounded across 120 occupied months: ${reads.get()}", reads.get() in 1..3)
    }

    @Test fun exactAndFractionalMidnightBoundariesMatchJavaPolicy() = runBlocking {
        media(2, "SERIES")
        season(1, 2, 1)
        listOf(
            "2026-09-30T23:59:59.999999999Z",
            "2026-10-01T00:00:00Z",
            "2026-10-01T00:00:00.000000001Z",
            "2026-11-01T00:00:00Z"
        ).forEachIndexed { i, time ->
            episode(i + 1, 1, i + 1, time)
        }
        val rows = db.historyDao().readRows(HistoryQueries.page(HistoryPageQuery(month, context)))
        assertEquals(1, rows.size)
        assertEquals(2, rows.single().episodeCount)
    }

    @Test
    fun autumnDayUsesTwentyFiveHoursWithoutIncludingNextMidnight() = runBlocking {
        media(2, "SERIES")
        season(1, 2, 1)
        listOf(
            "2026-10-24T22:00:00Z",
            "2026-10-25T00:30:00Z",
            "2026-10-25T01:30:00Z",
            "2026-10-25T22:59:59.999999999Z",
            "2026-10-25T23:00:00Z"
        ).forEachIndexed { index, time ->
            episode(index + 1, 1, index + 1, time)
        }
        val rome = HistoryContext(LocalDate.of(2026, 10, 31), ZoneId.of("Europe/Rome"))
        val rows = db.historyDao().readRows(HistoryQueries.page(HistoryPageQuery(month, rome)))
        assertEquals(4, rows.single { it.day == LocalDate.of(2026, 10, 25) }.episodeCount)
        assertEquals(1, rows.single { it.day == LocalDate.of(2026, 10, 26) }.episodeCount)
    }

    @Test fun staticPagesHaveStableTiesAndReverseWithoutSkips() = runBlocking {
        (1..65).forEach { id ->
            media(id, "MOVIE")
            sql("INSERT INTO movie_watch_progress VALUES ($id,'2026-10-02T00:00:00Z',NULL)")
        }
        val first = db.historyDao().readRows(HistoryQueries.page(HistoryPageQuery(month, context)))
        assertEquals(31, first.size)
        val cursor = HistoryCursor(LocalDate.of(2026, 10, 2), 0, 30)
        val next = db.historyDao().readRows(
            HistoryQueries.page(HistoryPageQuery(month, context, cursor = cursor))
        )
        assertEquals((31L..61L).toList(), next.map { it.mediaId })
        val previous = db.historyDao().readRows(
            HistoryQueries.page(
                HistoryPageQuery(
                    month,
                    context,
                    cursor = HistoryCursor(LocalDate.of(2026, 10, 2), 0, 31),
                    backwards = true
                )
            )
        )
        assertEquals((30L downTo 1L).toList(), previous.map { it.mediaId })
        val visited = mutableListOf<Long>()
        var page = first
        while (true) {
            val visible = page.take(30)
            visited += visible.map { it.mediaId }
            if (page.size <= 30) break
            val last = visible.last()
            page = db.historyDao().readRows(
                HistoryQueries.page(
                    HistoryPageQuery(month, context, cursor = HistoryCursor(last.day, last.kind, last.mediaId))
                )
            )
        }
        assertEquals((1L..65L).toList(), visited)
    }

    @Test fun manualMovieDateMovesHistoryAndStatisticsTogetherWithoutChangingProgressTime() = runBlocking {
        media(1, "MOVIE")
        sql("UPDATE media_entries SET runtime_minutes=90 WHERE local_media_id=1")
        val timestamp = "2026-10-02T00:00:00Z"
        sql("INSERT INTO movie_watch_progress VALUES (1,'$timestamp','2026-09-02')")
        val repository = repository()
        suspend fun assertMonth(expected: Int?) {
            val entries = (repository.observePersonalViewing().first() as AppResult.Success).value
            val statistics = calculateWatchedStatistics(entries, context.zoneId, context.today, 2026)
            assertEquals(if (expected == null) 0L else 90L, statistics.movieWatchTimeMinutes)
            for (number in listOf(9, 10)) {
                val rows = db.historyDao().readRows(
                    HistoryQueries.page(HistoryPageQuery(YearMonth.of(2026, number), context))
                )
                assertEquals(if (expected == number) 1 else 0, rows.size)
                assertEquals(
                    if (expected == number) 90L else 0L,
                    statistics.monthlyViewing.months.single { it.month == number }.movieMinutes
                )
            }
            if (expected != null) assertEquals(Instant.parse(timestamp), entries.single().movieWatchedAt)
        }
        assertMonth(9)
        sql("UPDATE movie_watch_progress SET watched_date=NULL WHERE local_media_id=1")
        assertMonth(10)
        sql("DELETE FROM movie_watch_progress WHERE local_media_id=1")
        assertMonth(null)
    }

    @Test
    fun repositoryPublishesParentCountsAndChildrenFromOneTransaction() = runBlocking {
        media(2, "SERIES")
        season(1, 2, 1)
        episode(1, 1, 1)
        episode(2, 1, 2)
        val repository = repository()
        val key = "2026-10-02:SERIES:2"
        val query = HistorySnapshotQuery(
            HistoryPageQuery(month, context),
            mapOf(key to HistoryEpisodeQuery(context, LocalDate.of(2026, 10, 2), 2))
        )
        val count = MutableStateFlow(0)
        val collector = launch(Dispatchers.Default) {
            repository.observeHistory(query).collect { result ->
                val snapshot = (result as AppResult.Success).value
                val total = snapshot.page.items.single().episodeCount
                val children = (snapshot.groups.getValue(key) as AppResult.Success).value
                assertEquals(total, children.items.size)
                count.value = total
            }
        }
        try {
            withTimeout(5000) { count.first { it == 2 } }
            repeat(20) { index ->
                val expected = if (index % 2 == 0) 1 else 2
                db.withTransaction {
                    if (expected == 1) {
                        sql("DELETE FROM episode_watch_progress WHERE local_episode_id=2")
                    } else {
                        sql("INSERT INTO episode_watch_progress VALUES (2,'2026-10-02T00:00:00Z')")
                    }
                }
                withTimeout(5000) { count.first { it == expected } }
            }
        } finally {
            collector.cancelAndJoin()
        }
    }

    private suspend fun timeline(dateContext: HistoryContext): HistoryPage = (
        repository().observeHistory(
            HistorySnapshotQuery(HistoryPageQuery(null, dateContext))
        ).first() as AppResult.Success
        ).value.page

    private fun repository() = DefaultLibraryRepository(
        db.libraryDao(),
        db.watchProgressDao(),
        db.ratingDao(),
        Clock.fixed(Instant.parse("2026-10-20T12:00:00Z"), ZoneOffset.UTC),
        TestCalendarDateSource(context.today),
        Dispatchers.Default,
        db
    )

    private fun media(id: Int, type: String) {
        sql(
            "INSERT INTO media_entries(local_media_id,media_type,title,created_at,metadata_updated_at,is_favorite) VALUES ($id,'$type','Title $id','2026-01-01T00:00:00Z','2026-01-01T00:00:00Z',0)"
        )
        sql("INSERT INTO external_refs VALUES ($id,'TMDB','$type','$id')")
    }
    private fun season(id: Int, media: Int, number: Int) = sql(
        "INSERT INTO seasons(local_season_id,local_media_id,source,external_id,season_number,name,episode_count,metadata_updated_at,is_known_empty) VALUES ($id,$media,'TMDB','$id',$number,'Season',1,'2026-01-01T00:00:00Z',0)"
    )
    private fun episode(id: Int, season: Int, number: Int, time: String = "2026-10-02T00:00:00Z") {
        sql(
            "INSERT INTO episodes(local_episode_id,local_season_id,source,external_id,episode_number,title,metadata_updated_at) VALUES ($id,$season,'TMDB','$id',$number,'Episode $number','2026-01-01T00:00:00Z')"
        )
        sql("INSERT INTO episode_watch_progress VALUES ($id,'$time')")
    }
    private fun sql(statement: String) = db.openHelper.writableDatabase.execSQL(statement)
}
