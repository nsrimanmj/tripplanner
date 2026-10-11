package com.example.tripchecker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tripchecker.data.repo.TicketRepository
import com.example.tripchecker.domain.ObserveJourneysUseCase
import com.example.tripchecker.domain.model.JourneyGroup
import com.example.tripchecker.domain.model.JourneyStatus
import com.example.tripchecker.domain.model.Ticket
import com.example.tripchecker.ui.common.Formatters
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.Instant

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
            buildState(groups, tickets, now)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private fun buildState(
        groups: List<JourneyGroup>,
        tickets: List<Ticket>,
        now: Instant,
    ): HomeUiState {
        val needsReview = tickets.filter { it.needsReview }
        val reviewIds = needsReview.map { it.id }.toSet()
        val visibleGroups =
            groups.mapNotNull { group ->
                val legs = group.legs.filter { it.id !in reviewIds }
                if (legs.isEmpty()) null else group.copy(legs = legs)
            }
        val next =
            visibleGroups
                .flatMap { it.legs }
                .filter { it.departureAt != null && it.departureAt!! > now }
                .minByOrNull { it.departureAt!! }
                ?.let { leg ->
                    NextJourneyUi(
                        ticketId = leg.id,
                        countdown = Formatters.formatCountdown(now, leg.departureAt!!),
                        trainLine = listOfNotNull(leg.trainNumber, leg.trainName).joinToString(" "),
                        route = "${leg.origin?.code ?: "?"} → ${leg.to?.code ?: "?"}",
                        departureTime = Formatters.formatTime(leg.departureAt!!),
                        berthSummary = Formatters.formatBerthSummary(leg),
                    )
                }
        val upcoming =
            visibleGroups.filter {
                it.status == JourneyStatus.UPCOMING || it.status == JourneyStatus.IN_PROGRESS
            }
        val past =
            visibleGroups
                .filter { it.status == JourneyStatus.PAST }
                .groupBy { group ->
                    group.legs.first().departureAt?.atZone(com.example.tripchecker.extraction.parser.IrctcDates.IST)?.toLocalDate()
                        ?: java.time.LocalDate.EPOCH
                }
                .toList()
                .sortedByDescending { it.first }
                .map { (date, monthGroups) ->
                    PastMonthGroup(Formatters.formatMonthHeader(date), monthGroups)
                }
        val isEmpty = tickets.isEmpty()
        return HomeUiState(
            isLoading = false,
            nextJourney = next,
            needsReview = needsReview,
            upcoming = upcoming,
            pastByMonth = past,
            isEmpty = isEmpty,
        )
    }
}
