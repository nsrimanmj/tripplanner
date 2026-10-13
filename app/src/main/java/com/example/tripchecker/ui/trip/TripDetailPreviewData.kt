package com.example.tripchecker.ui.trip

import com.example.tripchecker.domain.model.BerthStatus
import com.example.tripchecker.domain.model.ExtractionMethod
import com.example.tripchecker.domain.model.JourneyGroup
import com.example.tripchecker.domain.model.JourneyKind
import com.example.tripchecker.domain.model.JourneyStatus
import com.example.tripchecker.domain.model.Passenger
import com.example.tripchecker.domain.model.Station
import com.example.tripchecker.domain.model.Ticket
import java.time.Instant

/** Fake data for `@Preview`s of the trip detail screen. */
internal object TripDetailPreviewData {
    private val now: Instant = Instant.parse("2026-08-01T10:00:00Z")

    private fun passenger(
        id: String,
        name: String,
        age: Int,
        gender: String,
        booking: String,
        current: BerthStatus?,
        currentRaw: String?,
    ) = Passenger(id, 1, name, age, gender, booking, currentRaw, current)

    val outbound =
        Ticket(
            id = "out",
            pnr = "8751652729",
            trainNumber = "17001",
            trainName = "SNSI SC EXPRESS",
            from = Station("SNSI", "SAINAGAR SHIRDI"),
            boarding = null,
            to = Station("BMT", "BEGAMPET"),
            startDate = null,
            departureAt = Instant.parse("2026-08-03T11:30:00Z"),
            arrivalAt = Instant.parse("2026-08-04T02:29:00Z"),
            travelClass = "3A",
            classLabel = "THIRD AC",
            quota = "TQ",
            quotaLabel = "TATKAL",
            distanceKm = 772,
            bookedAt = null,
            transactionId = null,
            totalFarePaise = 149405,
            passengers =
                listOf(
                    passenger("p1", "MADHURI", 26, "F", "CNF/B4/33", BerthStatus("CNF", "B4", 33, "LOWER"), "CNF/B4/33"),
                ),
            pdfFileName = "out.pdf",
            pdfSha256 = null,
            originalFileName = null,
            extractionMethod = ExtractionMethod.OCR,
            needsReview = false,
            linkGroupId = null,
            autoPairDisabled = false,
            isCancelled = false,
            notes = null,
            createdAt = now,
            updatedAt = now,
        )

    val inbound =
        outbound.copy(
            id = "ret",
            pnr = "4523198760",
            trainNumber = "17002",
            trainName = "SC SNSI EXPRESS",
            from = Station("SC", "SECUNDERABAD JN"),
            boarding = Station("LPI", "LINGAMPALLI"),
            to = Station("SNSI", "SAINAGAR SHIRDI"),
            departureAt = Instant.parse("2026-08-10T16:10:00Z"),
            arrivalAt = null,
            travelClass = "SL",
            classLabel = "SLEEPER CLASS",
            quota = "GN",
            quotaLabel = "GENERAL",
            distanceKm = 690,
            totalFarePaise = 216360,
            extractionMethod = ExtractionMethod.TEXT_LAYER,
            passengers =
                listOf(
                    passenger("p2", "RAVI KUMAR", 34, "M", "CNF/S7/49", BerthStatus("CNF", "S7", 49, "SIDE_UPPER"), "CNF/S7/49"),
                    passenger("p3", "ANITA", 31, "F", "RLWL/12", BerthStatus("RLWL", waitlistNumber = 5), "RLWL/5"),
                    passenger("p4", "ARJUN", 8, "M", "RAC/23", BerthStatus("RAC", "S2", 71, "SIDE_LOWER"), "RAC/S2/71"),
                    passenger("p5", "MEERA", 60, "F", "WL/40", BerthStatus("CAN"), "CAN"),
                ),
        )

    val roundTrip = JourneyGroup("auto:out", JourneyKind.ROUND_TRIP, listOf(outbound, inbound), false, JourneyStatus.UPCOMING)
    val manualCustom = JourneyGroup("manual:x", JourneyKind.CUSTOM, listOf(outbound, inbound), true, JourneyStatus.PAST)
    val oneWay = JourneyGroup("auto:out", JourneyKind.ONE_WAY, listOf(outbound.copy(autoPairDisabled = true)), false, JourneyStatus.UPCOMING)

    private fun state(group: JourneyGroup, sheet: LinkSheetUi = LinkSheetUi("", emptyList())) =
        TripDetailUiState(
            group = group,
            anchor = group.legs.first(),
            header = TripDetailUiMapper.header(group),
            menu = TripDetailUiMapper.menu(group),
            legs = TripDetailUiMapper.legs(group),
            stays = TripDetailUiMapper.stays(group),
            pairing = TripDetailUiMapper.pairingCard(group),
            linkSheet = sheet,
        )

    val roundTripState = state(roundTrip)
    val oneWayState = state(oneWay)

    val linkSheet =
        LinkSheetUi(
            destinationCity = "Hyderabad",
            candidates =
                listOf(
                    LinkCandidateUi("a", "BMT → SNSI", "Sat, 29 Aug · 21:40 · 17002", true, "7 days later"),
                    LinkCandidateUi("b", "SC → SNSI", "Sun, 6 Sep · 21:40 · 17002", true, "15 days later"),
                    LinkCandidateUi("c", "SNSI → BMT", "Sat, 5 Sep · 17:00 · 17001", false, "14 days later"),
                ),
        )
}
