package com.example.tripchecker.domain.grouping

import com.example.tripchecker.domain.model.JourneyGroup
import com.example.tripchecker.domain.model.JourneyKind
import com.example.tripchecker.domain.model.JourneyStatus
import com.example.tripchecker.domain.model.Ticket
import java.time.Duration
import java.time.Instant

/** Derives journey cards from tickets (not persisted). */
object TripGrouper {
    const val PAIRING_WINDOW_DAYS: Long = 14

    fun group(tickets: List<Ticket>, now: Instant): List<JourneyGroup> {
        val manual = buildManualGroups(tickets, now)
        val manualIds = manual.flatMap { it.legs }.map { it.id }.toSet()
        val remaining = tickets.filter { it.id !in manualIds }
        val singles = mutableListOf<JourneyGroup>()
        val autoPairs = mutableListOf<JourneyGroup>()
        val paired = mutableSetOf<String>()
        val autoEligible =
            remaining.filter { ticket ->
                !ticket.needsReview &&
                    !ticket.isCancelled &&
                    !ticket.autoPairDisabled &&
                    ticket.origin != null &&
                    ticket.to != null &&
                    ticket.departureAt != null
            }
        val sorted = autoEligible.sortedWith(compareBy({ it.departureAt }, { it.id }))
        for (outbound in sorted) {
            if (outbound.id in paired) continue
            val originCity = StationClusters.cityOf(outbound.origin!!.code)
            val destCity = StationClusters.cityOf(outbound.to!!.code)
            val outboundEnd = outbound.arrivalAt ?: outbound.departureAt!!
            val candidates =
                sorted.filter { candidate ->
                    candidate.id !in paired &&
                        candidate.id != outbound.id &&
                        StationClusters.cityOf(candidate.origin?.code) == destCity &&
                        StationClusters.cityOf(candidate.to?.code) == originCity &&
                        !candidate.departureAt!!.isBefore(outboundEnd) &&
                        Duration.between(outbound.departureAt, candidate.departureAt).toDays() <= PAIRING_WINDOW_DAYS
                }
            val chosen = candidates.minWithOrNull(compareBy({ it.departureAt }, { it.id }))
            if (chosen != null) {
                paired.add(outbound.id)
                paired.add(chosen.id)
                val legs = listOf(outbound, chosen).sortedBy { it.departureAt }
                autoPairs.add(
                    JourneyGroup(
                        key = "auto:${outbound.id}",
                        kind = JourneyKind.ROUND_TRIP,
                        legs = legs,
                        isManual = false,
                        status = statusFor(legs, now),
                    ),
                )
            }
        }
        for (ticket in remaining) {
            if (ticket.id in paired) continue
            singles.add(singleGroup(ticket, now))
        }
        val all = manual + autoPairs + singles
        return sortGroups(all)
    }

    private fun buildManualGroups(tickets: List<Ticket>, now: Instant): List<JourneyGroup> {
        return tickets
            .filter { it.linkGroupId != null }
            .groupBy { it.linkGroupId!! }
            .map { (groupId, legs) ->
                val sorted = legs.sortedBy { it.departureAt ?: Instant.EPOCH }
                val kind =
                    when (sorted.size) {
                        2 -> JourneyKind.ROUND_TRIP
                        else -> JourneyKind.CUSTOM
                    }
                JourneyGroup(
                    key = "manual:$groupId",
                    kind = kind,
                    legs = sorted,
                    isManual = true,
                    status = statusFor(sorted, now),
                )
            }
    }

    private fun singleGroup(ticket: Ticket, now: Instant): JourneyGroup =
        JourneyGroup(
            key = "single:${ticket.id}",
            kind = JourneyKind.ONE_WAY,
            legs = listOf(ticket),
            isManual = false,
            status = statusFor(listOf(ticket), now),
        )

    private fun statusFor(legs: List<Ticket>, now: Instant): JourneyStatus {
        val first = legs.first().departureAt ?: return JourneyStatus.PAST
        val lastLeg = legs.last()
        val last = lastLeg.arrivalAt ?: lastLeg.departureAt ?: first
        return when {
            now.isBefore(first) -> JourneyStatus.UPCOMING
            now.isAfter(last) -> JourneyStatus.PAST
            else -> JourneyStatus.IN_PROGRESS
        }
    }

    private fun sortGroups(groups: List<JourneyGroup>): List<JourneyGroup> {
        val upcoming = groups.filter { it.status != JourneyStatus.PAST }
        val past = groups.filter { it.status == JourneyStatus.PAST }
        val sortedUpcoming =
            upcoming.sortedWith(
                compareBy<JourneyGroup> { it.legs.first().departureAt ?: Instant.EPOCH }
                    .thenBy { it.key },
            )
        val sortedPast =
            past.sortedWith(
                compareByDescending<JourneyGroup> { it.legs.first().departureAt ?: Instant.EPOCH }
                    .thenBy { it.key },
            )
        return sortedUpcoming + sortedPast
    }
}
