package com.example.tripchecker.ui.edit

import com.example.tripchecker.extraction.parser.TicketField
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

class EditTicketMapperTest {
    @Test
    fun `missing required fields detected`() {
        val missing = EditTicketMapper.missingFields(EditTicketForm())
        assertTrue(TicketField.PNR in missing)
        assertTrue(TicketField.TRAIN_NUMBER in missing)
    }

    @Test
    fun `complete form clears needs review`() {
        val form =
            EditTicketForm(
                pnr = "1234567890",
                trainNumber = "17001",
                fromCode = "SNSI",
                toCode = "BMT",
                departureDate = LocalDate.of(2026, 8, 3),
                departureTime = LocalTime.of(17, 0),
            )
        val ticket = EditTicketMapper.toTicket(form, existing = null, now = Instant.EPOCH)
        assertFalse(ticket.needsReview)
    }

    @Test
    fun `fare round trip preserves paise`() {
        assertEquals(12814L, EditTicketMapper.parseFarePaise("128.14"))
        assertEquals("128.14", EditTicketMapper.formatFareRupees(12814L))
        assertEquals(147000L, EditTicketMapper.parseFarePaise("1,470.00"))
    }
}
