package com.example.tripchecker.domain.grouping

import com.example.tripchecker.domain.model.ExtractionMethod
import com.example.tripchecker.domain.model.JourneyKind
import com.example.tripchecker.domain.model.JourneyStatus
import com.example.tripchecker.domain.model.Passenger
import com.example.tripchecker.domain.model.Station
import com.example.tripchecker.domain.model.Ticket
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class TripGrouperTest {
    private val ist = ZoneId.of("Asia/Kolkata")

    @Test
    fun `pairs outbound with return within 14 days`() {
        val outbound = ticket("o", "SNSI", "BMT", instant(2026, 8, 3, 17, 0), instant(2026, 8, 4, 7, 59))
        val ret = ticket("r", "BMT", "SNSI", instant(2026, 8, 10, 21, 0))
        val groups = TripGrouper.group(listOf(outbound, ret), instant(2026, 8, 1, 0, 0))
        assertEquals(1, groups.size)
        assertEquals(JourneyKind.ROUND_TRIP, groups[0].kind)
    }

    @Test
    fun `return exactly 14 days later pairs`() {
        val outbound = ticket("o", "SNSI", "BMT", instant(2026, 8, 1, 10, 0))
        val ret = ticket("r", "BMT", "SNSI", instant(2026, 8, 15, 10, 0))
        val groups = TripGrouper.group(listOf(outbound, ret), instant(2026, 8, 1, 0, 0))
        assertEquals(1, groups.size)
        assertEquals(JourneyKind.ROUND_TRIP, groups[0].kind)
    }

    @Test
    fun `return 14 days and one minute later stays one way`() {
        val outbound = ticket("o", "SNSI", "BMT", instant(2026, 8, 1, 10, 0))
        val ret = ticket("r", "BMT", "SNSI", instant(2026, 8, 15, 10, 1))
        val groups = TripGrouper.group(listOf(outbound, ret), instant(2026, 8, 1, 0, 0))
        assertEquals(2, groups.count { it.kind == JourneyKind.ONE_WAY })
    }

    @Test
    fun `return beyond 14 days stays one way`() {
        val outbound = ticket("o", "SNSI", "BMT", instant(2026, 8, 3, 17, 0))
        val ret = ticket("r", "BMT", "SNSI", instant(2026, 8, 20, 21, 0))
        val groups = TripGrouper.group(listOf(outbound, ret), instant(2026, 8, 1, 0, 0))
        assertEquals(2, groups.count { it.kind == JourneyKind.ONE_WAY })
    }

    @Test
    fun `alternating legs pair in order not cross`() {
        val a1 = ticket("1", "A", "B", instant(2026, 8, 1, 10, 0))
        val b1 = ticket("2", "B", "A", instant(2026, 8, 8, 10, 0))
        val a2 = ticket("3", "A", "B", instant(2026, 8, 15, 10, 0))
        val b2 = ticket("4", "B", "A", instant(2026, 8, 22, 10, 0))
        val groups = TripGrouper.group(listOf(a1, b1, a2, b2), instant(2026, 7, 1, 0, 0))
        val roundTrips = groups.filter { it.kind == JourneyKind.ROUND_TRIP }
        assertEquals(2, roundTrips.size)
        assertTrue(roundTrips.any { it.legs.map { it.id } == listOf("1", "2") })
        assertTrue(roundTrips.any { it.legs.map { it.id } == listOf("3", "4") })
    }

    @Test
    fun `cluster SC with BMT return`() {
        val outbound = ticket("o", "SNSI", "BMT", instant(2026, 8, 3, 17, 0))
        val ret = ticket("r", "SC", "SNSI", instant(2026, 8, 8, 21, 0))
        val groups = TripGrouper.group(listOf(outbound, ret), instant(2026, 8, 1, 0, 0))
        assertEquals(JourneyKind.ROUND_TRIP, groups.single().kind)
    }

    @Test
    fun `list order does not change pairing`() {
        val outbound = ticket("o", "SNSI", "BMT", instant(2026, 8, 3, 17, 0))
        val ret = ticket("r", "BMT", "SNSI", instant(2026, 8, 10, 21, 0))
        val a = TripGrouper.group(listOf(outbound, ret), instant(2026, 8, 1, 0, 0))
        val b = TripGrouper.group(listOf(ret, outbound), instant(2026, 8, 1, 0, 0))
        assertEquals(a.map { it.legs.map { it.id } }, b.map { it.legs.map { it.id } })
    }

    @Test
    fun `chooses earliest return candidate`() {
        val outbound = ticket("o", "SNSI", "BMT", instant(2026, 8, 3, 17, 0))
        val r1 = ticket("r1", "BMT", "SNSI", instant(2026, 8, 6, 21, 0))
        val r2 = ticket("r2", "BMT", "SNSI", instant(2026, 8, 9, 21, 0))
        val groups = TripGrouper.group(listOf(outbound, r1, r2), instant(2026, 8, 1, 0, 0))
        val round = groups.single { it.kind == JourneyKind.ROUND_TRIP }
        assertEquals(listOf("o", "r1"), round.legs.map { it.id })
        assertTrue(groups.any { it.kind == JourneyKind.ONE_WAY && it.legs.single().id == "r2" })
    }

    @Test
    fun `return before outbound arrival not paired`() {
        val outbound = ticket("o", "SNSI", "BMT", instant(2026, 8, 3, 17, 0), instant(2026, 8, 4, 7, 59))
        val ret = ticket("r", "BMT", "SNSI", instant(2026, 8, 3, 20, 0))
        val groups = TripGrouper.group(listOf(outbound, ret), instant(2026, 8, 1, 0, 0))
        assertEquals(2, groups.size)
    }

    @Test
    fun `needsReview cancelled and autoPairDisabled are not paired`() {
        val outbound = ticket("o", "SNSI", "BMT", instant(2026, 8, 3, 17, 0))
        val ret = ticket("r", "BMT", "SNSI", instant(2026, 8, 10, 21, 0), needsReview = true)
        val groups = TripGrouper.group(listOf(outbound, ret), instant(2026, 8, 1, 0, 0))
        assertTrue(groups.all { it.kind == JourneyKind.ONE_WAY })
    }

    @Test
    fun `manual link group of three is custom`() {
        val t1 = ticket("1", "SNSI", "BMT", instant(2026, 8, 1, 10, 0), linkGroupId = "g1")
        val t2 = ticket("2", "BMT", "SNSI", instant(2026, 8, 15, 10, 0), linkGroupId = "g1")
        val t3 = ticket("3", "SNSI", "BMT", instant(2026, 9, 1, 10, 0), linkGroupId = "g1")
        val groups = TripGrouper.group(listOf(t1, t2, t3), instant(2026, 7, 1, 0, 0))
        assertEquals(JourneyKind.CUSTOM, groups.single().kind)
        assertTrue(groups.single().isManual)
    }

    @Test
    fun `boarding station used for matching`() {
        val outbound =
            ticket(
                "o",
                from = "SC",
                to = "SNSI",
                departure = instant(2026, 8, 3, 17, 0),
                boarding = "LPI",
            )
        val ret = ticket("r", "SNSI", "BMT", instant(2026, 8, 10, 21, 0))
        val groups = TripGrouper.group(listOf(outbound, ret), instant(2026, 8, 1, 0, 0))
        assertEquals(JourneyKind.ROUND_TRIP, groups.single().kind)
    }

    @Test
    fun `status upcoming in progress past`() {
        val outbound = ticket("o", "SNSI", "BMT", instant(2026, 8, 3, 17, 0), instant(2026, 8, 4, 7, 59))
        val ret = ticket("r", "BMT", "SNSI", instant(2026, 8, 10, 21, 0), instant(2026, 8, 11, 8, 0))
        val legs = listOf(outbound, ret)
        val before = TripGrouper.group(legs, instant(2026, 8, 1, 0, 0)).single()
        val between = TripGrouper.group(legs, instant(2026, 8, 5, 0, 0)).single()
        val after = TripGrouper.group(legs, instant(2026, 8, 12, 0, 0)).single()
        assertEquals(JourneyStatus.UPCOMING, before.status)
        assertEquals(JourneyStatus.IN_PROGRESS, between.status)
        assertEquals(JourneyStatus.PAST, after.status)
    }

    @Test
    fun `sorts upcoming ascending and past descending`() {
        val pastLate = ticket("p2", "SNSI", "BMT", instant(2026, 6, 10, 10, 0))
        val pastEarly = ticket("p1", "SNSI", "BMT", instant(2026, 6, 1, 10, 0))
        val futureLate = ticket("f2", "SNSI", "BMT", instant(2026, 12, 10, 10, 0))
        val futureEarly = ticket("f1", "SNSI", "BMT", instant(2026, 12, 1, 10, 0))
        val now = instant(2026, 8, 1, 0, 0)
        val groups = TripGrouper.group(listOf(pastLate, pastEarly, futureLate, futureEarly), now)
        assertEquals(listOf("f1", "f2", "p2", "p1"), groups.map { it.legs.single().id })
    }

    private fun instant(y: Int, m: Int, d: Int, h: Int, min: Int, sec: Int = 0): Instant =
        java.time.ZonedDateTime.of(y, m, d, h, min, sec, 0, ist).toInstant()

    private fun ticket(
        id: String,
        from: String,
        to: String,
        departure: Instant,
        arrival: Instant? = null,
        boarding: String? = null,
        needsReview: Boolean = false,
        linkGroupId: String? = null,
    ): Ticket {
        val now = Instant.EPOCH
        return Ticket(
            id = id,
            pnr = null,
            trainNumber = null,
            trainName = null,
            from = Station(from, from),
            boarding = boarding?.let { Station(it, it) },
            to = Station(to, to),
            startDate = null,
            departureAt = departure,
            arrivalAt = arrival,
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
            needsReview = needsReview,
            linkGroupId = linkGroupId,
            autoPairDisabled = false,
            isCancelled = false,
            notes = null,
            createdAt = now,
            updatedAt = now,
        )
    }
}
