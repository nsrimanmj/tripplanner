package com.example.tripchecker.ui.home

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.tripchecker.domain.model.BerthStatus
import com.example.tripchecker.domain.model.JourneyKind
import com.example.tripchecker.extraction.parser.TicketField
import com.example.tripchecker.ui.common.StatusKind
import com.example.tripchecker.ui.theme.TripcheckerTheme
import java.time.LocalDate

/** Themed background wrapper shared by the Home previews. */
@Composable
internal fun HomePreviewSurface(
    dark: Boolean,
    content: @Composable () -> Unit,
) {
    TripcheckerTheme(darkTheme = dark) {
        Surface(color = MaterialTheme.colorScheme.surface, content = content)
    }
}

/** Fake pre-computed UI models for the Home previews. */
internal object HomePreviewData {
    val seatConfirmed =
        SeatPanelUi(
            passengerName = "Madhuri",
            othersCount = 3,
            status = BerthStatus("CNF", "B4", 33, "LOWER"),
            statusKind = StatusKind.CONFIRMED,
            coach = "B4",
            berth = "33",
            berthType = "Lower",
            bigValue = null,
        )

    val seatWaitlist =
        SeatPanelUi(
            passengerName = "Madhuri",
            othersCount = 0,
            status = BerthStatus("GNWL", waitlistNumber = 12),
            statusKind = StatusKind.WAITLIST,
            coach = "—",
            berth = "—",
            berthType = "—",
            bigValue = "WL 12",
        )

    val hero =
        NextJourneyUi(
            ticketId = "t1",
            overline = "MON, 3 AUG · 17:00",
            countdown = "in 2d 4h",
            fromCode = "SNSI",
            fromName = "Sainagar Shirdi",
            departureTime = "17:00",
            toCode = "BMT",
            toName = "Begampet",
            arrivalTime = "07:59",
            dayOffset = "+1",
            duration = "14h 59m",
            trainLine = "17001 · SNSI SC Express · Third AC",
            seat = seatConfirmed,
            pdfFileName = "t1.pdf",
        )

    val heroWaitlist = hero.copy(ticketId = "t2", seat = seatWaitlist)

    val needsReview =
        NeedsReviewUi("n1", "scan_0812.pdf", listOf(TicketField.PNR, TicketField.DEPARTURE_DATE), 0)
    val needsReviewMany =
        NeedsReviewUi("n2", null, listOf(TicketField.PNR, TicketField.TRAIN_NUMBER), 3)

    private val seatLeg1 = LegSeatUi(LegSeatKind.SEAT, "B4 · 33", "Lower", 0, StatusKind.CONFIRMED)
    private val seatLeg2 = LegSeatUi(LegSeatKind.SEAT, "S7 · 49", "Side Upper", 3, StatusKind.CONFIRMED)

    val roundTripCard =
        TripCardUi(
            key = "rt",
            anchorTicketId = "t1",
            kind = JourneyKind.ROUND_TRIP,
            inProgress = false,
            dateRange = "3 – 10 Aug · 8 days",
            title = "Shirdi ⇄ Hyderabad",
            legs =
                listOf(
                    LegRowUi("t1", LocalDate.of(2026, 8, 3), true, "SNSI → BMT", "17:00 · 17001 · 3A", false, seatLeg1),
                    LegRowUi(
                        "t3", LocalDate.of(2026, 8, 10), false, "SC → SNSI",
                        "21:40 · 17002 · SL · boards LPI", false, seatLeg2,
                    ),
                ),
        )

    val oneWayWaitlistCard =
        TripCardUi(
            key = "ow",
            anchorTicketId = "t4",
            kind = JourneyKind.ONE_WAY,
            inProgress = false,
            dateRange = "Sat, 22 Aug",
            title = null,
            legs =
                listOf(
                    LegRowUi(
                        "t4", LocalDate.of(2026, 8, 22), false, "SNSI → BMT", "17:00 · 17001 · 3A", false,
                        LegSeatUi(LegSeatKind.STATUS_CHIP, "WL 12", statusKind = StatusKind.WAITLIST),
                    ),
                ),
        )

    val customInProgressCard =
        TripCardUi(
            key = "cu",
            anchorTicketId = "t5",
            kind = JourneyKind.CUSTOM,
            inProgress = true,
            dateRange = "1 – 4 Aug · 4 days",
            title = "Shirdi → Hyderabad → Pune",
            legs =
                listOf(
                    LegRowUi("t5", LocalDate.of(2026, 8, 1), false, "SNSI → BMT", "17:00 · 17001 · 3A", false, seatLeg1),
                    LegRowUi(
                        "t6", LocalDate.of(2026, 8, 4), true, "SC → PUNE", "21:00 · 12026 · CC", true, LegSeatUi.None,
                    ),
                ),
        )

    val pastJuly =
        listOf(
            PastTripUi("p1", "p1", JourneyKind.ROUND_TRIP, "Shirdi ⇄ Hyderabad", "17 – 24 Jul · 17001 / 17002", false),
            PastTripUi("p2", "p2", JourneyKind.ONE_WAY, "SNSI → BMT", "Sun, 5 Jul · 17001", true),
        )

    val pastJune =
        listOf(
            PastTripUi("p3", "p3", JourneyKind.ROUND_TRIP, "Shirdi ⇄ Hyderabad", "19 – 26 Jun · 17001 / 17002", false),
        )

    val fullState =
        HomeUiState(
            isLoading = false,
            subtitle = "Sat, 1 Aug · 2 trips ahead",
            nextJourney = hero,
            needsReview = listOf(needsReview),
            upcoming = listOf(roundTripCard, oneWayWaitlistCard),
            pastByMonth = listOf(PastMonthUi("JULY 2026", pastJuly), PastMonthUi("JUNE 2026", pastJune)),
        )
}

@Preview(name = "Home light", widthDp = 390, heightDp = 1800)
@Composable
private fun HomeLightPreview() =
    HomePreviewSurface(dark = false) {
        HomeContent(
            state = HomePreviewData.fullState,
            onOpenTrip = {},
            onEditTicket = {},
            onPdfsSelected = {},
            onAddManual = {},
        )
    }

@Preview(name = "Home dark", widthDp = 390, heightDp = 1800)
@Composable
private fun HomeDarkPreview() =
    HomePreviewSurface(dark = true) {
        HomeContent(
            state = HomePreviewData.fullState,
            onOpenTrip = {},
            onEditTicket = {},
            onPdfsSelected = {},
            onAddManual = {},
        )
    }

@Preview(name = "Home empty light", widthDp = 390, heightDp = 800)
@Composable
private fun HomeEmptyLightPreview() =
    HomePreviewSurface(dark = false) {
        HomeContent(
            state = HomeUiState(isLoading = false, isEmpty = true),
            onOpenTrip = {},
            onEditTicket = {},
            onPdfsSelected = {},
            onAddManual = {},
        )
    }

@Preview(name = "Home empty dark", widthDp = 390, heightDp = 800)
@Composable
private fun HomeEmptyDarkPreview() =
    HomePreviewSurface(dark = true) {
        HomeContent(
            state = HomeUiState(isLoading = false, isEmpty = true),
            onOpenTrip = {},
            onEditTicket = {},
            onPdfsSelected = {},
            onAddManual = {},
        )
    }
