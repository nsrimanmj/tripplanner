package com.example.tripchecker.domain.text

import com.example.tripchecker.domain.model.ExtractionMethod
import com.example.tripchecker.domain.model.Station
import com.example.tripchecker.domain.model.Ticket
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class DisplayTextTest {
    @Test
    fun `title-cases plain all caps words`() {
        assertEquals("Sainagar Shirdi", DisplayText.titleCase("SAINAGAR SHIRDI"))
        assertEquals("Madhuri", DisplayText.titleCase("MADHURI"))
    }

    @Test
    fun `keeps words in keepUpper uppercase`() {
        assertEquals("SNSI SC Express", DisplayText.titleCase("SNSI SC EXPRESS", setOf("SNSI", "SC")))
    }

    @Test
    fun `keeps words in keepUpper regardless of case of the set`() {
        assertEquals("ABCD Express", DisplayText.titleCase("ABCD EXPRESS", setOf("abcd")))
    }

    @Test
    fun `keeps built in abbreviations uppercase`() {
        assertEquals("Secunderabad JN", DisplayText.titleCase("SECUNDERABAD JN"))
        assertEquals("Third AC", DisplayText.titleCase("THIRD AC"))
        assertEquals("Sleeper SL", DisplayText.titleCase("SLEEPER SL"))
        assertEquals("Superfast SF Chair CC Exec EC", DisplayText.titleCase("SUPERFAST SF CHAIR CC EXEC EC"))
        assertEquals("CNF RAC", DisplayText.titleCase("CNF RAC"))
    }

    @Test
    fun `keeps words of two letters or fewer uppercase`() {
        assertEquals("AB CD", DisplayText.titleCase("AB CD"))
        assertEquals("X Yard", DisplayText.titleCase("X YARD"))
    }

    @Test
    fun `keeps words containing a digit uppercase`() {
        assertEquals("Coach B3 Seat 12A", DisplayText.titleCase("COACH B3 SEAT 12A"))
        assertEquals("Train 17001", DisplayText.titleCase("TRAIN 17001"))
    }

    @Test
    fun `known cluster station codes stay uppercase`() {
        assertEquals("HYB Express", DisplayText.titleCase("HYB EXPRESS"))
        assertEquals("CSMT Terminus", DisplayText.titleCase("CSMT TERMINUS"))
    }

    @Test
    fun `null empty and blank input give empty string`() {
        assertEquals("", DisplayText.titleCase(null))
        assertEquals("", DisplayText.titleCase(""))
        assertEquals("", DisplayText.titleCase("   "))
    }

    @Test
    fun `preserves spacing and punctuation`() {
        assertEquals("Sri Sai-Nagar (Shirdi)", DisplayText.titleCase("SRI SAI-NAGAR (SHIRDI)"))
        assertEquals("King's Road", DisplayText.titleCase("KING'S ROAD"))
    }

    @Test
    fun `lower and mixed case input is normalised`() {
        assertEquals("Sainagar Shirdi", DisplayText.titleCase("sainagar sHIRDI"))
    }

    @Test
    fun `ticket station codes are kept uppercase via forTicket`() {
        val t = ticket(from = Station("ZZTOP", "ZZTOP JN"), to = Station("ABCD", null))
        assertEquals("ZZTOP Junction ABCD", DisplayText.forTicket("ZZTOP JUNCTION ABCD", t))
        assertEquals("Zztop Junction Abcd", DisplayText.titleCase("ZZTOP JUNCTION ABCD"))
    }

    @Test
    fun `keepUpperFor collects from boarding and to codes`() {
        val t = ticket(from = Station("aa1", null), boarding = Station("BB2", null), to = Station("cc3", null))
        assertEquals(setOf("AA1", "BB2", "CC3"), DisplayText.keepUpperFor(t))
    }

    private fun ticket(
        from: Station? = null,
        boarding: Station? = null,
        to: Station? = null,
    ) = Ticket(
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
