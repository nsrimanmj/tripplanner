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
