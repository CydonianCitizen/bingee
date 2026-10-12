package com.cydoniancitizen.bingee.domain.model

import com.cydoniancitizen.bingee.core.model.MediaSource
import com.cydoniancitizen.bingee.core.model.MediaType
import com.cydoniancitizen.bingee.core.model.PersonalRating
import com.cydoniancitizen.bingee.core.model.PersonalViewingEntry
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

const val TASTE_RADAR_GENRE_LIMIT = 6

data class MonthlyViewingData(
    val year: Int,
    val month: Int,
    val movieMinutes: Long = 0L,
    val seriesMinutes: Long = 0L,
    val movieTimeIncomplete: Boolean = false,
    val seriesTimeIncomplete: Boolean = false
) {
    init {
        require(month in 1..12) { "Viewing month must be between 1 and 12" }
        require(movieMinutes >= 0) { "Movie viewing minutes must not be negative" }
        require(seriesMinutes >= 0) { "Series viewing minutes must not be negative" }
    }

    val totalMinutes: Long get() = movieMinutes + seriesMinutes
    val isIncomplete: Boolean get() = movieTimeIncomplete || seriesTimeIncomplete
}

data class MonthlyViewingStatistics(
    val currentYear: Int = 0,
    val currentMonth: Int = 0,
    val selectedYear: Int = 0,
    val availableYears: List<Int> = emptyList(),
    val months: List<MonthlyViewingData> = emptyList()
)

data class GenreStatistic(val source: MediaSource, val genreId: Long, val name: String, val titleCount: Int) {
    init {
        require(genreId > 0) { "Genre ID must be positive" }
        require(name.isNotBlank()) { "Genre name must not be blank" }
        require(titleCount > 0) { "Genre count must be positive" }
    }
}

enum class StatisticsMediaScope {
    ALL,
    MOVIES,
    SERIES
}

data class TasteStatistics(
    val scope: StatisticsMediaScope = StatisticsMediaScope.ALL,
    val rankedGenres: List<GenreStatistic> = emptyList()
) {
    val radarGenres: List<GenreStatistic> get() = rankedGenres.take(TASTE_RADAR_GENRE_LIMIT)
}

data class RatingHistogramBucket(val rating: Int, val titleCount: Int) {
    init {
        require(rating in PersonalRating.MIN_VALUE..PersonalRating.MAX_VALUE) {
            "Rating histogram value must be between 1 and 10"
        }
        require(titleCount >= 0) { "Rating histogram count must not be negative" }
    }
}

data class PersonalRatingStatistics(
    val averageRating: Double? = null,
    val histogram: List<RatingHistogramBucket> = emptyRatingHistogram(),
    val ratedTitles: List<PersonalViewingEntry> = emptyList()
) {
    val ratedTitleCount: Int get() = ratedTitles.size

    init {
        require(histogram.map(RatingHistogramBucket::rating) == PERSONAL_RATING_VALUES) {
            "Rating histogram must contain values 1 through 10"
        }
    }
}

private val PERSONAL_RATING_VALUES = (PersonalRating.MIN_VALUE..PersonalRating.MAX_VALUE).toList()

private fun emptyRatingHistogram(): List<RatingHistogramBucket> = PERSONAL_RATING_VALUES.map {
    RatingHistogramBucket(it, 0)
}

data class WatchedStatistics(
    val moviesWatchedCount: Int = 0,
    val tvSeriesCompletedCount: Int = 0,
    val episodesWatchedCount: Int = 0,
    val movieWatchTimeMinutes: Long = 0L,
    val seriesWatchTimeMinutes: Long = 0L,
    val movieWatchTimeIncomplete: Boolean = false,
    val seriesWatchTimeIncomplete: Boolean = false,
    val personalRatingStatistics: PersonalRatingStatistics = PersonalRatingStatistics(),
    val movieGenres: List<GenreStatistic> = emptyList(),
    val seriesGenres: List<GenreStatistic> = emptyList(),
    val monthlyViewing: MonthlyViewingStatistics = MonthlyViewingStatistics()
) {
    val watchTimeMinutes: Long get() = movieWatchTimeMinutes + seriesWatchTimeMinutes
    val isWatchTimeIncomplete: Boolean get() = movieWatchTimeIncomplete || seriesWatchTimeIncomplete

    val isEmpty: Boolean
        get() =
            moviesWatchedCount == 0 &&
                tvSeriesCompletedCount == 0 &&
                episodesWatchedCount == 0
}

fun calculateWatchedStatistics(
    entries: List<PersonalViewingEntry>,
    zoneId: ZoneId = ZoneId.systemDefault()
): WatchedStatistics {
    val currentDate = LocalDate.now(zoneId)
    return calculateWatchedStatistics(entries, zoneId, currentDate, currentDate.year)
}

fun calculateWatchedStatistics(
    entries: List<PersonalViewingEntry>,
    zoneId: ZoneId,
    currentDate: LocalDate,
    selectedYear: Int,
    checkCancelled: () -> Unit = {}
): WatchedStatistics {
    // TMDB reuses one ID for an unrelated movie and series, so identity needs the type (ADR 0028).
    val uniqueEntries = entries.distinctBy {
        checkCancelled()
        it.mediaRef to it.mediaType
    }
    val completedTitles = uniqueEntries.filter(PersonalViewingEntry::isCompletedTitle)
    val viewedTitles = uniqueEntries.filter(PersonalViewingEntry::isViewingTasteEligible)

    val moviesWatchedCount = completedTitles.count { it.mediaType == MediaType.MOVIE }
    val tvSeriesCompletedCount = completedTitles.count { it.mediaType == MediaType.SERIES }
    val episodesWatchedCount = viewedTitles.sumOf(PersonalViewingEntry::watchedRegularEpisodes)
    val movieTitles = viewedTitles.filter { it.mediaType == MediaType.MOVIE }
    val seriesTitles = viewedTitles.filter { it.mediaType == MediaType.SERIES }
    val movieWatchTimeMinutes = movieTitles.sumOf { it.movieRuntimeMinutes?.toLong() ?: 0L }
    val seriesWatchTimeMinutes = seriesTitles.sumOf { it.watchedRegularRuntimeMinutes }
    val personalRatingStatistics = calculatePersonalRatingStatistics(viewedTitles, checkCancelled)

    return WatchedStatistics(
        moviesWatchedCount = moviesWatchedCount,
        tvSeriesCompletedCount = tvSeriesCompletedCount,
        episodesWatchedCount = episodesWatchedCount,
        movieWatchTimeMinutes = movieWatchTimeMinutes,
        seriesWatchTimeMinutes = seriesWatchTimeMinutes,
        movieWatchTimeIncomplete = movieTitles.any { it.movieRuntimeMinutes == null },
        seriesWatchTimeIncomplete = seriesTitles.any { it.watchedRegularEpisodesWithoutRuntime > 0 },
        personalRatingStatistics = personalRatingStatistics,
        movieGenres = topGenreStatistics(movieTitles, checkCancelled),
        seriesGenres = topGenreStatistics(seriesTitles, checkCancelled),
        monthlyViewing = calculateMonthlyViewing(
            entries = uniqueEntries,
            zoneId = zoneId,
            currentDate = currentDate,
            selectedYear = selectedYear,
            checkCancelled = checkCancelled
        )
    )
}

fun calculatePersonalRatingStatistics(
    entries: List<PersonalViewingEntry>,
    checkCancelled: () -> Unit = {}
): PersonalRatingStatistics {
    val ratedTitles = entries
        .asSequence()
        .distinctBy {
            checkCancelled()
            it.mediaRef to it.mediaType
        }
        .filter { it.personalRating != null }
        .sortedWith(
            compareByDescending<PersonalViewingEntry> { it.personalRatingUpdatedAt != null }
                .thenByDescending { it.personalRatingUpdatedAt ?: Instant.MIN }
                .thenBy { it.mediaRef.source.name }
                .thenBy { it.mediaRef.externalId }
                .thenBy { it.mediaType }
        )
        .toList()
    val counts = ratedTitles.groupingBy { it.personalRating!!.value }.eachCount()
    val histogram = PERSONAL_RATING_VALUES.map { rating ->
        RatingHistogramBucket(rating, counts[rating] ?: 0)
    }
    return PersonalRatingStatistics(
        averageRating = ratedTitles.map { it.personalRating!!.value }.average().takeUnless(Double::isNaN),
        histogram = histogram,
        ratedTitles = ratedTitles
    )
}

fun relativeRatingNormalization(histogram: List<RatingHistogramBucket>): List<Float> {
    val maximum = histogram.maxOfOrNull { it.titleCount } ?: return emptyList()
    if (maximum == 0) return histogram.map { 0f }
    return histogram.map { it.titleCount.toFloat() / maximum }
}

fun formatPersonalRatingAverage(value: Double, locale: Locale): String = NumberFormat
    .getNumberInstance(locale)
    .apply {
        minimumFractionDigits = 1
        maximumFractionDigits = 1
    }
    .format(value)

private class MonthlyAccumulator {
    var movieMinutes = 0L
    var seriesMinutes = 0L
    var movieTimeIncomplete = false
    var seriesTimeIncomplete = false
}

private fun calculateMonthlyViewing(
    entries: List<PersonalViewingEntry>,
    zoneId: ZoneId,
    currentDate: LocalDate,
    selectedYear: Int,
    checkCancelled: () -> Unit
): MonthlyViewingStatistics {
    // Keep only the requested year and the current-year fallback, never a second copy of the history.
    val requested = Array(12) { MonthlyAccumulator() }
    val current = if (selectedYear == currentDate.year) requested else Array(12) { MonthlyAccumulator() }
    var firstYear = currentDate.year
    fun record(type: MediaType, watchedAt: Instant, runtime: Int?) {
        checkCancelled()
        val date = watchedAt.atZone(zoneId).toLocalDate()
        if (date.isAfter(currentDate)) return
        firstYear = minOf(firstYear, date.year)
        val bucket = when (date.year) {
            selectedYear -> requested[date.monthValue - 1]
            currentDate.year -> current[date.monthValue - 1]
            else -> return
        }
        when (type) {
            MediaType.MOVIE -> {
                bucket.movieMinutes += runtime?.toLong() ?: 0L
                bucket.movieTimeIncomplete = bucket.movieTimeIncomplete || runtime == null
            }
            MediaType.SERIES -> {
                bucket.seriesMinutes += runtime?.toLong() ?: 0L
                bucket.seriesTimeIncomplete = bucket.seriesTimeIncomplete || runtime == null
            }
        }
    }
    entries.forEach { entry ->
        checkCancelled()
        when (entry.mediaType) {
            MediaType.MOVIE -> entry.movieWatchedAt?.let { record(MediaType.MOVIE, it, entry.movieRuntimeMinutes) }
            MediaType.SERIES -> entry.watchedRegularEpisodeActivities.forEach {
                record(MediaType.SERIES, it.watchedAt, it.runtimeMinutes)
            }
        }
    }
    val availableYears = (firstYear..currentDate.year).toList().asReversed()
    val year = selectedYear.takeIf { it in firstYear..currentDate.year } ?: currentDate.year
    val buckets = if (year == selectedYear) requested else current
    return MonthlyViewingStatistics(
        currentYear = currentDate.year,
        currentMonth = currentDate.monthValue,
        selectedYear = year,
        availableYears = availableYears,
        months = buckets.mapIndexed { index, bucket ->
            MonthlyViewingData(
                year = year,
                month = index + 1,
                movieMinutes = bucket.movieMinutes,
                seriesMinutes = bucket.seriesMinutes,
                movieTimeIncomplete = bucket.movieTimeIncomplete,
                seriesTimeIncomplete = bucket.seriesTimeIncomplete
            )
        }
    )
}

fun relativeViewingNormalization(months: List<MonthlyViewingData>): List<Float> {
    val maximum = months.maxOfOrNull { it.totalMinutes } ?: return emptyList()
    if (maximum == 0L) return months.map { 0f }
    return months.map { it.totalMinutes.toFloat() / maximum }
}

fun calculateTasteStatistics(
    entries: List<PersonalViewingEntry>,
    scope: StatisticsMediaScope = StatisticsMediaScope.ALL,
    checkCancelled: () -> Unit = {}
): TasteStatistics = TasteStatistics(
    scope = scope,
    rankedGenres = rankedGenreStatistics(
        entries
            .asSequence()
            .distinctBy {
                checkCancelled()
                it.mediaRef to it.mediaType
            }
            .filter(PersonalViewingEntry::isViewingTasteEligible)
            .filter { entry ->
                when (scope) {
                    StatisticsMediaScope.ALL -> true
                    StatisticsMediaScope.MOVIES -> entry.mediaType == MediaType.MOVIE
                    StatisticsMediaScope.SERIES -> entry.mediaType == MediaType.SERIES
                }
            }
            .toList(),
        checkCancelled
    )
)

fun relativeGenreNormalization(counts: List<Int>): List<Float> {
    val maximum = counts.maxOrNull()?.coerceAtLeast(0) ?: return emptyList()
    if (maximum == 0) return counts.map { 0f }
    return counts.map { it.coerceAtLeast(0).toFloat() / maximum }
}

private fun topGenreStatistics(entries: List<PersonalViewingEntry>, checkCancelled: () -> Unit): List<GenreStatistic> =
    rankedGenreStatistics(entries, checkCancelled).take(3)

private fun rankedGenreStatistics(
    entries: List<PersonalViewingEntry>,
    checkCancelled: () -> Unit
): List<GenreStatistic> {
    val counts = linkedMapOf<Pair<MediaSource, Long>, GenreStatistic>()
    entries.forEach { entry ->
        checkCancelled()
        entry.genres
            .filter { it.source != null && it.genreId != null }
            .distinctBy { it.source!! to it.genreId!! }
            .forEach { genre ->
                val source = requireNotNull(genre.source)
                val genreId = requireNotNull(genre.genreId)
                val key = source to genreId
                counts[key] = counts[key]?.copy(titleCount = counts.getValue(key).titleCount + 1)
                    ?: GenreStatistic(source, genreId, genre.name, 1)
            }
    }
    return counts.values
        .sortedWith(
            compareByDescending<GenreStatistic> { it.titleCount }
                .thenBy { it.source.name }
                .thenBy { it.genreId }
        )
}
