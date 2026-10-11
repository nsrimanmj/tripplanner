package com.example.tripchecker.ui.importflow

import androidx.lifecycle.ViewModel
import com.example.tripchecker.importer.ImportItem
import com.example.tripchecker.importer.ImportItemState
import com.example.tripchecker.importer.ImportManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.viewModelScope

data class ImportUiState(
    val items: List<ImportItem> = emptyList(),
    val summary: String = "",
)

class ImportViewModel(
    private val importManager: ImportManager,
) : ViewModel() {
    val uiState: StateFlow<ImportUiState> =
        importManager.items
            .map { items -> ImportUiState(items = items, summary = summarize(items)) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ImportUiState())

    fun clearFinished() = importManager.clearFinished()

    private fun summarize(items: List<ImportItem>): String {
        var saved = 0
        var updated = 0
        var duplicate = 0
        var review = 0
        var failed = 0
        items.forEach { item ->
            when (item.state) {
                is ImportItemState.Saved -> saved++
                is ImportItemState.Updated -> updated++
                is ImportItemState.Duplicate -> duplicate++
                is ImportItemState.NeedsReview -> review++
                is ImportItemState.Failed -> failed++
                else -> Unit
            }
        }
        return buildList {
            if (saved > 0) add("$saved saved")
            if (updated > 0) add("$updated updated")
            if (duplicate > 0) add("$duplicate duplicate")
            if (review > 0) add("$review needs review")
            if (failed > 0) add("$failed failed")
        }.joinToString(" · ")
    }
}
