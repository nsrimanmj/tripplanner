package com.example.tripchecker.extraction.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PassengerStatusParserTest {
    @Test fun `CNF B4 33 LOWER`() = assertBerth("CNF/B4/33/LOWER", "CNF", "B4", 33, "LOWER")
    @Test fun `CNF spaced slash`() = assertBerth("CNF /B4/33/LOWER", "CNF", "B4", 33, "LOWER")
    @Test fun `SIDE UPPER`() = assertBerth("CNF/S7/49/SIDE UPPER", "CNF", "S7", 49, "SIDE UPPER")
    @Test fun `WINDOW SIDE`() = assertBerth("CNF/C1/45/WINDOW SIDE", "CNF", "C1", 45, "WINDOW SIDE")
    @Test fun `no berth type`() = assertBerth("CNF/B4/33", "CNF", "B4", 33, null)
    @Test fun `RAC side lower`() = assertBerth("RAC/S2/71/SIDE LOWER", "RAC", "S2", 71, "SIDE LOWER")
    @Test fun `RAC number only`() {
        val s = PassengerStatusParser.parse("RAC/23")!!
        assertEquals("RAC", s.code)
        assertEquals(23, s.waitlistNumber)
    }
    @Test fun `RLWL waitlist`() {
        val s = PassengerStatusParser.parse("RLWL/12")!!
        assertEquals("RLWL", s.code)
        assertEquals(12, s.waitlistNumber)
    }
    @Test fun `WL waitlist`() {
        val s = PassengerStatusParser.parse("WL/40")!!
        assertEquals("WL", s.code)
        assertEquals(40, s.waitlistNumber)
    }
    @Test fun `PQWL waitlist`() {
        val s = PassengerStatusParser.parse("PQWL/7")!!
        assertEquals(7, s.waitlistNumber)
    }
    @Test fun `GNWL waitlist`() {
        val s = PassengerStatusParser.parse("GNWL/105")!!
        assertEquals(105, s.waitlistNumber)
    }
    @Test fun `TQWL waitlist`() {
        val s = PassengerStatusParser.parse("TQWL/3")!!
        assertEquals(3, s.waitlistNumber)
    }
    @Test fun `CAN`() {
        assertEquals("CAN", PassengerStatusParser.parse("CAN")!!.code)
    }
    @Test fun `null raw`() {
        assertNull(PassengerStatusParser.parse(null))
        assertNull(PassengerStatusParser.parse(""))
    }

    private fun assertBerth(
        raw: String,
        code: String,
        coach: String,
        berth: Int,
        type: String?,
    ) {
        val s = PassengerStatusParser.parse(raw)!!
        assertEquals(code, s.code)
        assertEquals(coach, s.coach)
        assertEquals(berth, s.berthNumber)
        assertEquals(type, s.berthType)
    }
}
