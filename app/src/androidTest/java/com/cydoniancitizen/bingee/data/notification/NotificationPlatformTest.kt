package com.cydoniancitizen.bingee.data.notification

import android.app.NotificationManager
import android.content.Context
import android.content.res.Configuration
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cydoniancitizen.bingee.R
import com.cydoniancitizen.bingee.core.model.ExternalMediaRef
import com.cydoniancitizen.bingee.core.model.MediaSource
import com.cydoniancitizen.bingee.core.model.MediaType
import com.cydoniancitizen.bingee.core.model.ReleaseEvent
import com.cydoniancitizen.bingee.core.model.ReleaseEventType
import com.cydoniancitizen.bingee.core.model.ReleaseSubjectIdentity
import com.cydoniancitizen.bingee.core.model.ReleaseSubjectType
import java.time.LocalDate
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationPlatformTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun grantNotificationPermissionForPlatformChecks() {
        context.getSystemService(NotificationManager::class.java)
            .deleteNotificationChannel(AndroidReleaseNotificationCapability.CHANNEL_ID)
    }

    @Test
    fun channelCreationIsIdempotentDefaultImportanceAndPrivate() {
        val capability = AndroidReleaseNotificationCapability(context)
        capability.ensureChannel()
        capability.ensureChannel()
        val channel = context.getSystemService(NotificationManager::class.java)
            .getNotificationChannel(AndroidReleaseNotificationCapability.CHANNEL_ID)
        assertEquals(NotificationManager.IMPORTANCE_DEFAULT, channel.importance)
        val notification = NotificationCompat.Builder(
            context,
            AndroidReleaseNotificationCapability.CHANNEL_ID
        ).setSmallIcon(com.cydoniancitizen.bingee.R.drawable.ic_notification)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()
        assertEquals(NotificationCompat.VISIBILITY_PRIVATE, notification.visibility)
    }

    @Test
    fun localizedContentCoversMovieSeasonSpecialAndUntitledEpisode() {
        listOf("en", "it").forEach { language ->
            val localizedContext = context.createConfigurationContext(
                Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag(language)) }
            )
            val mapper = AndroidReleaseNotificationContentMapper(localizedContext)
            val movie = mapper.map(event(ReleaseEventType.MOVIE_RELEASE), 0)
            val season = mapper.map(event(ReleaseEventType.SEASON_PREMIERE, season = 0), 1)
            val episode = mapper.map(event(ReleaseEventType.EPISODE_AIRING, season = 1, episode = 2), 3)
            assertEquals(
                localizedContext.getString(
                    R.string.notification_movie_body,
                    localizedContext.getString(R.string.notification_relative_today)
                ),
                movie.body
            )
            assertEquals(
                localizedContext.getString(
                    R.string.notification_season_body,
                    0,
                    localizedContext.getString(R.string.notification_relative_tomorrow)
                ),
                season.body
            )
            assertEquals(
                localizedContext.getString(
                    R.string.notification_episode_body,
                    1,
                    2,
                    localizedContext.resources.getQuantityString(R.plurals.notification_relative_days, 3, 3),
                    localizedContext.getString(R.string.home_episode_title_unknown)
                ),
                episode.body
            )
        }
    }

    private fun event(type: ReleaseEventType, season: Int? = null, episode: Int? = null): ReleaseEvent {
        val subjectType = when (type) {
            ReleaseEventType.MOVIE_RELEASE -> ReleaseSubjectType.MEDIA
            ReleaseEventType.SEASON_PREMIERE -> ReleaseSubjectType.SEASON
            ReleaseEventType.EPISODE_AIRING -> ReleaseSubjectType.EPISODE
        }

        val source = MediaSource.TMDB
        val mediaType = when (type) {
            ReleaseEventType.MOVIE_RELEASE -> MediaType.MOVIE
            else -> MediaType.SERIES
        }
        return ReleaseEvent(
            mediaRef = ExternalMediaRef(source, "42"),
            subject = ReleaseSubjectIdentity(source, subjectType, "7", type),
            mediaType = mediaType,
            eventDate = LocalDate.of(2026, 8, 4),
            title = "Fixture",
            seasonNumber = season,
            episodeNumber = episode
        )
    }
}
