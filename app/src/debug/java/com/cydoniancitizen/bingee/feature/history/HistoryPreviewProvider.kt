package com.cydoniancitizen.bingee.feature.history

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.cydoniancitizen.bingee.core.designsystem.theme.BingeeTheme
import com.cydoniancitizen.bingee.core.model.HistoryEpisode
import com.cydoniancitizen.bingee.core.model.HistoryEpisodePage
import com.cydoniancitizen.bingee.core.model.HistoryFilter
import com.cydoniancitizen.bingee.core.model.HistoryItem
import com.cydoniancitizen.bingee.core.model.HistoryPage
import com.cydoniancitizen.bingee.core.model.MediaType
import com.cydoniancitizen.bingee.core.result.AppError
import java.time.LocalDate

internal class HistoryPreviewProvider : PreviewParameterProvider<HistoryUiState> {

    private val movie = HistoryItem(
        LocalDate.of(2026, 10, 2),
        1,
        MediaType.MOVIE,
        "A movie with a manually selected viewing date",
        null,
        null,
        0
    )
    private val series = HistoryItem(
        movie.day,
        2,
        MediaType.SERIES,
        "A very long series title that must remain readable with large fonts",
        null,
        null,
        100000
    )
    override val values = sequenceOf(
        HistoryUiState(),
        HistoryUiState(loading = false),
        HistoryUiState(loading = false, error = AppError.LocalStorageFailure),
        HistoryUiState(loading = false, page = HistoryPage(listOf(movie, movie.copy(mediaId = 3)))),
        HistoryUiState(filter = HistoryFilter.EPISODES, loading = false, page = HistoryPage(listOf(series))),
        HistoryUiState(
            filter = HistoryFilter.EPISODES,
            loading = false,
            page = HistoryPage(listOf(series.copy(day = series.day.minusDays(1)), series)),
            groups = mapOf(
                series.key to HistoryGroupState(
                    false,
                    HistoryEpisodePage(
                        listOf(
                            HistoryEpisode(1, 0, 1, "Special with a long title"),
                            HistoryEpisode(2, 1, 1, "Regular episode")
                        ),
                        hasNext = true
                    )
                )
            )
        )
    )
}

@Preview(name = "History 320 large", widthDp = 320, fontScale = 1.5f, showBackground = true)
@Preview(name = "History dark", widthDp = 411, uiMode = 0x20, showBackground = true)
@Composable
private fun HistoryPreview(@PreviewParameter(HistoryPreviewProvider::class) state: HistoryUiState) {
    BingeeTheme { HistoryContent(state, onBack = {}, onOpenDetails = { _, _ -> }) }
}
