package com.cydoniancitizen.bingee.feature.history

import com.cydoniancitizen.bingee.core.model.HistoryEpisodePage
import com.cydoniancitizen.bingee.core.model.HistoryFilter
import com.cydoniancitizen.bingee.core.model.HistoryOverview
import com.cydoniancitizen.bingee.core.model.HistoryPage
import com.cydoniancitizen.bingee.core.result.AppError

internal data class HistoryGroupState(
    val loading: Boolean = true,
    val page: HistoryEpisodePage = HistoryEpisodePage(),
    val error: AppError? = null
)

internal data class HistoryUiState(
    val filter: HistoryFilter = HistoryFilter.MOVIES,
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val page: HistoryPage = HistoryPage(),
    val overview: HistoryOverview = HistoryOverview(),
    val groups: Map<String, HistoryGroupState> = emptyMap(),
    val error: AppError? = null,
    val anchorKey: String? = null,
    val anchorOffset: Int = 0,
    val resetToken: String = ""
)
