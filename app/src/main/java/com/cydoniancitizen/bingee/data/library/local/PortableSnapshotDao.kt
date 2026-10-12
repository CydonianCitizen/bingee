package com.cydoniancitizen.bingee.data.library.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

// All user-owned state makes its parent title portable, including history outside the library.
private const val PORTABLE_MEDIA_IDS = """
    SELECT local_media_id FROM library_entries
    UNION SELECT local_media_id FROM media_ratings
    UNION SELECT local_media_id FROM movie_watch_progress
    UNION SELECT local_media_id FROM series_watch_progress
    UNION SELECT local_media_id FROM series_state_overrides
    UNION SELECT local_media_id FROM media_entries WHERE is_favorite = 1
    UNION SELECT s.local_media_id FROM episode_watch_progress p
      JOIN episodes e ON e.local_episode_id = p.local_episode_id
      JOIN seasons s ON s.local_season_id = e.local_season_id
"""

internal data class PortableSnapshotRows(
    val media: List<MediaEntity>,
    val refs: List<ExternalRefEntity>,
    val genres: List<MediaGenreEntity>,
    val memberships: List<LibraryMembershipEntity>,
    val seasons: List<SeasonEntity>,
    val episodes: List<EpisodeEntity>,
    val episodeProgress: List<EpisodeWatchProgressEntity>,
    val movieProgress: List<MovieWatchProgressEntity>,
    val seriesProgress: List<SeriesWatchProgressEntity>,
    val seriesStateOverrides: List<SeriesStateOverrideEntity>,
    val ratings: List<MediaRatingEntity>,
    val preferences: PortablePreferencesEntity?
)

@Dao
internal abstract class PortableSnapshotDao {
    @Query(
        "SELECT * FROM media_genres WHERE :includeCache OR local_media_id IN ($PORTABLE_MEDIA_IDS) ORDER BY local_media_id, genre_order"
    )
    protected abstract suspend fun getGenres(includeCache: Boolean): List<MediaGenreEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertGenres(genres: List<MediaGenreEntity>)

    @Query(
        "SELECT * FROM media_entries WHERE :includeCache OR local_media_id IN ($PORTABLE_MEDIA_IDS) ORDER BY local_media_id"
    )
    protected abstract suspend fun getMedia(includeCache: Boolean): List<MediaEntity>

    @Query(
        "SELECT * FROM external_refs WHERE :includeCache OR local_media_id IN ($PORTABLE_MEDIA_IDS) ORDER BY source, external_id"
    )
    protected abstract suspend fun getRefs(includeCache: Boolean): List<ExternalRefEntity>

    @Query("SELECT * FROM library_entries ORDER BY local_media_id")
    protected abstract suspend fun getMemberships(): List<LibraryMembershipEntity>

    @Query(
        "SELECT * FROM seasons WHERE :includeCache OR local_media_id IN ($PORTABLE_MEDIA_IDS) ORDER BY local_season_id"
    )
    protected abstract suspend fun getSeasons(includeCache: Boolean): List<SeasonEntity>

    @Query(
        """
        SELECT * FROM episodes WHERE :includeCache OR local_season_id IN (
            SELECT local_season_id FROM seasons WHERE local_media_id IN ($PORTABLE_MEDIA_IDS)
        ) ORDER BY local_episode_id
    """
    )
    protected abstract suspend fun getEpisodes(includeCache: Boolean): List<EpisodeEntity>

    @Query("SELECT * FROM episode_watch_progress ORDER BY local_episode_id")
    protected abstract suspend fun getEpisodeProgress(): List<EpisodeWatchProgressEntity>

    @Query("SELECT * FROM movie_watch_progress ORDER BY local_media_id")
    protected abstract suspend fun getMovieProgress(): List<MovieWatchProgressEntity>

    @Query("SELECT * FROM series_watch_progress ORDER BY local_media_id")
    protected abstract suspend fun getSeriesProgress(): List<SeriesWatchProgressEntity>

    @Query("SELECT * FROM series_state_overrides ORDER BY local_media_id")
    protected abstract suspend fun getSeriesStateOverrides(): List<SeriesStateOverrideEntity>

    @Query("SELECT * FROM media_ratings ORDER BY local_media_id")
    protected abstract suspend fun getRatings(): List<MediaRatingEntity>

    @Query("SELECT * FROM portable_preferences WHERE singleton_key = 1 LIMIT 1")
    abstract suspend fun getPreferences(): PortablePreferencesEntity?

    @Query("SELECT * FROM portable_preferences WHERE singleton_key = 1 LIMIT 1")
    abstract fun observePreferences(): Flow<PortablePreferencesEntity?>

    @Query("SELECT COUNT(*) FROM library_entries")
    abstract suspend fun countLibraryEntries(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertMedia(media: MediaEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertExternalRef(ref: ExternalRefEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertMembership(membership: LibraryMembershipEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertSeason(season: SeasonEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertEpisode(episode: EpisodeEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertEpisodeProgress(progress: EpisodeWatchProgressEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertMovieProgress(progress: MovieWatchProgressEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertSeriesProgress(progress: SeriesWatchProgressEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertSeriesStateOverride(override: SeriesStateOverrideEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertRating(rating: MediaRatingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun replacePreferences(preferences: PortablePreferencesEntity)

    @Query("DELETE FROM notification_deliveries")
    abstract suspend fun deleteNotificationDeliveries()

    @Query("DELETE FROM release_events")
    abstract suspend fun deleteReleaseEvents()

    @Query("DELETE FROM episode_watch_progress")
    abstract suspend fun deleteEpisodeProgress()

    @Query("DELETE FROM movie_watch_progress")
    abstract suspend fun deleteMovieProgress()

    @Query("DELETE FROM series_watch_progress")
    abstract suspend fun deleteSeriesProgress()

    @Query("DELETE FROM series_state_overrides")
    abstract suspend fun deleteSeriesStateOverrides()

    @Query("DELETE FROM media_ratings")
    abstract suspend fun deleteRatings()

    @Query("DELETE FROM library_entries")
    abstract suspend fun deleteMemberships()

    @Query("DELETE FROM episodes")
    abstract suspend fun deleteEpisodes()

    @Query("DELETE FROM seasons")
    abstract suspend fun deleteSeasons()

    @Query("DELETE FROM media_genres")
    abstract suspend fun deleteGenres()

    @Query("DELETE FROM media_details")
    abstract suspend fun deleteDetails()

    @Query("DELETE FROM external_refs")
    abstract suspend fun deleteRefs()

    @Query("DELETE FROM media_entries")
    abstract suspend fun deleteMedia()

    @Query("DELETE FROM calendar_refresh_state")
    abstract suspend fun deleteCalendarRefreshState()

    @Query("DELETE FROM portable_preferences")
    abstract suspend fun deletePreferences()

    @Transaction
    open suspend fun readSnapshot(includeCache: Boolean = true): PortableSnapshotRows = PortableSnapshotRows(
        media = getMedia(includeCache), refs = getRefs(includeCache), memberships = getMemberships(),
        genres = getGenres(includeCache),
        seasons = getSeasons(includeCache),
        episodes = getEpisodes(includeCache), episodeProgress = getEpisodeProgress(),
        movieProgress = getMovieProgress(), seriesProgress = getSeriesProgress(),
        seriesStateOverrides = getSeriesStateOverrides(),
        ratings = getRatings(),
        preferences = getPreferences()
    )
}
