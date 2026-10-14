package com.example.tripchecker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tripchecker.data.repo.TicketRepository
import com.example.tripchecker.domain.grouping.TripGrouper
import com.example.tripchecker.domain.journeyNowTicker
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock

/** Drives the home screen: groups tickets and maps them to [HomeUiState], re-evaluating "now" every minute. */
class HomeViewModel(
    private val repository: TicketRepository,
    private val clock: Clock,
) : ViewModel() {
    private val ticker = journeyNowTicker(clock)

    val uiState: StateFlow<HomeUiState> =
        combine(repository.observeTickets(), ticker) { tickets, now ->
            val groups = TripGrouper.group(tickets, now)
            HomeUiMapper.map(groups, tickets, now)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    /** Deletes a ticket and its PDF via the repository (same path as Trip detail delete). */
    fun deleteTicket(ticketId: String) {
        viewModelScope.launch {
            repository.delete(ticketId)
        }
    }
}
