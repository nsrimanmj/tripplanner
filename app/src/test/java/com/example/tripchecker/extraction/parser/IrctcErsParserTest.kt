package com.example.tripchecker.extraction.parser

import com.example.tripchecker.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class IrctcErsParserTest {
    private val parser = IrctcErsParser()
    private val ist = ZoneId.of("Asia/Kolkata")

    @Test
    fun `ers_sample_text_layer matches golden values`() {
        assertGolden(parser.parse(Fixtures.load("ers_sample_text_layer.txt")))
    }

    @Test
    fun `ers_sample_ocr_rows matches golden values`() {
        assertGolden(parser.parse(Fixtures.load("ers_sample_ocr_rows.txt")))
    }

    @Test
    fun `ers_multi_passenger expectations`() {
        val p = parser.parse(Fixtures.load("ers_multi_passenger.txt"))
        assertEquals("4523198760", p.pnr)
        assertEquals("17002", p.trainNumber)
        assertEquals("SC SNSI EXPRESS", p.trainName)
        assertEquals("SC", p.from?.code)
        assertEquals("LPI", p.boarding?.code)
        assertEquals("SNSI", p.to?.code)
        assertEquals(LocalDate.of(2026, 8, 10), p.startDate)
        assertEquals(
            "2026-08-10T21:40",
            p.departureAt!!.atZone(ist).toLocalDateTime().toString(),
        )
        assertNull(p.arrivalAt)
        assertFalse(p.missingFields.contains(TicketField.ARRIVAL))
        assertEquals("SL", p.travelClass)
        assertEquals("GN", p.quota)
        assertEquals(690, p.distanceKm)
        assertEquals(216360L, p.totalFarePaise)
        assertEquals(4, p.passengers.size)
    }

    @Test
    fun `missing pnr adds field without crash`() {
        val p = parser.parse("Electronic Reservation Slip\nTrain No./Name\nno digits here")
        assertTrue(p.missingFields.contains(TicketField.PNR))
    }

    @Test
    fun `empty string all missing no crash`() {
        val p = parser.parse("")
        assertTrue(p.missingFields.isNotEmpty())
    }

    @Test
    fun `canParse false for random text`() {
        assertFalse(parser.canParse("hello world"))
    }

    private fun assertGolden(p: ParsedTicket) {
        assertEquals("8751652729", p.pnr)
        assertEquals("17001", p.trainNumber)
        assertEquals("SNSI SC EXPRESS", p.trainName)
        assertEquals("SNSI", p.from?.code)
        assertEquals("SAINAGAR SHIRDI", p.from?.name)
        assertEquals("SNSI", p.boarding?.code)
        assertEquals("BMT", p.to?.code)
        assertEquals(LocalDate.of(2026, 8, 3), p.startDate)
        assertEquals(
            "2026-08-03T17:00",
            p.departureAt!!.atZone(ist).toLocalDateTime().toString(),
        )
        assertEquals(
            "2026-08-04T07:59",
            p.arrivalAt!!.atZone(ist).toLocalDateTime().toString(),
        )
        assertEquals("3A", p.travelClass)
        assertEquals("THIRD AC", p.classLabel)
        assertEquals("TQ", p.quota)
        assertEquals("TATKAL", p.quotaLabel)
        assertEquals(772, p.distanceKm)
        assertEquals("100006745112096", p.transactionId)
        assertEquals(149405L, p.totalFarePaise)
        assertEquals(1, p.passengers.size)
        assertEquals("MADHURI", p.passengers[0].name)
    }
}
