package com.example.tripchecker.ui.trip

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
private fun PreviewTripDetailRoundTrip() {
    val now = Instant.parse("2026-08-01T10:00:00Z")
    val leg =
        Ticket(
            id = "leg1",
            pnr = "8751652729",
            trainNumber = "17001",
            trainName = "SNSI SC EXPRESS",
            from = Station("SNSI", "SAINAGAR SHIRDI"),
            boarding = null,
            to = Station("BMT", "BEGAMPET"),
            startDate = null,
            departureAt = now,
            arrivalAt = now.plusSeconds(86_400),
            travelClass = "3A",
            classLabel = "THIRD AC",
            quota = "TQ",
            quotaLabel = "TATKAL",
            distanceKm = 772,
            bookedAt = null,
            transactionId = null,
            totalFarePaise = 149405,
            passengers = emptyList(),
            pdfFileName = "leg1.pdf",
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
    val group = JourneyGroup("auto:leg1", JourneyKind.ROUND_TRIP, listOf(leg), false, JourneyStatus.UPCOMING)
    TripcheckerTheme {
        TripDetailContent(
            state = TripDetailUiState(group = group, anchor = leg),
            snackbarHostState = remember { SnackbarHostState() },
            onBack = {},
            onEdit = {},
            onCopyPnr = {},
            onGoToTicket = {},
            onShare = {},
            onToggleCancelled = {},
            onDeleteRequest = {},
            onUnlink = {},
            onResetPairing = {},
            onLinkReturn = {},
            onLinkSelect = {},
            linkSheetVisible = false,
            onDismissLinkSheet = {},
            linkCandidates = emptyList(),
        )
    }
}
