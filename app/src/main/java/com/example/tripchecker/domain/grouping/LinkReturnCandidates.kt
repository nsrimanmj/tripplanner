package com.example.tripchecker.domain.grouping

import com.example.tripchecker.domain.model.JourneyGroup
import com.example.tripchecker.domain.model.JourneyKind
import com.example.tripchecker.domain.model.Ticket
import kotlin.math.abs

/** A ticket that can be manually linked as a return leg, with whether it is a "likely return". */
data class ReturnCandidate(val ticket: Ticket, val likelyReturn: Boolean)

/** Picks other one-way tickets to manually link as a return leg. */
object LinkReturnCandidates {
    /** Candidate tickets only, in the order of [findRanked]. */
    fun find(anchorLeg: Ticket, groups: List<JourneyGroup>): List<Ticket> =
        findRanked(anchorLeg, groups).map { it.ticket }

    /**
     * Other ONE_WAY tickets that are neither cancelled nor awaiting review. Likely returns (reverse
     * city route) come first, then the rest; each block is sorted by closeness in time to [anchorLeg].
     */
    fun findRanked(anchorLeg: Ticket, groups: List<JourneyGroup>): List<ReturnCandidate> {
        val anchorMillis = anchorLeg.departureAt?.toEpochMilli() ?: 0L
        return groups
            .filter { it.kind == JourneyKind.ONE_WAY }
            .flatMap { it.legs }
            .filter { it.id != anchorLeg.id && !it.needsReview && !it.isCancelled }
            .map { ReturnCandidate(it, isLikelyReturn(anchorLeg, it)) }
            .sortedWith(
                compareByDescending<ReturnCandidate> { it.likelyReturn }
                    .thenBy { abs(anchorMillis - (it.ticket.departureAt?.toEpochMilli() ?: 0L)) },
            )
    }

    private fun isLikelyReturn(outbound: Ticket, candidate: Ticket): Boolean {
        val from = StationClusters.cityOf(outbound.origin?.code)
        val to = StationClusters.cityOf(outbound.to?.code)
        val cFrom = StationClusters.cityOf(candidate.origin?.code)
        val cTo = StationClusters.cityOf(candidate.to?.code)
        return from.isNotBlank() && to.isNotBlank() && from == cTo && to == cFrom
    }
}
