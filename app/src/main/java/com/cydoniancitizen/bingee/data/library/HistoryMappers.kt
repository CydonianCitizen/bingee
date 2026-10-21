package com.cydoniancitizen.bingee.data.library

import androidx.sqlite.db.SimpleSQLiteQuery
import com.cydoniancitizen.bingee.core.model.ExternalMediaRef
import com.cydoniancitizen.bingee.core.model.HISTORY_EPISODE_PAGE_SIZE
import com.cydoniancitizen.bingee.core.model.HISTORY_WINDOW_SIZE
import com.cydoniancitizen.bingee.core.model.HistoryEpisode
import com.cydoniancitizen.bingee.core.model.HistoryEpisodePage
import com.cydoniancitizen.bingee.core.model.HistoryEpisodeQuery
import com.cydoniancitizen.bingee.core.model.HistoryFilter
import com.cydoniancitizen.bingee.core.model.HistoryItem
import com.cydoniancitizen.bingee.core.model.HistoryOverview
import com.cydoniancitizen.bingee.core.model.HistoryOverviewQuery
import com.cydoniancitizen.bingee.core.model.HistoryPage
import com.cydoniancitizen.bingee.core.model.HistoryPageQuery
import com.cydoniancitizen.bingee.core.model.MediaSource
import com.cydoniancitizen.bingee.core.model.MediaType
import com.cydoniancitizen.bingee.core.model.toNavigableDetailsRef
import com.cydoniancitizen.bingee.data.library.local.HistoryDao
import com.cydoniancitizen.bingee.data.library.local.HistoryEpisodeRow
import com.cydoniancitizen.bingee.data.library.local.HistoryOverviewRow
import com.cydoniancitizen.bingee.data.library.local.HistoryRow
import com.cydoniancitizen.bingee.domain.policy.ViewingDatePolicy
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

internal object HistoryQueries {
    // Remove Z rather than compare raw ISO strings: exact seconds must precede fractional seconds.
    // All day boundaries are whole seconds, including zones with historical second-based offsets.
    private fun instant(value: Instant): String = value.toString().removeSuffix("Z")

    fun page(query: HistoryPageQuery): SimpleSQLiteQuery {
        require(query.limit in 0..HISTORY_WINDOW_SIZE)
        val args = mutableListOf<Any>()
        val month = requireNotNull(query.month)
        val days = (1..month.lengthOfMonth()).map { month.atDay(it) }
            .filter { !it.isAfter(query.context.today) }
        if (days.isEmpty()) {
            return SimpleSQLiteQuery(
                "SELECT '' AS day, 0 AS kind, 0 AS mediaId, '' AS title, " +
                    "NULL AS posterUrl, NULL AS tmdbId, 0 AS episodeCount WHERE 0"
            )
        }
        val windows = days.map { ViewingDatePolicy.dayWindow(it, query.context.zoneId) }

        // Binary day lookup avoids scanning every progress row once for each day in the month.
        fun dayExpression(start: Int = 0, end: Int = days.size): String {
            if (end - start == 1) {
                args.add(days[start].toString())
                return "?"
            }
            val middle = (start + end) / 2
            args.add(instant(windows[middle].start))
            val left = dayExpression(start, middle)
            val right = dayExpression(middle, end)
            return "CASE WHEN rtrim(p.watched_at,'Z') < ? THEN $left ELSE $right END"
        }
        fun movies(): String {
            val day = dayExpression()
            args.add(days.first().toString())
            args.add(days.last().toString())
            args.add(instant(windows.first().start))
            args.add(instant(windows.last().endExclusive))
            return """
                SELECT COALESCE(p.watched_date,$day) AS day,
                    0 AS kind, m.local_media_id AS mediaId, 0 AS episodeCount
                FROM movie_watch_progress p JOIN media_entries m USING(local_media_id)
                WHERE m.media_type='MOVIE' AND (p.watched_date BETWEEN ? AND ? OR
                    (p.watched_date IS NULL AND rtrim(p.watched_at,'Z') >= ? AND rtrim(p.watched_at,'Z') < ?))
            """.trimIndent()
        }
        fun episodes(): String {
            val day = dayExpression()
            args.add(instant(windows.first().start))
            args.add(instant(windows.last().endExclusive))
            return """
                SELECT $day AS day, 1 AS kind, s.local_media_id AS mediaId, COUNT(*) AS episodeCount
                FROM episode_watch_progress p
                JOIN episodes e USING(local_episode_id) JOIN seasons s USING(local_season_id)
                JOIN media_entries m ON m.local_media_id=s.local_media_id
                WHERE m.media_type='SERIES' AND rtrim(p.watched_at,'Z') >= ? AND rtrim(p.watched_at,'Z') < ?
                GROUP BY day,s.local_media_id
            """.trimIndent()
        }
        val activity = when (query.filter) {
            HistoryFilter.ALL -> movies() + " UNION ALL " + episodes()
            HistoryFilter.MOVIES -> movies()
            HistoryFilter.EPISODES -> episodes()
        }
        return projection(query, activity, args)
    }

    fun timeline(query: HistoryPageQuery, overview: HistoryOverview): SimpleSQLiteQuery? {
        val firstDay = overview.firstDay ?: return null
        // SQLite calendar arithmetic supports only four-digit years. Keep the exact day-window path outside it.
        if (firstDay.year !in 1..9998 || query.context.today.year !in 1..9998) return null
        val start = firstDay.minusDays(2).atStartOfDay(query.context.zoneId).toInstant()
        val end = ViewingDatePolicy.dayWindow(query.context.today, query.context.zoneId).endExclusive
        val rules = query.context.zoneId.rules
        val starts = mutableListOf(start)
        val offsets = mutableListOf(rules.getOffset(start).totalSeconds)
        var transition = rules.nextTransition(start)
        while (transition != null && transition.instant < end) {
            // Both media branches bind these boundaries; stay below Android SQLite's 999-parameter floor.
            if (starts.size >= 400) return null
            starts += transition.instant
            offsets += transition.offsetAfter.totalSeconds
            transition = rules.nextTransition(transition.instant)
        }
        val args = mutableListOf<Any>()
        fun offsetExpression(from: Int = 0, until: Int = starts.size): String {
            if (until - from == 1) return "'${offsets[from]} seconds'"
            val middle = (from + until) / 2
            args.add(instant(starts[middle]))
            val left = offsetExpression(from, middle)
            val right = offsetExpression(middle, until)
            return "CASE WHEN rtrim(p.watched_at,'Z') < ? THEN $left ELSE $right END"
        }
        fun dayExpression(): String = // Whole-second truncation prevents SQLite rounding .999999999 into the next day.
            "date(substr(p.watched_at,1,19),${offsetExpression()})"
        fun movies(): String {
            val day = dayExpression()
            args.add(query.context.today.toString())
            args.add(instant(start))
            args.add(instant(end))
            return """
                SELECT COALESCE(p.watched_date,$day) AS day,
                    0 AS kind,m.local_media_id AS mediaId,0 AS episodeCount
                FROM movie_watch_progress p JOIN media_entries m USING(local_media_id)
                WHERE m.media_type='MOVIE' AND (p.watched_date <= ? OR
                    (p.watched_date IS NULL AND rtrim(p.watched_at,'Z') >= ? AND rtrim(p.watched_at,'Z') < ?))
            """.trimIndent()
        }
        fun episodes(): String {
            val day = dayExpression()
            args.add(instant(start))
            args.add(instant(end))
            return """
                SELECT $day AS day,1 AS kind,s.local_media_id AS mediaId,COUNT(*) AS episodeCount
                FROM episode_watch_progress p JOIN episodes e USING(local_episode_id)
                JOIN seasons s USING(local_season_id) JOIN media_entries m ON m.local_media_id=s.local_media_id
                WHERE m.media_type='SERIES' AND rtrim(p.watched_at,'Z') >= ? AND rtrim(p.watched_at,'Z') < ?
                GROUP BY day,s.local_media_id
            """.trimIndent()
        }
        val activity = when (query.filter) {
            HistoryFilter.ALL -> movies() + " UNION ALL " + episodes()
            HistoryFilter.MOVIES -> movies()
            HistoryFilter.EPISODES -> episodes()
        }
        return projection(query, activity, args)
    }

    private fun projection(query: HistoryPageQuery, activity: String, args: MutableList<Any>): SimpleSQLiteQuery {
        val cursor = query.cursor?.let {
            args.add(it.day.toString())
            args.add(it.day.toString())
            args.add(it.kind)
            args.add(it.kind)
            args.add(it.mediaId)
            val op = if (query.backwards) "<" else ">"
            "WHERE (a.day $op ? OR (a.day = ? AND " +
                "(a.kind $op ? OR (a.kind = ? AND a.mediaId $op ?))))"
        }.orEmpty()
        val order = if (query.backwards) {
            "a.day DESC,a.kind DESC,a.mediaId DESC"
        } else {
            "a.day ASC,a.kind ASC,a.mediaId ASC"
        }
        return SimpleSQLiteQuery(
            """
            WITH activity AS ($activity)
            SELECT a.day,a.kind,a.mediaId,m.title,m.poster_url AS posterUrl,
              (SELECT external_id FROM external_refs r WHERE r.local_media_id=a.mediaId
                AND r.source='TMDB' AND r.media_type=m.media_type ORDER BY external_id LIMIT 1) AS tmdbId,
              a.episodeCount
            FROM activity a JOIN media_entries m ON m.local_media_id=a.mediaId
            $cursor ORDER BY $order LIMIT ${query.limit + 1}
            """.trimIndent(),
            args.toTypedArray()
        )
    }

    fun episodes(query: HistoryEpisodeQuery): SimpleSQLiteQuery {
        val window = ViewingDatePolicy.dayWindow(query.day, query.context.zoneId)
        val args = mutableListOf<Any>(query.mediaId, instant(window.start), instant(window.endExclusive))
        val cursor = query.cursor?.let {
            args.add(it.season)
            args.add(it.season)
            args.add(it.episode)
            args.add(it.episode)
            args.add(it.id)
            val op = if (query.backwards) "<" else ">"
            "AND (s.season_number $op ? OR (s.season_number = ? AND " +
                "(e.episode_number $op ? OR (e.episode_number = ? AND e.local_episode_id $op ?))))"
        }.orEmpty()
        val order = if (query.backwards) "DESC" else "ASC"
        return SimpleSQLiteQuery(
            """
            SELECT e.local_episode_id AS id,s.season_number AS seasonNumber,e.episode_number AS episodeNumber,e.title
            FROM episode_watch_progress p JOIN episodes e USING(local_episode_id) JOIN seasons s USING(local_season_id)
            WHERE s.local_media_id=? AND rtrim(p.watched_at,'Z') >= ? AND rtrim(p.watched_at,'Z') < ? $cursor
            ORDER BY s.season_number $order,e.episode_number $order,e.local_episode_id $order
            LIMIT ${HISTORY_EPISODE_PAGE_SIZE + 1}
            """.trimIndent(),
            args.toTypedArray()
        )
    }

    fun overview(query: HistoryOverviewQuery): SimpleSQLiteQuery {
        val args = mutableListOf<Any>()
        val endDay = minOf(query.untilDay ?: query.context.today.plusDays(1), query.context.today.plusDays(1))
        val end = instant(endDay.atStartOfDay(query.context.zoneId).toInstant())
        val start = query.fromDay?.atStartOfDay(query.context.zoneId)?.toInstant()?.let(::instant)
        fun manual(aggregate: String): String {
            args.add(endDay.toString())
            val lower = query.fromDay?.let {
                args.add(it.toString())
                "AND watched_date >= ?"
            }.orEmpty()
            return "(SELECT $aggregate(watched_date) FROM movie_watch_progress " +
                "WHERE ${query.filter != HistoryFilter.EPISODES} AND watched_date < ? $lower)"
        }
        fun timestamp(table: String, aggregate: String, enabled: Boolean, extra: String = ""): String {
            args.add(end)
            val lower = start?.let {
                args.add(it)
                "AND rtrim(watched_at,'Z') >= ?"
            }.orEmpty()
            return "(SELECT $aggregate(rtrim(watched_at,'Z')) FROM $table " +
                "WHERE $enabled $extra AND rtrim(watched_at,'Z') < ? $lower)"
        }
        val firstManual = manual("MIN")
        val lastManual = manual("MAX")
        val firstMovie =
            timestamp("movie_watch_progress", "MIN", query.filter != HistoryFilter.EPISODES, "AND watched_date IS NULL")
        val lastMovie =
            timestamp("movie_watch_progress", "MAX", query.filter != HistoryFilter.EPISODES, "AND watched_date IS NULL")
        val firstEpisode = timestamp("episode_watch_progress", "MIN", query.filter != HistoryFilter.MOVIES)
        val lastEpisode = timestamp("episode_watch_progress", "MAX", query.filter != HistoryFilter.MOVIES)
        return SimpleSQLiteQuery(
            """
            SELECT
            $firstManual AS firstManual, $lastManual AS lastManual,
            $firstMovie AS firstMovie, $lastMovie AS lastMovie,
            $firstEpisode AS firstEpisode, $lastEpisode AS lastEpisode
            """.trimIndent(),
            args.toTypedArray()
        )
    }
}

internal fun List<HistoryRow>.toHistoryPage(query: HistoryPageQuery): HistoryPage {
    val selected = take(query.limit).let { if (query.backwards) it.asReversed() else it }
    return HistoryPage(
        selected.map { row ->
            val type = if (row.kind == 0) MediaType.MOVIE else MediaType.SERIES
            HistoryItem(
                row.day,
                row.mediaId,
                type,
                row.title,
                row.posterUrl,
                row.tmdbId?.let { ExternalMediaRef(MediaSource.TMDB, it).toNavigableDetailsRef() },
                row.episodeCount
            )
        },
        if (query.backwards) size > query.limit else query.cursor != null,
        if (query.backwards) query.cursor != null else size > query.limit
    )
}

internal suspend fun HistoryDao.readHistoryWindow(query: HistoryPageQuery, overview: HistoryOverview): HistoryPage {
    if (query.month != null) return readRows(HistoryQueries.page(query)).toHistoryPage(query)
    require(query.limit in 1..HISTORY_WINDOW_SIZE)
    val firstDay = query.cursor?.day ?: (if (query.backwards) overview.lastDay else overview.firstDay)
        ?: return HistoryPage()
    HistoryQueries.timeline(query, overview)?.let { return readRows(it).toHistoryPage(query) }
    var month = YearMonth.from(firstDay)
    var cursor = query.cursor
    val rows = mutableListOf<HistoryRow>()
    while (rows.size <= query.limit) {
        rows +=
            readRows(HistoryQueries.page(query.copy(month = month, cursor = cursor, limit = query.limit - rows.size)))
        if (rows.size > query.limit) break
        // Jump over empty months using retained progress, never iterate through calendar gaps.
        val boundsQuery = HistoryOverviewQuery(
            query.context,
            query.filter,
            fromDay = if (query.backwards) null else month.plusMonths(1).atDay(1),
            untilDay = if (query.backwards) month.atDay(1) else null
        )
        val bounds = readOverview(HistoryQueries.overview(boundsQuery)).toHistoryOverview(boundsQuery)
        val next = (if (query.backwards) bounds.lastDay else bounds.firstDay) ?: break
        month = YearMonth.from(next)
        cursor = null
    }
    return rows.toHistoryPage(query)
}

internal fun List<HistoryEpisodeRow>.toEpisodePage(query: HistoryEpisodeQuery): HistoryEpisodePage = HistoryEpisodePage(
    take(HISTORY_EPISODE_PAGE_SIZE).let { if (query.backwards) it.asReversed() else it }
        .map { HistoryEpisode(it.id, it.seasonNumber, it.episodeNumber, it.title) },
    if (query.backwards) size > HISTORY_EPISODE_PAGE_SIZE else query.cursor != null,
    if (query.backwards) query.cursor != null else size > HISTORY_EPISODE_PAGE_SIZE
)

internal fun HistoryOverviewRow.toHistoryOverview(query: HistoryOverviewQuery): HistoryOverview {
    fun date(value: String?): LocalDate? =
        value?.let { ViewingDatePolicy.effectiveEpisodeDate(Instant.parse(it + "Z"), query.context.zoneId) }
    val days = listOfNotNull(
        firstManual?.let(LocalDate::parse),
        lastManual?.let(LocalDate::parse),
        date(firstMovie),
        date(lastMovie),
        date(firstEpisode),
        date(lastEpisode)
    )
    return HistoryOverview(days.minOrNull(), days.maxOrNull())
}
