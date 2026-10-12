package com.example.tripchecker.ui.trip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tripchecker.data.repo.TicketRepository
import com.example.tripchecker.di.AppContainer
import com.example.tripchecker.domain.ObserveJourneysUseCase
import com.example.tripchecker.domain.grouping.LinkReturnCandidates
import com.example.tripchecker.domain.model.JourneyGroup
import com.example.tripchecker.domain.model.JourneyKind
import com.example.tripchecker.domain.model.Ticket
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class TripDetailUiState(
    val group: JourneyGroup? = null,
    val anchor: Ticket? = null,
    val linkCandidates: List<Ticket> = emptyList(),
)

sealed class TripDetailEvent {
    data class Message(val text: String) : TripDetailEvent()
    data object NavigateBack : TripDetailEvent()
}

/** ViewModel for trip detail: group actions and leg management. */
class TripDetailViewModel(
    private val anchorTicketId: String,
    observeJourneys: ObserveJourneysUseCase,
    private val repository: TicketRepository,
) : ViewModel() {
    val uiState: StateFlow<TripDetailUiState> =
        combine(
            observeJourneys.observe(),
            repository.observeTicket(anchorTicketId),
        ) { groups, ticket ->
            val group = groups.firstOrNull { g -> g.legs.any { it.id == anchorTicketId } }
            val anchorLeg = group?.legs?.find { it.id == anchorTicketId } ?: ticket
            val candidates =
                if (group?.kind == JourneyKind.ONE_WAY && anchorLeg != null) {
                    LinkReturnCandidates.find(anchorLeg, groups)
                } else {
                    emptyList()
                }
            TripDetailUiState(group = group, anchor = ticket, linkCandidates = candidates)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TripDetailUiState())

    private val _events = Channel<TripDetailEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun toggleCancelled(legId: String, cancelled: Boolean) {
        viewModelScope.launch { repository.setCancelled(legId, cancelled) }
    }

    fun deleteLeg(legId: String) {
        viewModelScope.launch {
            repository.delete(legId)
            if (legId == anchorTicketId) {
                _events.send(TripDetailEvent.NavigateBack)
            } else {
                _events.send(TripDetailEvent.Message("Ticket deleted"))
            }
        }
    }

    fun unlinkGroup() {
        val ids = uiState.value.group?.legs?.map { it.id } ?: return
        viewModelScope.launch {
            repository.unlink(ids)
            _events.send(TripDetailEvent.Message("Unlinked"))
        }
    }

    fun resetPairing() {
        val ids = uiState.value.group?.legs?.map { it.id } ?: return
        viewModelScope.launch {
            repository.resetPairing(ids)
            _events.send(TripDetailEvent.Message("Automatic pairing restored"))
        }
    }

    fun linkReturn(otherTicketId: String) {
        val anchor = uiState.value.anchor ?: return
        val groupId = UUID.randomUUID().toString()
        viewModelScope.launch {
            repository.link(listOf(anchor.id, otherTicketId), groupId)
            _events.send(TripDetailEvent.Message("Tickets linked"))
        }
    }

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
