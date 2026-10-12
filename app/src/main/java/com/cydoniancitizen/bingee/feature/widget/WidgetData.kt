package com.cydoniancitizen.bingee.feature.widget

import android.content.Context
import android.graphics.Bitmap
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.cydoniancitizen.bingee.core.model.ContinueWatchingItem
import com.cydoniancitizen.bingee.core.model.ReleaseDateCategory
import com.cydoniancitizen.bingee.core.model.ReleaseEvent
import com.cydoniancitizen.bingee.core.model.groupReleaseEvents
import com.cydoniancitizen.bingee.core.result.AppResult
import com.cydoniancitizen.bingee.data.settings.AppTheme
import com.cydoniancitizen.bingee.data.settings.AppearancePreferences
import com.cydoniancitizen.bingee.domain.calendar.CalendarDateSource
import com.cydoniancitizen.bingee.domain.repository.LibraryRepository
import com.cydoniancitizen.bingee.domain.repository.ReleaseCalendarRepository
import com.cydoniancitizen.bingee.domain.repository.WatchProgressRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

internal const val WIDGET_UPCOMING_LIMIT = 2

/** What both widgets draw: the series to continue and the next releases, read from Room only. */
internal data class WidgetSnapshot(
    val watching: ContinueWatchingItem?,
    val upcoming: List<ReleaseEvent>,
    val today: LocalDate,
    /** The in-app theme choice, which widgets follow because the launcher never sees the app's night mode. */
    val theme: AppTheme
)

/** Presence by type, not instance count: Glance renders newly placed/resized instances itself. */
internal data class WidgetTargets(val continueWatching: Boolean = false, val upNext: Boolean = false) {
    val isEmpty: Boolean get() = !continueWatching && !upNext
}

/** Only installed types observe data; an initial update also reconciles idle sessions on restart. */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun observeWidgetUpdates(
    installed: Flow<WidgetTargets>,
    snapshots: (WidgetTargets) -> Flow<WidgetSnapshot>
): Flow<WidgetTargets> = installed.distinctUntilChanged().flatMapLatest { targets ->
    if (targets.isEmpty) {
        emptyFlow()
    } else {
        flow {
            var previous: WidgetSnapshot? = null
            snapshots(targets).collect { current ->
                val old = previous
                val watchingChanged = old == null || old.theme != current.theme ||
                    !old.watching.sameWidgetContent(current.watching)
                val upcomingChanged = old == null || (old.today != current.today && current.upcoming.isNotEmpty()) ||
                    old.upcoming.size != current.upcoming.size ||
                    old.upcoming.indices.any { !old.upcoming[it].sameWidgetContent(current.upcoming[it]) }
                val changed = WidgetTargets(
                    continueWatching = targets.continueWatching && watchingChanged,
                    upNext = targets.upNext && (watchingChanged || upcomingChanged)
                )
                previous = current
                if (!changed.isEmpty) emit(changed)
            }
        }
    }
}

private fun ContinueWatchingItem?.sameWidgetContent(other: ContinueWatchingItem?): Boolean =
    if (this == null || other == null) {
        this == other
    } else {
        mediaRef == other.mediaRef && mediaType == other.mediaType && title == other.title &&
            posterUrl == other.posterUrl && nextEpisode == other.nextEpisode && nextEpisodeRef == other.nextEpisodeRef
    }

private fun ReleaseEvent.sameWidgetContent(other: ReleaseEvent): Boolean =
    mediaRef == other.mediaRef && mediaType == other.mediaType && eventDate == other.eventDate &&
        title == other.title && posterUrl == other.posterUrl && seasonNumber == other.seasonNumber &&
        episodeNumber == other.episodeNumber

/** Today's and later releases in calendar order; past ones belong to the app's "recent" section. */
internal fun selectUpcoming(events: List<ReleaseEvent>, today: LocalDate): List<ReleaseEvent> =
    groupReleaseEvents(events, today)
        .filter { it.category != ReleaseDateCategory.RECENT }
        .flatMap { it.events }
        .take(WIDGET_UPCOMING_LIMIT)

internal fun widgetDayLabel(
    date: LocalDate,
    today: LocalDate,
    todayLabel: String,
    tomorrowLabel: String,
    locale: Locale
): String = when (date) {
    today -> todayLabel
    today.plusDays(1) -> tomorrowLabel
    else -> date.format(DateTimeFormatter.ofPattern("EEE d MMM", locale))
}

/** Widgets are not Hilt entry points themselves, so they reach the app graph through this. */
@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface WidgetEntryPoint {
    fun widgetUpdater(): BingeeWidgetUpdater
    fun libraryRepository(): LibraryRepository
    fun releaseCalendarRepository(): ReleaseCalendarRepository
    fun watchProgressRepository(): WatchProgressRepository
    fun calendarDateSource(): CalendarDateSource
    fun appearancePreferences(): AppearancePreferences
}

internal fun Context.widgetEntryPoint(): WidgetEntryPoint =
    EntryPointAccessors.fromApplication(applicationContext, WidgetEntryPoint::class.java)

internal fun observeWidgetSnapshot(context: Context, includeUpcoming: Boolean = true): Flow<WidgetSnapshot> {
    val entryPoint = context.widgetEntryPoint()
    return observeWidgetSnapshot(
        watching = entryPoint.libraryRepository().observeContinueWatching(),
        events = entryPoint.releaseCalendarRepository()::observeEvents,
        dates = entryPoint.calendarDateSource().observeDate(),
        themes = entryPoint.appearancePreferences().observeTheme(),
        includeUpcoming = includeUpcoming
    )
}

@OptIn(ExperimentalCoroutinesApi::class)
internal fun observeWidgetSnapshot(
    watching: Flow<AppResult<List<ContinueWatchingItem>>>,
    events: (LocalDate) -> Flow<AppResult<List<ReleaseEvent>>>,
    dates: Flow<LocalDate>,
    themes: Flow<AppTheme>,
    includeUpcoming: Boolean = true
): Flow<WidgetSnapshot> = dates.flatMapLatest { today ->
    val releases = if (includeUpcoming) events(today) else flowOf(AppResult.Success(emptyList()))
    combine(watching, releases, themes) { progress, releases, theme ->
        WidgetSnapshot(
            watching = (progress as? AppResult.Success)?.value?.firstOrNull(),
            upcoming = selectUpcoming((releases as? AppResult.Success)?.value.orEmpty(), today),
            today = today,
            theme = theme
        )
    }
}.distinctUntilChanged { old, current ->
    old.theme == current.theme && old.watching.sameWidgetContent(current.watching) &&
        (old.today == current.today || current.upcoming.isEmpty()) &&
        old.upcoming.size == current.upcoming.size &&
        old.upcoming.indices.all { old.upcoming[it].sameWidgetContent(current.upcoming[it]) }
}

/**
 * Widgets draw through RemoteViews, which cannot take a hardware bitmap or load a URL, so the poster is
 * fetched up front through the app's image loader and its disk cache.
 */
internal suspend fun loadWidgetPoster(context: Context, url: String): Bitmap? {
    val request = ImageRequest.Builder(context)
        .data(url)
        .size(POSTER_WIDTH_PX, POSTER_HEIGHT_PX)
        .allowHardware(false)
        .build()
    return (SingletonImageLoader.get(context).execute(request) as? SuccessResult)?.image?.toBitmap()
}

private const val POSTER_WIDTH_PX = 300
private const val POSTER_HEIGHT_PX = 450
