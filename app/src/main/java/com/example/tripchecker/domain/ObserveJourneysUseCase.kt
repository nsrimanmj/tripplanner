package com.example.tripchecker.domain

import com.example.tripchecker.data.repo.TicketRepository
import com.example.tripchecker.domain.grouping.TripGrouper
import com.example.tripchecker.domain.model.JourneyGroup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock

/** Observes tickets and maps them through [TripGrouper]. */
class ObserveJourneysUseCase(
    private val repository: TicketRepository,
    private val clock: Clock,
) {
    fun observe(): Flow<List<JourneyGroup>> =
        repository.observeTickets().map { tickets ->
            TripGrouper.group(tickets, clock.instant())
        }
}
