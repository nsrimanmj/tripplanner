package com.example.tripchecker.data.db

import com.example.tripchecker.data.db.entity.PassengerEntity
import com.example.tripchecker.data.db.entity.TicketEntity
import com.example.tripchecker.data.db.relation.TicketWithPassengers
import com.example.tripchecker.domain.model.BerthStatus
import com.example.tripchecker.domain.model.ExtractionMethod
import com.example.tripchecker.domain.model.Passenger
import com.example.tripchecker.domain.model.Station
import com.example.tripchecker.domain.model.Ticket

fun TicketWithPassengers.toDomain(): Ticket = ticket.toDomain(passengers)

fun TicketEntity.toDomain(passengerEntities: List<PassengerEntity>): Ticket =
    Ticket(
        id = id,
        pnr = pnr,
        trainNumber = trainNumber,
        trainName = trainName,
        from = fromCode?.let { Station(it, fromName) },
        boarding = boardingCode?.let { Station(it, boardingName) },
        to = toCode?.let { Station(it, toName) },
        startDate = startDate,
        departureAt = departureAt,
        arrivalAt = arrivalAt,
        travelClass = travelClass,
        classLabel = classLabel,
        quota = quota,
        quotaLabel = quotaLabel,
        distanceKm = distanceKm,
        bookedAt = bookedAt,
        transactionId = transactionId,
        totalFarePaise = totalFarePaise,
        passengers = passengerEntities.sortedBy { it.serial }.map { it.toDomain() },
        pdfFileName = pdfFileName,
        pdfSha256 = pdfSha256,
        originalFileName = originalFileName,
        extractionMethod =
            runCatching { ExtractionMethod.valueOf(extractionMethod) }
                .getOrDefault(ExtractionMethod.OCR),
        needsReview = needsReview,
        linkGroupId = linkGroupId,
        autoPairDisabled = autoPairDisabled,
        isCancelled = isCancelled,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun PassengerEntity.toDomain(): Passenger =
    Passenger(
        id = id,
        serial = serial,
        name = name,
        age = age,
        gender = gender,
        bookingStatusRaw = bookingStatusRaw,
        currentStatusRaw = currentStatusRaw,
        current =
            if (statusCode != null) {
                BerthStatus(
                    code = statusCode,
                    coach = coach,
                    berthNumber = berthNumber,
                    berthType = berthType,
                    waitlistNumber = waitlistNumber,
                )
            } else {
                null
            },
    )

fun Ticket.toEntity(rawText: String? = null): TicketEntity =
    TicketEntity(
        id = id,
        pnr = pnr,
        trainNumber = trainNumber,
        trainName = trainName,
        fromCode = from?.code,
        fromName = from?.name,
        boardingCode = boarding?.code,
        boardingName = boarding?.name,
        toCode = to?.code,
        toName = to?.name,
        startDate = startDate,
        departureAt = departureAt,
        arrivalAt = arrivalAt,
        travelClass = travelClass,
        classLabel = classLabel,
        quota = quota,
        quotaLabel = quotaLabel,
        distanceKm = distanceKm,
        bookedAt = bookedAt,
        transactionId = transactionId,
        totalFarePaise = totalFarePaise,
        pdfFileName = pdfFileName,
        pdfSha256 = pdfSha256,
        originalFileName = originalFileName,
        extractionMethod = extractionMethod.name,
        needsReview = needsReview,
        linkGroupId = linkGroupId,
        autoPairDisabled = autoPairDisabled,
        isCancelled = isCancelled,
        notes = notes,
        rawText = rawText,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun Passenger.toEntity(ticketId: String): PassengerEntity {
    val status = current
    return PassengerEntity(
        id = id,
        ticketId = ticketId,
        serial = serial,
        name = name,
        age = age,
        gender = gender,
        bookingStatusRaw = bookingStatusRaw,
        currentStatusRaw = currentStatusRaw,
        statusCode = status?.code,
        coach = status?.coach,
        berthNumber = status?.berthNumber,
        berthType = status?.berthType,
        waitlistNumber = status?.waitlistNumber,
    )
}
