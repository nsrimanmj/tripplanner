package com.example.tripchecker.domain

import com.example.tripchecker.data.repo.TicketRepository
import com.example.tripchecker.domain.grouping.TripGrouper
import com.example.tripchecker.domain.model.JourneyGroup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Clock
import java.time.Instant

/** Observes tickets and maps them through [TripGrouper]. */
class ObserveJourneysUseCase(
    private val repository: TicketRepository,
    private val clock: Clock,
) {
    fun observeRefreshing(now: Flow<Instant> = journeyNowTicker(clock)): Flow<List<JourneyGroup>> =
        combine(repository.observeTickets(), now) { tickets, instant ->
            TripGrouper.group(tickets, instant)
        }
}
