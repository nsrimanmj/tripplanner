package com.example.tripchecker.ui.edit

import com.example.tripchecker.ui.edit.StatusReadback.Result
import org.junit.Assert.assertEquals
import org.junit.Test

class StatusReadbackTest {
    private fun summary(raw: String?) = (StatusReadback.of(raw) as Result.Recognised).summary

    @Test
    fun `confirmed berth with type reads coach berth and lower-cased type`() {
        assertEquals("coach B4, berth 33, lower", summary("CNF/B4/33/LOWER"))
    }

    @Test
    fun `confirmed berth without type omits the type`() {
        assertEquals("coach S7, berth 12", summary("CNF/S7/12"))
    }

    @Test
    fun `confirmed berth with spaces and underscore type is normalised`() {
        assertEquals("coach B1, berth 5, side upper", summary("CNF / B1 / 5 / SIDE_UPPER"))
    }

    @Test
    fun `RAC with berth mentions RAC`() {
        assertEquals("RAC, coach S1, berth 70, side lower", summary("RAC/S1/70/SIDE LOWER"))
    }

    @Test
    fun `RAC queue position reads as position`() {
        assertEquals("RAC, position 5", summary("RAC/5"))
    }

    @Test
    fun `waitlist forms read as waitlist number`() {
        assertEquals("RLWL, waitlist 5", summary("RLWL/5"))
        assertEquals("GNWL, waitlist 12", summary("gnwl/12"))
        assertEquals("WL, waitlist 3", summary("WL/3"))
    }

    @Test
    fun `cancelled reads as cancelled`() {
        assertEquals("cancelled", summary("CAN"))
    }

    @Test
    fun `bare confirmed has no berth yet`() {
        assertEquals("confirmed, berth not assigned yet", summary("CNF"))
    }

    @Test
    fun `blank or null is empty`() {
        assertEquals(Result.Empty, StatusReadback.of(""))
        assertEquals(Result.Empty, StatusReadback.of("   "))
        assertEquals(Result.Empty, StatusReadback.of(null))
    }

    @Test
    fun `garbage is unrecognised`() {
        assertEquals(Result.Unrecognised, StatusReadback.of("hello world"))
        assertEquals(Result.Unrecognised, StatusReadback.of("CNF/B4"))
        assertEquals(Result.Unrecognised, StatusReadback.of("XYZ/12"))
    }
}
