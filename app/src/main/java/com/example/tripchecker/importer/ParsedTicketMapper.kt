package com.example.tripchecker.importer

import com.example.tripchecker.domain.model.ExtractionMethod
import com.example.tripchecker.domain.model.Passenger
import com.example.tripchecker.domain.model.Ticket
import com.example.tripchecker.extraction.parser.ParsedTicket
import com.example.tripchecker.extraction.parser.PassengerStatusParser
import com.example.tripchecker.extraction.parser.REQUIRED_TICKET_FIELDS
import java.time.Instant
import java.util.UUID

/** Maps parsed ticket text fields to domain [Ticket] rows. */
object ParsedTicketMapper {
    fun toNewTicket(
        parsed: ParsedTicket,
        ticketId: String,
        pdfFileName: String?,
        pdfSha256: String?,
        originalFileName: String?,
        extractionMethod: ExtractionMethod,
        now: Instant,
    ): Ticket {
        val needsReview = parsed.missingFields.intersect(REQUIRED_TICKET_FIELDS).isNotEmpty()
        return Ticket(
            id = ticketId,
            pnr = parsed.pnr,
            trainNumber = parsed.trainNumber,
            trainName = parsed.trainName,
            from = parsed.from,
            boarding = parsed.boarding,
            to = parsed.to,
            startDate = parsed.startDate,
            departureAt = parsed.departureAt,
            arrivalAt = parsed.arrivalAt,
            travelClass = parsed.travelClass,
            classLabel = parsed.classLabel,
            quota = parsed.quota,
            quotaLabel = parsed.quotaLabel,
            distanceKm = parsed.distanceKm,
            bookedAt = parsed.bookedAt,
            transactionId = parsed.transactionId,
            totalFarePaise = parsed.totalFarePaise,
            passengers = parsed.passengers.map { row ->
                Passenger(
                    id = UUID.randomUUID().toString(),
                    serial = row.serial,
                    name = row.name,
                    age = row.age,
                    gender = row.gender,
                    bookingStatusRaw = row.bookingStatusRaw,
                    currentStatusRaw = row.currentStatusRaw,
                    current =
                        PassengerStatusParser.parse(row.currentStatusRaw)
                            ?: PassengerStatusParser.parse(row.bookingStatusRaw),
                )
            },
            pdfFileName = pdfFileName,
            pdfSha256 = pdfSha256,
            originalFileName = originalFileName,
            extractionMethod = extractionMethod,
            needsReview = needsReview,
            linkGroupId = null,
            autoPairDisabled = false,
            isCancelled = false,
            notes = null,
            createdAt = now,
            updatedAt = now,
        )
    }

    fun mergeParsedIntoExisting(
        existing: Ticket,
        parsed: ParsedTicket,
        pdfFileName: String?,
        pdfSha256: String?,
        originalFileName: String?,
        extractionMethod: ExtractionMethod,
        now: Instant,
    ): Ticket {
        val base = toNewTicket(parsed, existing.id, pdfFileName, pdfSha256, originalFileName, extractionMethod, now)
        val needsReview = parsed.missingFields.intersect(REQUIRED_TICKET_FIELDS).isNotEmpty()
        return base.copy(
            linkGroupId = existing.linkGroupId,
            autoPairDisabled = existing.autoPairDisabled,
            isCancelled = existing.isCancelled,
            notes = existing.notes,
            createdAt = existing.createdAt,
            needsReview = needsReview,
            updatedAt = now,
        )
    }
}
