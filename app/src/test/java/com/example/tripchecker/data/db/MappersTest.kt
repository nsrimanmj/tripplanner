package com.example.tripchecker.data.db

import com.example.tripchecker.domain.model.BerthStatus
import com.example.tripchecker.domain.model.ExtractionMethod
import com.example.tripchecker.domain.model.Passenger
import com.example.tripchecker.domain.model.Station
import com.example.tripchecker.domain.model.Ticket
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class MappersTest {
    @Test
    fun `ticket and passengers round trip`() {
        val now = Instant.parse("2026-08-01T10:00:00Z")
        val ticket =
            Ticket(
                id = "t1",
                pnr = "1234567890",
                trainNumber = "17001",
                trainName = "EXPRESS",
                from = Station("SNSI", "SHIRDI"),
                boarding = Station("SNSI", "SHIRDI"),
                to = Station("BMT", "BEGAMPET"),
                startDate = LocalDate.of(2026, 8, 3),
                departureAt = now,
                arrivalAt = now.plusSeconds(3600),
                travelClass = "3A",
                classLabel = "THIRD AC",
                quota = "TQ",
                quotaLabel = "TATKAL",
                distanceKm = 772,
                bookedAt = now,
                transactionId = "tx",
                totalFarePaise = 149405,
                passengers =
                    listOf(
                        Passenger(
                            id = "p1",
                            serial = 1,
                            name = "MADHURI",
                            age = 26,
                            gender = "F",
                            bookingStatusRaw = "CNF/B4/33/LOWER",
                            currentStatusRaw = "CNF/B4/33/LOWER",
                            current = BerthStatus("CNF", "B4", 33, "LOWER", null),
                        ),
                    ),
                pdfFileName = "t1.pdf",
                pdfSha256 = "abc",
                originalFileName = "ticket.pdf",
                extractionMethod = ExtractionMethod.OCR,
                needsReview = false,
                linkGroupId = null,
                autoPairDisabled = false,
                isCancelled = false,
                notes = null,
                createdAt = now,
                updatedAt = now,
            )
        val entity = ticket.toEntity(rawText = "raw")
        val passengers = ticket.passengers.map { it.toEntity(ticket.id) }
        val back = entity.toDomain(passengers)
        assertEquals(ticket, back)
        assertEquals("raw", entity.rawText)
    }
}
