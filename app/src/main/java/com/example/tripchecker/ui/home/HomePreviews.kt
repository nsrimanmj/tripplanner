package com.example.tripchecker.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.tripchecker.domain.model.ExtractionMethod
import com.example.tripchecker.domain.model.JourneyGroup
import com.example.tripchecker.domain.model.JourneyKind
import com.example.tripchecker.domain.model.JourneyStatus
import com.example.tripchecker.domain.model.Station
import com.example.tripchecker.domain.model.Ticket
import com.example.tripchecker.ui.theme.TripcheckerTheme
import java.time.Instant

@Preview(showBackground = true)
@Composable
private fun PreviewHomeWithNextJourney() {
    val leg = previewTicket()
    val group = JourneyGroup("auto:o", JourneyKind.ROUND_TRIP, listOf(leg), false, JourneyStatus.UPCOMING)
    TripcheckerTheme {
        HomeContent(
            state =
                HomeUiState(
                    nextJourney =
                        NextJourneyUi(
                            ticketId = leg.id,
                            countdown = "Today · 17:00",
                            trainLine = "17001 SNSI SC EXPRESS",
                            route = "SNSI → BMT",
                            departureTime = "17:00",
                            berthSummary = "B4 · 33 · LOWER",
                            pdfFileName = "x.pdf",
                        ),
                    upcoming = listOf(group),
                    isEmpty = false,
                ),
            onOpenTrip = {},
            onEditTicket = {},
            onPdfsSelected = {},
            onAddManual = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewHomeEmpty() {
    TripcheckerTheme {
        HomeContent(
            state = HomeUiState(isEmpty = true, isLoading = false),
            onOpenTrip = {},
            onEditTicket = {},
            onPdfsSelected = {},
            onAddManual = {},
        )
    }
}

private fun previewTicket(): Ticket {
    val now = Instant.parse("2026-08-01T10:00:00Z")
    return Ticket(
        id = "preview",
        pnr = "8751652729",
        trainNumber = "17001",
        trainName = "SNSI SC EXPRESS",
        from = Station("SNSI", "SAINAGAR SHIRDI"),
        boarding = null,
        to = Station("BMT", "BEGAMPET"),
        startDate = null,
        departureAt = now,
        arrivalAt = null,
        travelClass = "3A",
        classLabel = null,
        quota = "TQ",
        quotaLabel = null,
        distanceKm = 772,
        bookedAt = null,
        transactionId = null,
        totalFarePaise = 149405,
        passengers = emptyList(),
        pdfFileName = "preview.pdf",
        pdfSha256 = null,
        originalFileName = null,
        extractionMethod = ExtractionMethod.TEXT_LAYER,
        needsReview = false,
        linkGroupId = null,
        autoPairDisabled = false,
        isCancelled = false,
        notes = null,
        createdAt = now,
        updatedAt = now,
    )
}
