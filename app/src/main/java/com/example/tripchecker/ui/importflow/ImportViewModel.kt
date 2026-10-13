package com.example.tripchecker.ui.importflow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tripchecker.data.repo.TicketRepository
import com.example.tripchecker.domain.ObserveJourneysUseCase
import com.example.tripchecker.importer.ImportManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** UI state of the Import screen: per-file rows plus the aggregate summary. */
data class ImportUiState(
    val rows: List<ImportRowModel> = emptyList(),
    val summary: ImportSummary = ImportSummary(),
)

/** Combines the import queue with stored tickets and journeys to build [ImportUiState]. */
class ImportViewModel(
    private val importManager: ImportManager,
    ticketRepository: TicketRepository,
    observeJourneys: ObserveJourneysUseCase,
) : ViewModel() {
    val uiState: StateFlow<ImportUiState> =
        combine(
            importManager.items,
            ticketRepository.observeTickets(),
            observeJourneys.observeRefreshing(),
        ) { items, tickets, journeys ->
            ImportUiState(
                rows = ImportRowText.rows(items, tickets, journeys),
                summary = ImportRowText.summarize(items),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ImportUiState())

    /** Drops finished items from the queue; queued and reading items keep going. */
    fun clearFinished() = importManager.clearFinished()
}
