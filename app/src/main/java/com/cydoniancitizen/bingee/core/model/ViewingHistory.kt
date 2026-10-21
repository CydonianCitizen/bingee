package com.cydoniancitizen.bingee.core.model

import com.cydoniancitizen.bingee.core.result.AppResult
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

enum class HistoryFilter { ALL, MOVIES, EPISODES }
data class HistoryContext(val today: LocalDate, val zoneId: ZoneId)
data class HistoryCursor(val day: LocalDate, val kind: Int, val mediaId: Long)
data class HistoryEpisodeCursor(val season: Int, val episode: Int, val id: Long)
data class HistoryOverviewQuery(
    val context: HistoryContext,
    val filter: HistoryFilter = HistoryFilter.ALL,
    val fromDay: LocalDate? = null,
    val untilDay: LocalDate? = null
)
data class HistoryPageQuery(
    val month: YearMonth?,
    val context: HistoryContext,
    val filter: HistoryFilter = HistoryFilter.ALL,
    val cursor: HistoryCursor? = null,
    val backwards: Boolean = false,
    val limit: Int = if (month == null) HISTORY_WINDOW_SIZE else HISTORY_PAGE_SIZE
)
data class HistoryEpisodeQuery(
    val context: HistoryContext,
    val day: LocalDate,
    val mediaId: Long,
    val cursor: HistoryEpisodeCursor? = null,
    val backwards: Boolean = false
)

data class HistorySnapshotQuery(
    val page: HistoryPageQuery,
    val groups: Map<String, HistoryEpisodeQuery> = emptyMap(),
    val restoreGeneration: Long = 0
)

data class HistorySnapshot(
    val overview: HistoryOverview,
    val page: HistoryPage,
    val groups: Map<String, AppResult<HistoryEpisodePage>> = emptyMap()
)
data class HistoryOverview(val firstDay: LocalDate? = null, val lastDay: LocalDate? = null)
data class HistoryItem(
    val day: LocalDate,
    val mediaId: Long,
    val mediaType: MediaType,
    val title: String,
    val posterUrl: String?,
    val detailsRef: ExternalMediaRef?,
    val episodeCount: Int
) {
    val cursor: HistoryCursor get() = HistoryCursor(day, if (mediaType == MediaType.MOVIE) 0 else 1, mediaId)
    val key: String get() = "$day:${mediaType.name}:$mediaId"
}
data class HistoryEpisode(val id: Long, val season: Int, val number: Int, val title: String) {
    val cursor: HistoryEpisodeCursor get() = HistoryEpisodeCursor(season, number, id)
}
data class HistoryPage(
    val items: List<HistoryItem> = emptyList(),
    val hasPrevious: Boolean = false,
    val hasNext: Boolean = false
)
data class HistoryEpisodePage(
    val items: List<HistoryEpisode> = emptyList(),
    val hasPrevious: Boolean = false,
    val hasNext: Boolean = false
)

const val HISTORY_PAGE_SIZE = 30
const val HISTORY_WINDOW_SIZE = HISTORY_PAGE_SIZE * 3
const val HISTORY_EPISODE_PAGE_SIZE = 20
