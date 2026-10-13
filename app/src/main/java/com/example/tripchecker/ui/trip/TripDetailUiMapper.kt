package com.example.tripchecker.ui.trip

import com.example.tripchecker.domain.grouping.CityNames
import com.example.tripchecker.domain.grouping.LinkReturnCandidates
import com.example.tripchecker.domain.model.BerthStatus
import com.example.tripchecker.domain.model.ExtractionMethod
import com.example.tripchecker.domain.model.JourneyGroup
import com.example.tripchecker.domain.model.JourneyKind
import com.example.tripchecker.domain.model.JourneyStatus
import com.example.tripchecker.domain.model.Passenger
import com.example.tripchecker.domain.model.Ticket
import com.example.tripchecker.domain.text.DisplayText
import com.example.tripchecker.extraction.parser.IrctcDates
import com.example.tripchecker.ui.common.Formatters
import com.example.tripchecker.ui.common.StatusKind
import com.example.tripchecker.ui.common.StatusTones
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Role of a leg inside its trip, used for the overline ("OUTBOUND", "RETURN", "ONE WAY", "LEG 2"). */
sealed interface LegRole {
    /** First leg of a round trip. */
    data object Outbound : LegRole

    /** Second leg of a round trip. */
    data object Return : LegRole

    /** The only leg of a one-way trip. */
    data object OneWay : LegRole

    /** Leg [number] (1-based) of a custom chain. */
    data class Numbered(val number: Int) : LegRole
}

/** Status chip content of the trip header. */
enum class TripStatusLabel { UPCOMING, IN_PROGRESS, PAST }

/** Pairing chip content of the trip header; there is no chip for one-way trips. */
enum class PairingChip { AUTO_PAIRED, LINKED_MANUALLY }

/** Header (chips, title, subtitle) of the trip detail screen. */
data class TripHeaderUi(
    val kind: JourneyKind,
    val status: TripStatusLabel,
    val pairingChip: PairingChip?,
    val title: String,
    val subtitle: String,
)

/** Items of the "Trip options" overflow menu. */
data class TripMenuUi(
    val unlink: Boolean,
    val linkReturn: Boolean,
    val resetPairing: Boolean,
) {
    val isEmpty: Boolean get() = !unlink && !linkReturn && !resetPairing
}

/** Variant of the pairing footer card. */
enum class PairingVariant { AUTO, MANUAL, ONE_WAY }

/** Pairing footer card: the variant, the city the return leaves from, and which buttons it offers. */
data class PairingCardUi(
    val variant: PairingVariant,
    val destinationCity: String,
    val showUnlink: Boolean,
    val showLinkReturn: Boolean,
    val showReset: Boolean,
)

/** One timeline stop (departure or arrival). */
data class TimelineStopUi(
    val time: String?,
    val date: String?,
    val code: String,
    val name: String,
)

/** Route timeline of a leg. [bookedFromCode] is set when the boarding station differs from the booked-from one. */
data class TimelineUi(
    val departure: TimelineStopUi,
    val bookedFromCode: String?,
    val middleLabel: String?,
    val arrival: TimelineStopUi,
    val arrivalNotAvailable: Boolean,
)

/** One passenger row: [primary]/[secondary] are the prominent current-status texts on the right. */
data class PassengerRowUi(
    val id: String,
    val initial: String,
    val name: String,
    val meta: String,
    val primary: String?,
    val secondary: String?,
    val status: BerthStatus?,
    val kind: StatusKind,
) {
    val cancelled: Boolean get() = kind == StatusKind.CANCELLED
}

/** Everything a `LegCard` renders. [ticket] is kept for actions (open, share, edit, delete). */
data class LegUi(
    val ticket: Ticket,
    val role: LegRole,
    val dateLabel: String?,
    val title: String,
    val viaOcr: Boolean,
    val timeline: TimelineUi,
    val classChip: String?,
    val quotaChip: String?,
    val pnr: String?,
    val passengers: List<PassengerRowUi>,
    val totalFare: String?,
    val cancelled: Boolean,
)

/** One row of the link-return sheet. */
data class LinkCandidateUi(
    val ticketId: String,
    val route: String,
    val meta: String,
    val likelyReturn: Boolean,
    val relativeDays: String?,
)

/** Link-return sheet content: the candidates and the city named in its subtitle. */
data class LinkSheetUi(
    val destinationCity: String,
    val candidates: List<LinkCandidateUi>,
)

/** Pure mapping from journey groups and tickets to trip detail display models. */
object TripDetailUiMapper {
    private val zone = IrctcDates.IST
    private val stopDateFmt = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)

    /** Header chips, city title and subtitle for [group]. */
    fun header(group: JourneyGroup): TripHeaderUi =
        TripHeaderUi(
            kind = group.kind,
            status =
                when (group.status) {
                    JourneyStatus.UPCOMING -> TripStatusLabel.UPCOMING
                    JourneyStatus.IN_PROGRESS -> TripStatusLabel.IN_PROGRESS
                    JourneyStatus.PAST -> TripStatusLabel.PAST
                },
            pairingChip = pairingChip(group),
            title = CityNames.routeTitle(group),
            subtitle = headerSubtitle(group),
        )

    private fun pairingChip(group: JourneyGroup): PairingChip? =
        when (group.kind) {
            JourneyKind.ONE_WAY -> null
            else -> if (group.isManual) PairingChip.LINKED_MANUALLY else PairingChip.AUTO_PAIRED
        }

    /**
     * Multi-leg: "Mon, 3 Aug – Mon, 10 Aug · 8 days · ₹3,657.65 total" (total omitted when no leg has
     * a fare). One-way: "Sat, 22 Aug · 17001".
     */
    fun headerSubtitle(group: JourneyGroup): String {
        val legs = group.legs
        val firstDate = legs.firstNotNullOfOrNull { departureDate(it) }
        if (legs.size <= 1) {
            val train = legs.firstOrNull()?.trainNumber?.takeIf { it.isNotBlank() }
            return listOfNotNull(firstDate?.let(Formatters::formatDate), train).joinToString(" · ")
        }
        val lastDate = legs.reversed().firstNotNullOfOrNull { arrivalOrDepartureDate(it) } ?: firstDate
        val parts = ArrayList<String>()
        if (firstDate != null && lastDate != null) {
            parts += Formatters.formatDateRangeLong(firstDate, lastDate)
            parts += Formatters.formatTripLength(firstDate, lastDate)
        } else if (firstDate != null) {
            parts += Formatters.formatDate(firstDate)
        }
        val fares = legs.mapNotNull { it.totalFarePaise }
        if (fares.isNotEmpty()) parts += "${Formatters.formatMoneyPaise(fares.sum())} total"
        return parts.joinToString(" · ")
    }

    /** Items of the Trip options menu for [group]. */
    fun menu(group: JourneyGroup): TripMenuUi =
        TripMenuUi(
            unlink = group.kind == JourneyKind.ROUND_TRIP || group.kind == JourneyKind.CUSTOM,
            linkReturn = group.kind == JourneyKind.ONE_WAY,
            resetPairing = canReset(group),
        )

    private fun canReset(group: JourneyGroup): Boolean = group.isManual || group.legs.any { it.autoPairDisabled }

    /** Footer card variant and buttons for [group]. */
    fun pairingCard(group: JourneyGroup): PairingCardUi {
        val city = CityNames.cityLabel(group.legs.firstOrNull()?.to).orEmpty()
        return when {
            group.kind == JourneyKind.ONE_WAY ->
                PairingCardUi(PairingVariant.ONE_WAY, city, false, true, canReset(group))
            group.isManual -> PairingCardUi(PairingVariant.MANUAL, city, true, false, true)
            else -> PairingCardUi(PairingVariant.AUTO, city, true, false, false)
        }
    }

    /** Role of the leg at [index] in [group]. */
    fun legRole(group: JourneyGroup, index: Int): LegRole =
        when (group.kind) {
            JourneyKind.ROUND_TRIP -> if (index == 0) LegRole.Outbound else LegRole.Return
            JourneyKind.ONE_WAY -> LegRole.OneWay
            JourneyKind.CUSTOM -> LegRole.Numbered(index + 1)
        }

    /** Stay label between [previous] and [next] ("6 days in Hyderabad"), or null when dates are missing. */
    fun stayLabel(previous: Ticket, next: Ticket): String? {
        val from = previous.arrivalAt ?: previous.departureAt ?: return null
        val to = next.departureAt ?: return null
        val city = CityNames.cityLabel(previous.to) ?: CityNames.cityLabel(next.origin).orEmpty()
        return Formatters.formatStay(from, to, city)
    }

    /** Stay labels for each gap between consecutive legs (size = legs - 1). */
    fun stays(group: JourneyGroup): List<String?> = group.legs.zipWithNext { a, b -> stayLabel(a, b) }

    /** Display models for all legs of [group]. */
    fun legs(group: JourneyGroup): List<LegUi> = group.legs.mapIndexed { i, t -> leg(t, legRole(group, i)) }

    /** Display model for one [ticket] shown with [role]. */
    fun leg(ticket: Ticket, role: LegRole): LegUi =
        LegUi(
            ticket = ticket,
            role = role,
            dateLabel = departureDate(ticket)?.let { Formatters.formatDate(it).uppercase(Locale.ENGLISH) },
            title = legTitle(ticket),
            viaOcr = ticket.extractionMethod == ExtractionMethod.OCR,
            timeline = timeline(ticket),
            classChip = chip(DisplayText.forTicket(ticket.classLabel, ticket), ticket.travelClass),
            quotaChip = chip(DisplayText.forTicket(ticket.quotaLabel, ticket), ticket.quota),
            pnr = ticket.pnr?.takeIf { it.isNotBlank() },
            passengers = ticket.passengers.map { passengerRow(it) },
            totalFare = ticket.totalFarePaise?.let(Formatters::formatMoneyPaise),
            cancelled = ticket.isCancelled,
        )

    /** "17001 · SNSI SC Express" (either part optional). */
    fun legTitle(ticket: Ticket): String =
        listOfNotNull(
            ticket.trainNumber?.takeIf { it.isNotBlank() },
            DisplayText.forTicket(ticket.trainName, ticket).ifEmpty { null },
        ).joinToString(" · ")

    /** "Third AC · 3A": label and code joined, or whichever one exists; null if both are blank. */
    fun chip(label: String?, code: String?): String? {
        val l = label?.takeIf { it.isNotBlank() }
        val c = code?.takeIf { it.isNotBlank() }?.trim()?.uppercase(Locale.ENGLISH)
        return when {
            l != null && c != null -> if (l.equals(c, ignoreCase = true)) c else "$l · $c"
            else -> l ?: c
        }
    }

    /** Timeline model: the boarding station is on top when it differs from the booked-from station. */
    fun timeline(ticket: Ticket): TimelineUi {
        val boarding = ticket.boarding
        val from = ticket.from
        val boardingDiffers = boarding != null && from != null && !boarding.code.equals(from.code, ignoreCase = true)
        val top = ticket.origin
        val dep = ticket.departureAt
        val arr = ticket.arrivalAt
        val duration = if (dep != null && arr != null) Formatters.formatDuration(dep, arr) else null
        val distance = ticket.distanceKm?.let { "$it km" }
        return TimelineUi(
            departure =
                TimelineStopUi(
                    time = dep?.let(Formatters::formatTime),
                    date = dep?.let(::stopDate),
                    code = top?.code.orEmpty().ifEmpty { "—" },
                    name = DisplayText.forTicket(top?.name, ticket),
                ),
            bookedFromCode = if (boardingDiffers) from?.code else null,
            middleLabel = listOfNotNull(duration, distance).joinToString(" · ").ifEmpty { null },
            arrival =
                TimelineStopUi(
                    time = arr?.let(Formatters::formatTime),
                    date = arr?.let(::stopDate),
                    code = ticket.to?.code.orEmpty().ifEmpty { "—" },
                    name = DisplayText.forTicket(ticket.to?.name, ticket),
                ),
            arrivalNotAvailable = arr == null,
        )
    }

    private fun stopDate(instant: Instant): String = instant.atZone(zone).format(stopDateFmt)

    private fun departureDate(ticket: Ticket): LocalDate? = ticket.departureAt?.atZone(zone)?.toLocalDate()

    private fun arrivalOrDepartureDate(ticket: Ticket): LocalDate? =
        (ticket.arrivalAt ?: ticket.departureAt)?.atZone(zone)?.toLocalDate()

    /** Passenger row: title-cased name, meta "26 · F · booked CNF/B4/33" and the prominent current status. */
    fun passengerRow(p: Passenger): PassengerRowUi {
        val current = p.current
        val kind = StatusTones.kindOf(current)
        val name = DisplayText.titleCase(p.name)
        val meta =
            listOfNotNull(
                p.age?.toString(),
                p.gender?.takeIf { it.isNotBlank() },
                p.bookingStatusRaw?.takeIf { it.isNotBlank() }?.let { "booked $it" },
            ).joinToString(" · ")
        var primary: String? = null
        var secondary: String? = null
        if (kind != StatusKind.CANCELLED) {
            val waitlist = current?.waitlistNumber
            val coach = current?.coach
            val berth = current?.berthNumber
            when {
                current != null && waitlist != null -> primary = "${current.code} $waitlist"
                current != null && coach != null && berth != null -> {
                    primary = "$coach · $berth"
                    secondary = current.berthType?.let(::sentenceCase)
                }
                else -> primary = p.currentStatusRaw?.takeIf { it.isNotBlank() }
            }
        }
        return PassengerRowUi(
            id = p.id,
            initial = name.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "?",
            name = name,
            meta = meta,
            primary = primary,
            secondary = secondary,
            status = current,
            kind = kind,
        )
    }

    private fun sentenceCase(raw: String): String {
        val t = raw.trim()
        if (t.length <= 3 && t.all { it.isLetter() }) return t.uppercase(Locale.ENGLISH)
        return t.lowercase(Locale.ENGLISH).replaceFirstChar { it.uppercaseChar() }
    }

    /** Link sheet model for [anchor]; candidates come from [LinkReturnCandidates.findRanked]. */
    fun linkSheet(anchor: Ticket, groups: List<JourneyGroup>): LinkSheetUi {
        val anchorDate = departureDate(anchor)
        val rows =
            LinkReturnCandidates.findRanked(anchor, groups).map { c ->
                val t = c.ticket
                val date = departureDate(t)
                LinkCandidateUi(
                    ticketId = t.id,
                    route = "${t.origin?.code ?: "—"} → ${t.to?.code ?: "—"}",
                    meta =
                        listOfNotNull(
                            date?.let(Formatters::formatDate),
                            t.departureAt?.let(Formatters::formatTime),
                            t.trainNumber?.takeIf { it.isNotBlank() },
                        ).joinToString(" · "),
                    likelyReturn = c.likelyReturn,
                    relativeDays =
                        if (anchorDate != null && date != null) {
                            Formatters.formatRelativeDays(anchorDate, date)
                        } else {
                            null
                        },
                )
            }
        return LinkSheetUi(destinationCity = CityNames.cityLabel(anchor.to).orEmpty(), candidates = rows)
    }
}
