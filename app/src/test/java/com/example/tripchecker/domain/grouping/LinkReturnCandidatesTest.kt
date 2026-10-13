package com.example.tripchecker.domain.grouping

import com.example.tripchecker.domain.model.ExtractionMethod
import com.example.tripchecker.domain.model.JourneyKind
import com.example.tripchecker.domain.model.JourneyGroup
import com.example.tripchecker.domain.model.JourneyStatus
import com.example.tripchecker.domain.model.Station
import com.example.tripchecker.domain.model.Ticket
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class LinkReturnCandidatesTest {
    @Test
    fun `prefers reverse city route`() {
        val anchor = ticket("a", "SNSI", "BMT", t(2026, 8, 3))
        val likely = ticket("r", "BMT", "SNSI", t(2026, 8, 10))
        val other = ticket("o", "SNSI", "SC", t(2026, 8, 9))
        val groups =
            listOf(
                oneWay("a", anchor),
                oneWay("r", likely),
                oneWay("o", other),
            )
        val result = LinkReturnCandidates.find(anchor, groups)
        assertEquals("r", result.first().id)
    }

    @Test
    fun `excludes cancelled and needs-review tickets and the anchor`() {
        val anchor = ticket("a", "SNSI", "BMT", t(2026, 8, 3))
        val cancelled = ticket("c", "BMT", "SNSI", t(2026, 8, 10)).copy(isCancelled = true)
        val review = ticket("v", "BMT", "SNSI", t(2026, 8, 11)).copy(needsReview = true)
        val ok = ticket("k", "BMT", "SNSI", t(2026, 8, 12))
        val groups = listOf(anchor, cancelled, review, ok).map { oneWay(it.id, it) }
        assertEquals(listOf("k"), LinkReturnCandidates.find(anchor, groups).map { it.id })
    }

    @Test
    fun `ignores tickets that are part of round trips`() {
        val anchor = ticket("a", "SNSI", "BMT", t(2026, 8, 3))
        val paired = ticket("p", "BMT", "SNSI", t(2026, 8, 10))
        val groups =
            listOf(
                oneWay("a", anchor),
                JourneyGroup("rt", JourneyKind.ROUND_TRIP, listOf(paired), false, JourneyStatus.UPCOMING),
            )
        assertEquals(emptyList<String>(), LinkReturnCandidates.find(anchor, groups).map { it.id })
    }

    @Test
    fun `likely returns come first then the rest, each by closeness in time`() {
        val anchor = ticket("a", "SNSI", "BMT", t(2026, 8, 3))
        val likelyFar = ticket("lf", "SC", "SNSI", t(2026, 8, 20))
        val likelyNear = ticket("ln", "BMT", "SNSI", t(2026, 8, 10))
        val otherNear = ticket("on", "SNSI", "SC", t(2026, 8, 4))
        val otherFar = ticket("of", "SNSI", "SC", t(2026, 9, 4))
        val groups = listOf(anchor, otherFar, likelyFar, otherNear, likelyNear).map { oneWay(it.id, it) }
        val ranked = LinkReturnCandidates.findRanked(anchor, groups)
        assertEquals(listOf("ln", "lf", "on", "of"), ranked.map { it.ticket.id })
        assertEquals(listOf(true, true, false, false), ranked.map { it.likelyReturn })
    }

    private fun oneWay(key: String, leg: Ticket) =
        JourneyGroup(key, JourneyKind.ONE_WAY, listOf(leg), isManual = false, JourneyStatus.UPCOMING)

    private fun t(y: Int, m: Int, d: Int): Instant =
        java.time.ZonedDateTime.of(y, m, d, 12, 0, 0, 0, ZoneId.of("Asia/Kolkata")).toInstant()

    private fun ticket(id: String, from: String, to: String, dep: Instant): Ticket =
        Ticket(
            id = id,
            pnr = "1",
            trainNumber = "1",
            trainName = null,
            from = Station(from, null),
            boarding = null,
            to = Station(to, null),
            startDate = null,
            departureAt = dep,
            arrivalAt = null,
            travelClass = null,
            classLabel = null,
            quota = null,
            quotaLabel = null,
            distanceKm = null,
            bookedAt = null,
            transactionId = null,
            totalFarePaise = null,
            passengers = emptyList(),
            pdfFileName = null,
            pdfSha256 = null,
            originalFileName = null,
            extractionMethod = ExtractionMethod.MANUAL,
            needsReview = false,
            linkGroupId = null,
            autoPairDisabled = false,
            isCancelled = false,
            notes = null,
            createdAt = dep,
            updatedAt = dep,
        )
}
