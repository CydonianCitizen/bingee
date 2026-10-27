package com.cydoniancitizen.bingee.core.model

enum class LibraryMediaFilter(val mediaType: MediaType?) {
    ALL(null),
    MOVIES(MediaType.MOVIE),
    TV_SERIES(MediaType.SERIES)
}

data class LibraryQuery(val mediaFilter: LibraryMediaFilter = LibraryMediaFilter.ALL)
