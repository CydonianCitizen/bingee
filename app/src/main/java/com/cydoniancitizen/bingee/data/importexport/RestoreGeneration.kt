package com.cydoniancitizen.bingee.data.importexport

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Transient identity boundary: restored local IDs can name different works. Never exported. */
@Singleton
internal class RestoreGeneration @Inject constructor() {
    private val generation = MutableStateFlow(RestoreIdentity())
    val changes = generation.asStateFlow()
    fun beginRestore() {
        generation.update { it.copy(generation = it.generation + 1, activeRestores = it.activeRestores + 1) }
    }
    fun endRestore() {
        generation.update { it.copy(generation = it.generation + 1, activeRestores = it.activeRestores - 1) }
    }
}

internal data class RestoreIdentity(val generation: Long = 0, val activeRestores: Int = 0)
