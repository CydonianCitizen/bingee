package com.cydoniancitizen.bingee.data.library.local

import androidx.room.Dao
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import java.time.LocalDate

internal data class HistoryRow(
    val day: LocalDate,
    val kind: Int,
    val mediaId: Long,
    val title: String,
    val posterUrl: String?,
    val tmdbId: String?,
    val episodeCount: Int
)
internal data class HistoryEpisodeRow(val id: Long, val seasonNumber: Int, val episodeNumber: Int, val title: String)
internal data class HistoryOverviewRow(
    val firstManual: String?,
    val lastManual: String?,
    val firstMovie: String?,
    val lastMovie: String?,
    val firstEpisode: String?,
    val lastEpisode: String?
)

@Dao
internal abstract class HistoryDao {
    @RawQuery
    abstract suspend fun readRows(query: SupportSQLiteQuery): List<HistoryRow>

    @RawQuery
    abstract suspend fun readEpisodes(query: SupportSQLiteQuery): List<HistoryEpisodeRow>

    @RawQuery
    abstract suspend fun readOverview(query: SupportSQLiteQuery): HistoryOverviewRow
}
