package com.example.tripchecker.extraction.parser

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class IrctcDatesTest {
    @Test
    fun `parses case-insensitive month`() {
        assertEquals(LocalDate.of(2026, 8, 3), IrctcDates.parseDate("03-aug-2026"))
    }

    @Test
    fun `parses single-digit day`() {
        assertEquals(LocalDate.of(2026, 6, 5), IrctcDates.parseDate("5-Jun-2026"))
    }

    @Test
    fun `converts departure to IST instant`() {
        val instant = IrctcDates.parseInstant("03-Aug-2026", "17:00")
        assertEquals(
            java.time.LocalTime.of(17, 0),
            instant.atZone(IrctcDates.IST).toLocalTime(),
        )
    }
}
