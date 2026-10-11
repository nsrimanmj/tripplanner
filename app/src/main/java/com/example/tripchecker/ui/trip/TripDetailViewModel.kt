package com.example.tripchecker.ui.trip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tripchecker.data.repo.TicketRepository
import com.example.tripchecker.di.AppContainer
import com.example.tripchecker.domain.ObserveJourneysUseCase
import com.example.tripchecker.domain.model.JourneyGroup
import com.example.tripchecker.domain.model.Ticket
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class TripDetailUiState(
    val group: JourneyGroup? = null,
    val anchor: Ticket? = null,
)

class TripDetailViewModel(
    private val anchorTicketId: String,
    observeJourneys: ObserveJourneysUseCase,
    repository: TicketRepository,
) : ViewModel() {
    val uiState: StateFlow<TripDetailUiState> =
        combine(
            observeJourneys.observe(),
            repository.observeTicket(anchorTicketId),
        ) { groups, ticket ->
            val group = groups.firstOrNull { g -> g.legs.any { it.id == anchorTicketId } }
            TripDetailUiState(group = group, anchor = ticket)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TripDetailUiState())

    class Factory(
        private val container: AppContainer,
        private val anchorTicketId: String,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TripDetailViewModel::class.java)) {
                return TripDetailViewModel(
                    anchorTicketId,
                    container.observeJourneysUseCase,
                    container.ticketRepository,
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel $modelClass")
        }
    }
}
