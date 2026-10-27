package com.cydoniancitizen.bingee.core.model

import java.util.Locale

fun sortLibraryEntries(entries: List<LibraryEntry>): List<LibraryEntry> = entries.sortedWith(
    compareByDescending<LibraryEntry> { it.addedAt }
        .thenBy { it.title.lowercase(Locale.ROOT) }
        .thenBy { it.originalTitle?.lowercase(Locale.ROOT).orEmpty() }
        .thenBy { it.mediaRef.source.name }
        .thenBy { it.mediaRef.externalId }
)
