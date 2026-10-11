package com.example.tripchecker.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

@Entity(
    tableName = "tickets",
    indices = [
        Index(value = ["pnr"], unique = true),
        Index(value = ["pdfSha256"], unique = true),
        Index(value = ["departureAt"]),
        Index(value = ["linkGroupId"]),
    ],
)
data class TicketEntity(
    @PrimaryKey val id: String,
    val pnr: String?,
    val trainNumber: String?,
    val trainName: String?,
    val fromCode: String?,
    val fromName: String?,
    val boardingCode: String?,
    val boardingName: String?,
    val toCode: String?,
    val toName: String?,
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
    val pdfFileName: String?,
    val pdfSha256: String?,
    val originalFileName: String?,
    val extractionMethod: String,
    val needsReview: Boolean,
    val linkGroupId: String?,
    val autoPairDisabled: Boolean,
    val isCancelled: Boolean,
    val notes: String?,
    val rawText: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)
