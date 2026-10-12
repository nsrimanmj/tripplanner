package com.example.tripchecker.ui.home

import com.example.tripchecker.domain.model.JourneyGroup
import com.example.tripchecker.domain.model.JourneyStatus
import com.example.tripchecker.domain.model.Ticket
import com.example.tripchecker.extraction.parser.IrctcDates
import com.example.tripchecker.ui.common.Formatters
import java.time.Instant
import java.time.LocalDate

/** Pure mapping from domain journeys to [HomeUiState]. */
object HomeUiMapper {
    fun map(
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
                        pdfFileName = leg.pdfFileName,
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
                    group.legs.first().departureAt?.atZone(IrctcDates.IST)?.toLocalDate()
                        ?: LocalDate.EPOCH
                }
                .toList()
                .sortedByDescending { it.first }
                .map { (date, monthGroups) ->
                    PastMonthGroup(Formatters.formatMonthHeader(date), monthGroups)
                }
        return HomeUiState(
            isLoading = false,
            nextJourney = next,
            needsReview = needsReview,
            upcoming = upcoming,
            pastByMonth = past,
            isEmpty = tickets.isEmpty(),
        )
    }
}
