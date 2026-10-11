package com.example.tripchecker.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class FormattersTest {
    private val ist = ZoneId.of("Asia/Kolkata")

    @Test
    fun `formats money en-IN`() {
        assertEquals("₹1,494.05", Formatters.formatMoneyPaise(149405))
    }

    @Test
    fun `formats duration`() {
        val start = Instant.parse("2026-08-03T11:30:00Z")
        val end = Instant.parse("2026-08-04T02:29:00Z")
        assertEquals("14h 59m", Formatters.formatDuration(start, end))
    }

    @Test
    fun `countdown today`() {
        val now = LocalDate.of(2026, 8, 3).atTime(10, 0).atZone(ist).toInstant()
        val dep = LocalDate.of(2026, 8, 3).atTime(17, 0).atZone(ist).toInstant()
        assertEquals("Today · 17:00", Formatters.formatCountdown(now, dep))
    }
}
