package com.example.tripchecker.ui.common

import com.example.tripchecker.domain.model.BerthStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class StatusToneTest {
    private fun kind(code: String?) = StatusTones.kindOf(code?.let { BerthStatus(code = it) })

    @Test
    fun `CNF is confirmed`() = assertEquals(StatusKind.CONFIRMED, kind("CNF"))

    @Test
    fun `RAC is rac`() = assertEquals(StatusKind.RAC, kind("RAC"))

    @Test
    fun `every waitlist code maps to waitlist`() {
        listOf("WL", "RLWL", "PQWL", "GNWL", "TQWL", "RSWL", "CKWL", "RLGNWL").forEach {
            assertEquals(it, StatusKind.WAITLIST, kind(it))
        }
    }

    @Test
    fun `CAN is cancelled`() = assertEquals(StatusKind.CANCELLED, kind("CAN"))

    @Test
    fun `null status is unknown`() = assertEquals(StatusKind.UNKNOWN, kind(null))

    @Test
    fun `unrecognised or blank code is unknown`() {
        assertEquals(StatusKind.UNKNOWN, kind("XYZ"))
        assertEquals(StatusKind.UNKNOWN, kind(""))
        assertEquals(StatusKind.UNKNOWN, kind("WLX"))
    }

    @Test
    fun `lower case and padded codes are normalised`() {
        assertEquals(StatusKind.CONFIRMED, kind(" cnf "))
        assertEquals(StatusKind.WAITLIST, kind("gnwl"))
    }

    @Test
    fun `tones follow the PLAN table`() {
        assertEquals(StatusTone.PRIMARY, StatusTones.toneOf(StatusKind.CONFIRMED))
        assertEquals(StatusTone.SECONDARY, StatusTones.toneOf(StatusKind.RAC))
        assertEquals(StatusTone.TERTIARY, StatusTones.toneOf(StatusKind.WAITLIST))
        assertEquals(StatusTone.ERROR, StatusTones.toneOf(StatusKind.CANCELLED))
        assertEquals(StatusTone.NEUTRAL, StatusTones.toneOf(StatusKind.UNKNOWN))
    }
}
