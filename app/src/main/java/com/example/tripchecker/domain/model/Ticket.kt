package com.example.tripchecker.domain.model

import java.time.Instant
import java.time.LocalDate

/** A stored IRCTC e-ticket with extracted fields and PDF metadata. */
data class Ticket(
    val id: String,
    val pnr: String?,
    val trainNumber: String?,
    val trainName: String?,
    val from: Station?,
    val boarding: Station?,
    val to: Station?,
    val startDate: LocalDate?,
    val departureAt: Instant?,
    val arrivalAt: Instant?,
    val travelClass: String?,
    val classLabel: String?,
    val quota: String?,
    val quotaLabel: String?,
    val distanceKm: Int?,
    val bookedAt: Instant?,
    val transactionId: String?,
    val totalFarePaise: Long?,
    val passengers: List<Passenger>,
    val pdfFileName: String?,
    val pdfSha256: String?,
    val originalFileName: String?,
    val extractionMethod: ExtractionMethod,
    val needsReview: Boolean,
    val linkGroupId: String?,
    val autoPairDisabled: Boolean,
    val isCancelled: Boolean,
    val notes: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    val origin: Station? get() = boarding ?: from
}
