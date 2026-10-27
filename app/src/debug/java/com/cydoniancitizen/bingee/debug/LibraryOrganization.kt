package com.cydoniancitizen.bingee.debug

import com.cydoniancitizen.bingee.core.model.LibraryEntry
import com.cydoniancitizen.bingee.core.model.LibraryQuery
import com.cydoniancitizen.bingee.core.model.sortLibraryEntries

/** In-memory media restriction for the debug repository; production filters through Room. */
internal fun organizeLibraryEntries(entries: List<LibraryEntry>, query: LibraryQuery): List<LibraryEntry> =
    sortLibraryEntries(
        entries.filter {
            query.mediaFilter.mediaType == null ||
                it.mediaType == query.mediaFilter.mediaType
        }
    )
