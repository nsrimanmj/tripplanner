package com.example.tripchecker.domain.grouping

import com.example.tripchecker.domain.model.JourneyGroup
import com.example.tripchecker.domain.model.JourneyKind
import com.example.tripchecker.domain.model.Ticket
import kotlin.math.abs

/** Picks other one-way tickets to manually link as a return leg. */
object LinkReturnCandidates {
    fun find(anchorLeg: Ticket, groups: List<JourneyGroup>): List<Ticket> {
        val oneWayLegs =
            groups
                .filter { it.kind == JourneyKind.ONE_WAY }
                .flatMap { it.legs }
        return oneWayLegs
            .filter { it.id != anchorLeg.id && !it.needsReview }
            .sortedWith(
                compareByDescending<Ticket> { isLikelyReturn(anchorLeg, it) }
                    .thenBy {
                        val a = anchorLeg.departureAt?.toEpochMilli() ?: 0L
                        val b = it.departureAt?.toEpochMilli() ?: 0L
                        abs(a - b)
                    },
            )
    }

    private fun isLikelyReturn(outbound: Ticket, candidate: Ticket): Boolean {
        val from = StationClusters.cityOf(outbound.origin?.code)
        val to = StationClusters.cityOf(outbound.to?.code)
        val cFrom = StationClusters.cityOf(candidate.origin?.code)
        val cTo = StationClusters.cityOf(candidate.to?.code)
        return from == cTo && to == cFrom
    }
}
