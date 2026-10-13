package com.example.tripchecker.ui.edit

import com.example.tripchecker.extraction.parser.TicketField
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class EditTicketUiStateTest {
    @Test
    fun `fresh state is not dirty and editing a field makes it dirty`() {
        val state = EditTicketUiState(isLoading = false)
        assertFalse(state.isDirty)
        assertTrue(state.copy(form = state.form.copy(pnr = "1")).isDirty)
    }

    @Test
    fun `reverting an edit clears dirty`() {
        val state = EditTicketUiState(isLoading = false)
        val edited = state.copy(form = state.form.copy(trainName = "X"))
        assertFalse(edited.copy(form = edited.form.copy(trainName = "")).isDirty)
    }

    @Test
    fun `missing required fields are listed in display order and exclude optional ones`() {
        val state =
            EditTicketUiState(
                missingFields = setOf(TicketField.DEPARTURE_DATE, TicketField.TRAIN_NAME, TicketField.PNR),
            )
        assertEquals(listOf(TicketField.PNR, TicketField.DEPARTURE_DATE), state.missingRequired)
    }

    @Test
    fun `incomplete form still maps to a ticket flagged for review`() {
        val ticket = EditTicketMapper.toTicket(EditTicketForm(pnr = "1234567890"), existing = null, now = Instant.EPOCH)
        assertTrue(ticket.needsReview)
        assertEquals("1234567890", ticket.pnr)
    }
}
