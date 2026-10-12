package com.example.tripchecker.ui.home

import com.example.tripchecker.domain.grouping.TripGrouper
import com.example.tripchecker.domain.model.ExtractionMethod
import com.example.tripchecker.domain.model.Station
import com.example.tripchecker.domain.model.Ticket
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class HomeUiMapperTest {
    private val ist = ZoneId.of("Asia/Kolkata")

    @Test
    fun `needs review tickets excluded from upcoming and hero`() {
        val now = instant(2026, 8, 1, 12, 0)
        val good = ticket("g", needsReview = false, dep = instant(2026, 8, 10, 10, 0))
        val bad = ticket("b", needsReview = true, dep = instant(2026, 8, 5, 10, 0))
        val groups = TripGrouper.group(listOf(good, bad), now)
        val state = HomeUiMapper.map(groups, listOf(good, bad), now)
        assertEquals(1, state.needsReview.size)
        assertEquals("g", state.nextJourney?.ticketId)
        assertTrue(state.upcoming.all { group -> group.legs.none { it.id == "b" } })
    }

    @Test
    fun `empty when no tickets`() {
        val state = HomeUiMapper.map(emptyList(), emptyList(), Instant.EPOCH)
        assertTrue(state.isEmpty)
        assertNull(state.nextJourney)
    }

    private fun instant(y: Int, m: Int, d: Int, h: Int, min: Int): Instant =
        java.time.ZonedDateTime.of(y, m, d, h, min, 0, 0, ist).toInstant()

    private fun ticket(
        id: String,
        needsReview: Boolean,
        dep: Instant,
    ): Ticket =
        Ticket(
            id = id,
            pnr = "1234567890",
            trainNumber = "1",
            trainName = "EXP",
            from = Station("A", null),
            boarding = null,
            to = Station("B", null),
            startDate = null,
            departureAt = dep,
            arrivalAt = null,
            travelClass = "SL",
            classLabel = null,
            quota = "GN",
            quotaLabel = null,
            distanceKm = null,
            bookedAt = null,
            transactionId = null,
            totalFarePaise = null,
            passengers = emptyList(),
            pdfFileName = "$id.pdf",
            pdfSha256 = null,
            originalFileName = null,
            extractionMethod = ExtractionMethod.TEXT_LAYER,
            needsReview = needsReview,
            linkGroupId = null,
            autoPairDisabled = false,
            isCancelled = false,
            notes = null,
            createdAt = dep,
            updatedAt = dep,
        )
}
