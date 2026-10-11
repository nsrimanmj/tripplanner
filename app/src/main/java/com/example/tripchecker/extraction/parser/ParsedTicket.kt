package com.example.tripchecker.extraction.parser

import com.example.tripchecker.domain.model.Station
import java.time.Instant
import java.time.LocalDate

/** Fields that must be present for auto-save without review. */
enum class TicketField {
    PNR,
    TRAIN_NUMBER,
    FROM,
    TO,
    DEPARTURE_DATE,
    ARRIVAL,
    START_DATE,
    TRAIN_NAME,
    CLASS,
    QUOTA,
    DISTANCE,
    BOOKED_AT,
    TRANSACTION_ID,
    TOTAL_FARE,
    PASSENGERS,
}

val REQUIRED_TICKET_FIELDS: Set<TicketField> = setOf(
    TicketField.PNR,
    TicketField.TRAIN_NUMBER,
    TicketField.FROM,
    TicketField.TO,
    TicketField.DEPARTURE_DATE,
)

/** Passenger row before persistence IDs are assigned. */
data class ParsedPassengerRow(
    val serial: Int,
    val name: String,
    val age: Int?,
    val gender: String?,
    val bookingStatusRaw: String?,
    val currentStatusRaw: String?,
)

/** Result of parsing extracted ticket text. */
data class ParsedTicket(
    val pnr: String? = null,
    val trainNumber: String? = null,
    val trainName: String? = null,
    val from: Station? = null,
    val boarding: Station? = null,
    val to: Station? = null,
    val startDate: LocalDate? = null,
    val departureAt: Instant? = null,
    val arrivalAt: Instant? = null,
    val travelClass: String? = null,
    val classLabel: String? = null,
    val quota: String? = null,
    val quotaLabel: String? = null,
    val distanceKm: Int? = null,
    val bookedAt: Instant? = null,
    val transactionId: String? = null,
    val totalFarePaise: Long? = null,
    val passengers: List<ParsedPassengerRow> = emptyList(),
    val missingFields: Set<TicketField> = emptySet(),
    val warnings: List<String> = emptyList(),
)
