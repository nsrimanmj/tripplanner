package com.example.tripchecker.ui.home

import com.example.tripchecker.domain.model.BerthStatus
import com.example.tripchecker.domain.model.JourneyKind
import com.example.tripchecker.extraction.parser.TicketField
import com.example.tripchecker.ui.common.StatusKind
import java.time.LocalDate

/** UI state for the home journey list; every field is pre-computed display data. */
data class HomeUiState(
    val isLoading: Boolean = true,
    val subtitle: String? = null,
    val nextJourney: NextJourneyUi? = null,
    val needsReview: List<NeedsReviewUi> = emptyList(),
    val upcoming: List<TripCardUi> = emptyList(),
    val pastByMonth: List<PastMonthUi> = emptyList(),
    val isEmpty: Boolean = false,
)

/** Hero card for the earliest future leg. */
data class NextJourneyUi(
    val ticketId: String,
    val overline: String,
    val countdown: String,
    val fromCode: String,
    val fromName: String,
    val departureTime: String,
    val toCode: String,
    val toName: String,
    /** "07:59", or "—" when the arrival time is unknown. */
    val arrivalTime: String,
    /** "+1" style next-day marker, null for same-day arrival. */
    val dayOffset: String?,
    /** "14h 59m"; null when the arrival time is unknown. */
    val duration: String?,
    /** "17001 · SNSI SC Express · Third AC". */
    val trainLine: String,
    val seat: SeatPanelUi?,
    val pdfFileName: String?,
)

/** Seat panel of the hero: the first passenger that is not cancelled. */
data class SeatPanelUi(
    val passengerName: String,
    /** Number of other non-cancelled passengers ("+3 more"). */
    val othersCount: Int,
    val status: BerthStatus?,
    val statusKind: StatusKind,
    val coach: String,
    val berth: String,
    val berthType: String,
    /** Single large value such as "WL 12" or "RAC 23"; non-null replaces the three seat columns. */
    val bigValue: String?,
)

/** Needs-review row: the file name plus a short list of missing required fields. */
data class NeedsReviewUi(
    val ticketId: String,
    val fileName: String?,
    /** At most [HomeUiMapper.MAX_MISSING_SHOWN] missing required fields, in canonical order. */
    val missing: List<TicketField>,
    /** How many further missing fields are not listed. */
    val moreMissing: Int,
)

/** Upcoming or in-progress group card. */
data class TripCardUi(
    val key: String,
    val anchorTicketId: String,
    val kind: JourneyKind,
    val inProgress: Boolean,
    /** "3 – 10 Aug · 8 days" for multi-leg, "Sat, 22 Aug" for one-way. */
    val dateRange: String,
    /** City title for ROUND_TRIP and CUSTOM; null for ONE_WAY. */
    val title: String?,
    val legs: List<LegRowUi>,
)

/** One leg row inside a [TripCardUi]. */
data class LegRowUi(
    val id: String,
    val date: LocalDate?,
    /** True for the leg that is the hero's next departure. */
    val highlighted: Boolean,
    /** "SNSI → BMT". */
    val route: String,
    /** "17:00 · 17001 · 3A", plus " · boards LPI" when boarding differs from the origin. */
    val meta: String,
    val cancelled: Boolean,
    val seat: LegSeatUi,
)

/** How the trailing seat area of a leg row renders. */
enum class LegSeatKind { NONE, SEAT, STATUS_CHIP }

/** Trailing seat summary of a leg row. */
data class LegSeatUi(
    val kind: LegSeatKind,
    /** "B4 · 33" for SEAT, "WL 12" for STATUS_CHIP. */
    val primary: String = "",
    val berthType: String = "",
    /** Other non-cancelled passengers; when above zero the UI shows "+N passengers" instead of the berth type. */
    val othersCount: Int = 0,
    val statusKind: StatusKind = StatusKind.UNKNOWN,
) {
    companion object {
        val None = LegSeatUi(LegSeatKind.NONE)
    }
}

/** Past-trips month section ("JULY 2026"). */
data class PastMonthUi(
    val header: String,
    val rows: List<PastTripUi>,
)

/** Compact past trip row. */
data class PastTripUi(
    val key: String,
    val anchorTicketId: String,
    val kind: JourneyKind,
    val title: String,
    /** "17 – 24 Jul · 17001 / 17002" or "Sun, 5 Jul · 17001". */
    val subtitle: String,
    /** True for a cancelled one-way: struck-through title and a "Cancelled" chip. */
    val cancelled: Boolean,
)
