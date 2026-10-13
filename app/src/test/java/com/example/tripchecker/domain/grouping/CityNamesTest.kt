package com.example.tripchecker.domain.grouping

import com.example.tripchecker.domain.model.ExtractionMethod
import com.example.tripchecker.domain.model.JourneyGroup
import com.example.tripchecker.domain.model.JourneyKind
import com.example.tripchecker.domain.model.JourneyStatus
import com.example.tripchecker.domain.model.Station
import com.example.tripchecker.domain.model.Ticket
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class CityNamesTest {
    private val shirdi = Station("SNSI", "SAINAGAR SHIRDI")
    private val hyd = Station("SC", "SECUNDERABAD JN")
    private val blr = Station("SBC", "KSR BENGALURU")
    private val mas = Station("MAS", "CHENNAI CENTRAL")

    @Test
    fun `cityLabel uses the cluster city in title case`() {
        assertEquals("Hyderabad", CityNames.cityLabel(hyd))
        assertEquals("Bengaluru", CityNames.cityLabel(blr))
        assertEquals("Shirdi", CityNames.cityLabel(shirdi))
    }

    @Test
    fun `cityLabel uses the cluster city when the code equals the city key`() {
        assertEquals("Pune", CityNames.cityLabel(Station("PUNE", "PUNE JN")))
    }

    @Test
    fun `cityLabel is case insensitive on the code`() {
        assertEquals("Hyderabad", CityNames.cityLabel(Station("sc", null)))
    }

    @Test
    fun `cityLabel falls back to title cased name for unknown codes`() {
        assertEquals("Nagpur JN", CityNames.cityLabel(Station("NGP", "NAGPUR JN")))
        assertEquals("Wardha", CityNames.cityLabel(Station("WR", "WARDHA")))
    }

    @Test
    fun `cityLabel falls back to the code when name is blank or null`() {
        assertEquals("NGP", CityNames.cityLabel(Station("NGP", null)))
        assertEquals("NGP", CityNames.cityLabel(Station("NGP", "  ")))
    }

    @Test
    fun `cityLabel of null station is null`() {
        assertNull(CityNames.cityLabel(null))
    }

    @Test
    fun `round trip title uses a double arrow`() {
        val g = group(JourneyKind.ROUND_TRIP, leg(shirdi, hyd), leg(hyd, shirdi))
        assertEquals("Shirdi ⇄ Hyderabad", CityNames.routeTitle(g))
    }

    @Test
    fun `one way title uses a single arrow`() {
        val g = group(JourneyKind.ONE_WAY, leg(shirdi, hyd))
        assertEquals("Shirdi → Hyderabad", CityNames.routeTitle(g))
    }

    @Test
    fun `custom title chains cities and collapses consecutive duplicates`() {
        val g = group(JourneyKind.CUSTOM, leg(shirdi, hyd), leg(hyd, blr), leg(blr, mas))
        assertEquals("Shirdi → Hyderabad → Bengaluru → Chennai", CityNames.routeTitle(g))
    }

    @Test
    fun `custom title collapses clustered stations of the same city`() {
        val g = group(JourneyKind.CUSTOM, leg(shirdi, Station("HYB", null)), leg(hyd, blr))
        assertEquals("Shirdi → Hyderabad → Bengaluru", CityNames.routeTitle(g))
    }

    @Test
    fun `origin prefers boarding over from`() {
        val g = group(JourneyKind.ONE_WAY, leg(from = hyd, boarding = shirdi, to = blr))
        assertEquals("Shirdi → Bengaluru", CityNames.routeTitle(g))
    }

    @Test
    fun `empty group gives empty title`() {
        assertEquals("", CityNames.routeTitle(group(JourneyKind.CUSTOM)))
    }

    private fun group(kind: JourneyKind, vararg legs: Ticket) =
        JourneyGroup("k", kind, legs.toList(), isManual = false, status = JourneyStatus.UPCOMING)

    private fun leg(from: Station?, to: Station?) = leg(from, null, to)

    private fun leg(from: Station?, boarding: Station?, to: Station?) = Ticket(
        id = "t", pnr = null, trainNumber = null, trainName = null,
        from = from, boarding = boarding, to = to,
        startDate = null, departureAt = null, arrivalAt = null,
        travelClass = null, classLabel = null, quota = null, quotaLabel = null,
        distanceKm = null, bookedAt = null, transactionId = null, totalFarePaise = null,
        passengers = emptyList(), pdfFileName = null, pdfSha256 = null, originalFileName = null,
        extractionMethod = ExtractionMethod.MANUAL, needsReview = false,
        linkGroupId = null, autoPairDisabled = false, isCancelled = false, notes = null,
        createdAt = Instant.EPOCH, updatedAt = Instant.EPOCH,
    )
}
