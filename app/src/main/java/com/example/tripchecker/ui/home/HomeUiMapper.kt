package com.example.tripchecker.ui.home

import com.example.tripchecker.domain.grouping.CityNames
import com.example.tripchecker.domain.model.BerthStatus
import com.example.tripchecker.domain.model.JourneyGroup
import com.example.tripchecker.domain.model.JourneyKind
import com.example.tripchecker.domain.model.JourneyStatus
import com.example.tripchecker.domain.model.Ticket
import com.example.tripchecker.domain.text.DisplayText
import com.example.tripchecker.extraction.parser.IrctcDates
import com.example.tripchecker.extraction.parser.REQUIRED_TICKET_FIELDS
import com.example.tripchecker.extraction.parser.TicketField
import com.example.tripchecker.ui.common.Formatters
import com.example.tripchecker.ui.common.StatusKind
import com.example.tripchecker.ui.common.StatusTones
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

/** Pure mapping from domain journeys to [HomeUiState]. */
object HomeUiMapper {
    /** Maximum number of missing fields spelled out on a needs-review row before "+N". */
    const val MAX_MISSING_SHOWN = 2

    private const val SEP = " · "

    fun map(
        groups: List<JourneyGroup>,
        tickets: List<Ticket>,
        now: Instant,
    ): HomeUiState {
        val reviewTickets = tickets.filter { it.needsReview }
        val reviewIds = reviewTickets.map { it.id }.toSet()
        val visibleGroups =
            groups.mapNotNull { group ->
                val legs = group.legs.filter { it.id !in reviewIds }
                if (legs.isEmpty()) null else group.copy(legs = legs)
            }
        val nextLeg =
            visibleGroups
                .flatMap { it.legs }
                .filter { leg -> !leg.isCancelled && leg.departureAt?.let { it > now } == true }
                .minByOrNull { it.departureAt!! }
        val upcomingGroups =
            visibleGroups.filter {
                it.status == JourneyStatus.UPCOMING || it.status == JourneyStatus.IN_PROGRESS
            }
        val isEmpty = tickets.isEmpty()
        return HomeUiState(
            isLoading = false,
            subtitle = if (isEmpty) null else Formatters.formatHomeSubtitle(istDate(now), upcomingGroups.size),
            nextJourney = nextLeg?.let { nextJourney(it, now) },
            needsReview = reviewTickets.map(::needsReviewRow),
            upcoming = upcomingGroups.map { tripCard(it, nextLeg?.id) },
            pastByMonth = pastByMonth(visibleGroups.filter { it.status == JourneyStatus.PAST }),
            isEmpty = isEmpty,
        )
    }

    /** Required fields of [ticket] that are blank, in the canonical [REQUIRED_TICKET_FIELDS] order. */
    fun missingRequiredFields(ticket: Ticket): List<TicketField> =
        REQUIRED_TICKET_FIELDS.filter { field ->
            when (field) {
                TicketField.PNR -> ticket.pnr.isNullOrBlank()
                TicketField.TRAIN_NUMBER -> ticket.trainNumber.isNullOrBlank()
                TicketField.FROM -> ticket.from?.code.isNullOrBlank()
                TicketField.TO -> ticket.to?.code.isNullOrBlank()
                TicketField.DEPARTURE_DATE -> ticket.departureAt == null
                else -> false
            }
        }

    private fun needsReviewRow(ticket: Ticket): NeedsReviewUi {
        val missing = missingRequiredFields(ticket)
        return NeedsReviewUi(
            ticketId = ticket.id,
            fileName = ticket.originalFileName?.takeIf { it.isNotBlank() },
            missing = missing.take(MAX_MISSING_SHOWN),
            moreMissing = (missing.size - MAX_MISSING_SHOWN).coerceAtLeast(0),
        )
    }

    private fun nextJourney(leg: Ticket, now: Instant): NextJourneyUi {
        val dep = leg.departureAt!!
        val arr = leg.arrivalAt
        val from = leg.from ?: leg.boarding
        val trainName = DisplayText.forTicket(leg.trainName, leg).ifBlank { null }
        return NextJourneyUi(
            ticketId = leg.id,
            overline = Formatters.formatHeroOverline(now, dep),
            countdown = Formatters.formatCountdownIn(now, dep),
            fromCode = from?.code.orEmpty(),
            fromName = DisplayText.forTicket(from?.name, leg),
            departureTime = Formatters.formatTime(dep),
            toCode = leg.to?.code.orEmpty(),
            toName = DisplayText.forTicket(leg.to?.name, leg),
            arrivalTime = arr?.let { Formatters.formatTime(it) } ?: "—",
            dayOffset = arr?.let { Formatters.formatDayOffset(dep, it) },
            duration = arr?.let { Formatters.formatDuration(dep, it) },
            trainLine = listOfNotNull(leg.trainNumber, trainName, classText(leg)).joinToString(SEP),
            seat = seatPanel(leg),
            pdfFileName = leg.pdfFileName,
        )
    }

    private fun classText(leg: Ticket): String? =
        DisplayText.forTicket(leg.classLabel, leg).ifBlank { null } ?: leg.travelClass?.takeIf { it.isNotBlank() }

    private fun activePassengers(ticket: Ticket) =
        ticket.passengers.filter { StatusTones.kindOf(it.current) != StatusKind.CANCELLED }

    /** "WL 12" / "RAC 23" for waitlist and RAC statuses (number optional). */
    internal fun statusText(status: BerthStatus, kind: StatusKind): String {
        val label = if (kind == StatusKind.RAC) "RAC" else "WL"
        val number = status.waitlistNumber ?: if (kind == StatusKind.RAC) status.berthNumber else null
        return if (number != null) "$label $number" else label
    }

    private fun seatPanel(leg: Ticket): SeatPanelUi? {
        val active = activePassengers(leg)
        val first = active.firstOrNull() ?: return null
        val status = first.current
        val kind = StatusTones.kindOf(status)
        val waitingKind = kind == StatusKind.WAITLIST || kind == StatusKind.RAC
        return SeatPanelUi(
            passengerName = DisplayText.forTicket(first.name, leg),
            othersCount = active.size - 1,
            status = status,
            statusKind = kind,
            coach = status?.coach?.takeIf { it.isNotBlank() } ?: "—",
            berth = status?.berthNumber?.toString() ?: "—",
            berthType = DisplayText.titleCase(status?.berthType).ifBlank { "—" },
            bigValue = if (waitingKind && status != null) statusText(status, kind) else null,
        )
    }

    private fun legSeat(leg: Ticket): LegSeatUi {
        if (leg.isCancelled) return LegSeatUi.None
        val active = activePassengers(leg)
        val first = active.firstOrNull() ?: return LegSeatUi.None
        val status = first.current ?: return LegSeatUi.None
        val kind = StatusTones.kindOf(status)
        return when {
            kind == StatusKind.WAITLIST || kind == StatusKind.RAC ->
                LegSeatUi(
                    kind = LegSeatKind.STATUS_CHIP,
                    primary = statusText(status, kind),
                    othersCount = active.size - 1,
                    statusKind = kind,
                )
            !status.coach.isNullOrBlank() ->
                LegSeatUi(
                    kind = LegSeatKind.SEAT,
                    primary = listOfNotNull(status.coach, status.berthNumber?.toString()).joinToString(SEP),
                    berthType = DisplayText.titleCase(status.berthType),
                    othersCount = active.size - 1,
                    statusKind = kind,
                )
            else -> LegSeatUi.None
        }
    }

    private fun legRow(leg: Ticket, nextLegId: String?): LegRowUi {
        val from = leg.from ?: leg.boarding
        val boarding = leg.boarding
        val boards =
            if (boarding != null && from != null && !boarding.code.equals(from.code, ignoreCase = true)) {
                "boards ${boarding.code}"
            } else {
                null
            }
        val meta =
            listOfNotNull(
                leg.departureAt?.let { Formatters.formatTime(it) },
                leg.trainNumber?.takeIf { it.isNotBlank() },
                leg.travelClass?.takeIf { it.isNotBlank() },
                boards,
            ).joinToString(SEP)
        return LegRowUi(
            id = leg.id,
            date = legDate(leg),
            highlighted = leg.id == nextLegId,
            route = "${from?.code ?: "?"} → ${leg.to?.code ?: "?"}",
            meta = meta,
            cancelled = leg.isCancelled,
            seat = legSeat(leg),
        )
    }

    private fun legDate(leg: Ticket): LocalDate? = leg.departureAt?.let(::istDate) ?: leg.startDate

    private fun istDate(instant: Instant): LocalDate = instant.atZone(IrctcDates.IST).toLocalDate()

    private fun dateRange(group: JourneyGroup): String {
        val dates = group.legs.mapNotNull(::legDate)
        val start = dates.minOrNull() ?: return ""
        val end = dates.maxOrNull() ?: return ""
        return if (group.legs.size <= 1) Formatters.formatDate(start) else Formatters.formatDateRange(start, end)
    }

    private fun tripCard(group: JourneyGroup, nextLegId: String?): TripCardUi {
        val dates = group.legs.mapNotNull(::legDate)
        val range = dateRange(group)
        val dateText =
            if (group.legs.size > 1 && dates.isNotEmpty()) {
                range + SEP + Formatters.formatTripLength(dates.min(), dates.max())
            } else {
                range
            }
        return TripCardUi(
            key = group.key,
            anchorTicketId = group.legs.first().id,
            kind = group.kind,
            inProgress = group.status == JourneyStatus.IN_PROGRESS,
            dateRange = dateText,
            title = if (group.kind == JourneyKind.ONE_WAY) null else CityNames.routeTitle(group),
            legs = group.legs.map { legRow(it, nextLegId) },
        )
    }

    private fun pastByMonth(past: List<JourneyGroup>): List<PastMonthUi> =
        past
            .mapNotNull { group -> group.legs.firstNotNullOfOrNull(::legDate)?.let { it to group } }
            .groupBy { (date, _) -> YearMonth.from(date) }
            .toList()
            .sortedByDescending { it.first }
            .map { (month, entries) ->
                PastMonthUi(
                    header = Formatters.formatMonthHeaderUpper(month.atDay(1)),
                    rows = entries.sortedByDescending { it.first }.map { pastRow(it.second) },
                )
            }

    private fun pastRow(group: JourneyGroup): PastTripUi {
        val first = group.legs.first()
        val oneWay = group.kind == JourneyKind.ONE_WAY
        val trains = group.legs.mapNotNull { it.trainNumber?.takeIf(String::isNotBlank) }.distinct().joinToString(" / ")
        val dateText = if (oneWay) legDate(first)?.let(Formatters::formatDate).orEmpty() else dateRange(group)
        val title =
            if (oneWay) {
                "${(first.from ?: first.boarding)?.code ?: "?"} → ${first.to?.code ?: "?"}"
            } else {
                CityNames.routeTitle(group)
            }
        return PastTripUi(
            key = group.key,
            anchorTicketId = first.id,
            kind = group.kind,
            title = title,
            subtitle = listOf(dateText, trains).filter { it.isNotBlank() }.joinToString(SEP),
            cancelled = oneWay && first.isCancelled,
        )
    }
}
