package com.example.tripchecker.ui.edit

import com.example.tripchecker.domain.model.Ticket
import com.example.tripchecker.extraction.parser.ParsedTicket
import com.example.tripchecker.extraction.parser.REQUIRED_TICKET_FIELDS
import com.example.tripchecker.extraction.parser.TicketParser
import com.example.tripchecker.importer.ParsedTicketMapper
/** Builds an edit form from DB ticket plus optional re-parse of stored extract text. */
object EditTicketHydrator {
    fun hydrate(
        ticket: Ticket,
        rawText: String?,
        parsers: List<TicketParser>,
    ): EditTicketForm {
        val fromDb = EditTicketMapper.fromTicket(ticket)
        val missingRequired = EditTicketMapper.missingFields(fromDb).intersect(REQUIRED_TICKET_FIELDS)
        if (missingRequired.isEmpty() || rawText.isNullOrBlank()) {
            return fromDb
        }
        val parser = parsers.firstOrNull { it.canParse(rawText) } ?: return fromDb
        val parsed = parser.parse(rawText)
        return mergeParsedIntoForm(fromDb, parsed, ticket)
    }

    private fun mergeParsedIntoForm(
        form: EditTicketForm,
        parsed: ParsedTicket,
        ticket: Ticket,
    ): EditTicketForm {
        val parsedTicket =
            ParsedTicketMapper.toNewTicket(
                parsed = parsed,
                ticketId = ticket.id,
                pdfFileName = ticket.pdfFileName,
                pdfSha256 = ticket.pdfSha256,
                originalFileName = ticket.originalFileName,
                extractionMethod = ticket.extractionMethod,
                now = ticket.updatedAt,
            )
        val parsedForm = EditTicketMapper.fromTicket(parsedTicket)
        return form.copy(
            pnr = form.pnr.ifBlank { parsedForm.pnr },
            trainNumber = form.trainNumber.ifBlank { parsedForm.trainNumber },
            trainName = form.trainName.ifBlank { parsedForm.trainName },
            fromCode = form.fromCode.ifBlank { parsedForm.fromCode },
            fromName = form.fromName.ifBlank { parsedForm.fromName },
            boardingCode = form.boardingCode.ifBlank { parsedForm.boardingCode },
            boardingName = form.boardingName.ifBlank { parsedForm.boardingName },
            toCode = form.toCode.ifBlank { parsedForm.toCode },
            toName = form.toName.ifBlank { parsedForm.toName },
            startDate = form.startDate ?: parsedForm.startDate,
            departureDate = form.departureDate ?: parsedForm.departureDate,
            departureTime = form.departureTime ?: parsedForm.departureTime,
            arrivalDate = form.arrivalDate ?: parsedForm.arrivalDate,
            arrivalTime = form.arrivalTime ?: parsedForm.arrivalTime,
            travelClass = form.travelClass.ifBlank { parsedForm.travelClass },
            classLabel = form.classLabel.ifBlank { parsedForm.classLabel },
            quota = form.quota.ifBlank { parsedForm.quota },
            quotaLabel = form.quotaLabel.ifBlank { parsedForm.quotaLabel },
            distanceKm = form.distanceKm.ifBlank { parsedForm.distanceKm },
            totalFareRupees = form.totalFareRupees.ifBlank { parsedForm.totalFareRupees },
            passengers =
                if (form.passengers.all { it.name.isBlank() } && parsedForm.passengers.any { it.name.isNotBlank() }) {
                    parsedForm.passengers
                } else {
                    form.passengers
                },
        )
    }
}
