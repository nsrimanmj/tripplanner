package com.example.tripchecker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tripchecker.data.repo.TicketRepository
import com.example.tripchecker.domain.ObserveJourneysUseCase
import com.example.tripchecker.domain.model.JourneyGroup
import com.example.tripchecker.domain.model.Ticket
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.time.Clock

/** UI state for the home journey list. */
data class HomeUiState(
    val isLoading: Boolean = true,
    val nextJourney: NextJourneyUi? = null,
    val needsReview: List<Ticket> = emptyList(),
    val upcoming: List<JourneyGroup> = emptyList(),
    val pastByMonth: List<PastMonthGroup> = emptyList(),
    val isEmpty: Boolean = false,
)

data class NextJourneyUi(
    val ticketId: String,
    val countdown: String,
    val trainLine: String,
    val route: String,
    val departureTime: String,
    val berthSummary: String,
    val pdfFileName: String? = null,
)

data class PastMonthGroup(
    val header: String,
    val groups: List<JourneyGroup>,
)

class HomeViewModel(
    observeJourneys: ObserveJourneysUseCase,
    private val repository: TicketRepository,
    private val clock: Clock,
) : ViewModel() {
    private val ticker =
        flow {
            while (true) {
                emit(clock.instant())
                delay(60_000)
            }
        }

    val uiState: StateFlow<HomeUiState> =
        combine(
            observeJourneys.observe(),
            repository.observeTickets(),
            ticker,
        ) { groups, tickets, now ->
            HomeUiMapper.map(groups, tickets, now)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}
