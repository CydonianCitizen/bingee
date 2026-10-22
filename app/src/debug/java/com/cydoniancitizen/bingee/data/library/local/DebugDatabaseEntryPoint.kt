package com.cydoniancitizen.bingee.data.library.local

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Fixture writes must invalidate the same Room instance observed by the real app. */
@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface DebugDatabaseEntryPoint {
    fun database(): BingeeDatabase
}
