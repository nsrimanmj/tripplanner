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
            passengers = mapPassengers(parsed),
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
        val mergedPassengers =
            if (parsed.passengers.isNotEmpty()) {
                mapPassengers(parsed)
            } else {
                existing.passengers
            }
        val merged =
            existing.copy(
                pnr = parsed.pnr ?: existing.pnr,
                trainNumber = parsed.trainNumber ?: existing.trainNumber,
                trainName = parsed.trainName ?: existing.trainName,
                from = parsed.from ?: existing.from,
                boarding = parsed.boarding ?: existing.boarding,
                to = parsed.to ?: existing.to,
                startDate = parsed.startDate ?: existing.startDate,
                departureAt = parsed.departureAt ?: existing.departureAt,
                arrivalAt = parsed.arrivalAt ?: existing.arrivalAt,
                travelClass = parsed.travelClass ?: existing.travelClass,
                classLabel = parsed.classLabel ?: existing.classLabel,
                quota = parsed.quota ?: existing.quota,
                quotaLabel = parsed.quotaLabel ?: existing.quotaLabel,
                distanceKm = parsed.distanceKm ?: existing.distanceKm,
                bookedAt = parsed.bookedAt ?: existing.bookedAt,
                transactionId = parsed.transactionId ?: existing.transactionId,
                totalFarePaise = parsed.totalFarePaise ?: existing.totalFarePaise,
                passengers = mergedPassengers,
                pdfFileName = pdfFileName,
                pdfSha256 = pdfSha256,
                originalFileName = originalFileName ?: existing.originalFileName,
                extractionMethod = extractionMethod,
                updatedAt = now,
            )
        val missingAfterMerge =
            buildSet {
                if (merged.pnr.isNullOrBlank()) add(com.example.tripchecker.extraction.parser.TicketField.PNR)
                if (merged.trainNumber.isNullOrBlank()) add(com.example.tripchecker.extraction.parser.TicketField.TRAIN_NUMBER)
                if (merged.from == null) add(com.example.tripchecker.extraction.parser.TicketField.FROM)
                if (merged.to == null) add(com.example.tripchecker.extraction.parser.TicketField.TO)
                if (merged.departureAt == null) add(com.example.tripchecker.extraction.parser.TicketField.DEPARTURE_DATE)
            }
        return merged.copy(needsReview = missingAfterMerge.intersect(REQUIRED_TICKET_FIELDS).isNotEmpty())
    }

    private fun mapPassengers(parsed: ParsedTicket): List<Passenger> =
        parsed.passengers.map { row ->
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
        }
}
