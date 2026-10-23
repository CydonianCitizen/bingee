package com.cydoniancitizen.bingee.core.navigation

import com.cydoniancitizen.bingee.core.model.MediaType

data class DetailRouteArgs(val mediaType: MediaType, val tmdbId: Long) {
    init {
        require(tmdbId > 0) { "TMDB ID must be positive" }
    }
}

object DetailRoute {
    const val MEDIA_TYPE_ARG = "mediaType"
    const val TMDB_ID_ARG = "tmdbId"
    const val TEMPLATE = "details/{$MEDIA_TYPE_ARG}/{$TMDB_ID_ARG}"
    const val EPISODE_ID_ARG = "episodeId"
    const val EPISODE_TEMPLATE = "episode/{$TMDB_ID_ARG}/{$EPISODE_ID_ARG}"

    fun episode(seriesId: Long, episodeId: Long): String {
        require(seriesId > 0 && episodeId > 0) { "TMDB IDs must be positive" }
        return "episode/$seriesId/$episodeId"
    }

    fun create(mediaType: MediaType, tmdbId: Long): String {
        require(tmdbId > 0) { "TMDB ID must be positive" }
        return "details/${mediaType.name}/$tmdbId"
    }

    fun parse(mediaType: String?, tmdbId: String?): DetailRouteArgs? {
        val parsedType = MediaType.entries.firstOrNull { it.name == mediaType } ?: return null
        val parsedId = tmdbId?.toLongOrNull()?.takeIf { it > 0 } ?: return null
        return DetailRouteArgs(parsedType, parsedId)
    }
}
