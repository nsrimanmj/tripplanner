package com.example.tripchecker.ui.trip

import com.example.tripchecker.domain.model.BerthStatus
import com.example.tripchecker.domain.model.ExtractionMethod
import com.example.tripchecker.domain.model.JourneyGroup
import com.example.tripchecker.domain.model.JourneyKind
import com.example.tripchecker.domain.model.JourneyStatus
import com.example.tripchecker.domain.model.Passenger
import com.example.tripchecker.domain.model.Station
import com.example.tripchecker.domain.model.Ticket
import com.example.tripchecker.ui.common.StatusKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class TripDetailUiMapperTest {
    private val now = Instant.parse("2026-08-01T10:00:00Z")

    private fun ticket(
        id: String = "t",
        from: Station? = Station("SNSI", "SAINAGAR SHIRDI"),
        boarding: Station? = null,
        to: Station? = Station("BMT", "BEGAMPET"),
        dep: String? = "2026-08-03T11:30:00Z",
        arr: String? = "2026-08-04T02:29:00Z",
        fare: Long? = 149405,
        manual: Boolean = false,
        autoPairDisabled: Boolean = false,
        method: ExtractionMethod = ExtractionMethod.TEXT_LAYER,
        passengers: List<Passenger> = emptyList(),
    ) = Ticket(
        id = id,
        pnr = "8751652729",
        trainNumber = "17001",
        trainName = "SNSI SC EXPRESS",
        from = from,
        boarding = boarding,
        to = to,
        startDate = null,
        departureAt = dep?.let(Instant::parse),
        arrivalAt = arr?.let(Instant::parse),
        travelClass = "3A",
        classLabel = "THIRD AC",
        quota = "TQ",
        quotaLabel = "TATKAL",
        distanceKm = 772,
        bookedAt = null,
        transactionId = null,
        totalFarePaise = fare,
        passengers = passengers,
        pdfFileName = null,
        pdfSha256 = null,
        originalFileName = null,
        extractionMethod = method,
        needsReview = false,
        linkGroupId = if (manual) "g" else null,
        autoPairDisabled = autoPairDisabled,
        isCancelled = false,
        notes = null,
        createdAt = now,
        updatedAt = now,
    )

    private fun ret(
        dep: String = "2026-08-10T16:10:00Z",
        arr: String? = null,
        fare: Long? = 216360,
    ) = ticket(
        id = "r",
        from = Station("SC", "SECUNDERABAD JN"),
        boarding = Station("LPI", "LINGAMPALLI"),
        to = Station("SNSI", "SAINAGAR SHIRDI"),
        dep = dep,
        arr = arr,
        fare = fare,
    )

    private fun group(
        kind: JourneyKind,
        vararg legs: Ticket,
        manual: Boolean = false,
        status: JourneyStatus = JourneyStatus.UPCOMING,
    ) = JourneyGroup("k", kind, legs.toList(), manual, status)

    private val roundTrip get() = group(JourneyKind.ROUND_TRIP, ticket(), ret())

    @Test
    fun `round trip subtitle has date range, length and summed fare`() {
        assertEquals(
            "Mon, 3 Aug – Mon, 10 Aug · 8 days · ₹3,657.65 total",
            TripDetailUiMapper.headerSubtitle(roundTrip),
        )
    }

    @Test
    fun `round trip subtitle omits total when no leg has a fare`() {
        val g = group(JourneyKind.ROUND_TRIP, ticket(fare = null), ret(fare = null))
        assertEquals("Mon, 3 Aug – Mon, 10 Aug · 8 days", TripDetailUiMapper.headerSubtitle(g))
    }

    @Test
    fun `subtitle total sums only legs that have a fare`() {
        val g = group(JourneyKind.ROUND_TRIP, ticket(fare = 149405), ret(fare = null))
        assertEquals("Mon, 3 Aug – Mon, 10 Aug · 8 days · ₹1,494.05 total", TripDetailUiMapper.headerSubtitle(g))
    }

    @Test
    fun `one way subtitle is the date and train number`() {
        assertEquals("Mon, 3 Aug · 17001", TripDetailUiMapper.headerSubtitle(group(JourneyKind.ONE_WAY, ticket())))
    }

    @Test
    fun `header has kind status title and pairing chip`() {
        val h = TripDetailUiMapper.header(roundTrip)
        assertEquals(JourneyKind.ROUND_TRIP, h.kind)
        assertEquals(TripStatusLabel.UPCOMING, h.status)
        assertEquals(PairingChip.AUTO_PAIRED, h.pairingChip)
        assertEquals("Shirdi ⇄ Hyderabad", h.title)
    }

    @Test
    fun `manual group shows linked manually chip and one way has no pairing chip`() {
        val manual =
            TripDetailUiMapper.header(
                group(JourneyKind.CUSTOM, ticket(), ret(), manual = true, status = JourneyStatus.PAST),
            )
        assertEquals(PairingChip.LINKED_MANUALLY, manual.pairingChip)
        assertEquals(TripStatusLabel.PAST, manual.status)
        assertNull(TripDetailUiMapper.header(group(JourneyKind.ONE_WAY, ticket())).pairingChip)
    }

    @Test
    fun `stay label counts days in the first leg destination city`() {
        assertEquals("6 days in Hyderabad", TripDetailUiMapper.stayLabel(ticket(), ret()))
    }

    @Test
    fun `stay label uses departure date when arrival is missing and says same-day connection for zero`() {
        val a = ticket(arr = null, dep = "2026-08-03T11:30:00Z")
        val b = ret(dep = "2026-08-03T14:00:00Z")
        assertEquals("Same-day connection", TripDetailUiMapper.stayLabel(a, b))
        assertEquals("1 day in Hyderabad", TripDetailUiMapper.stayLabel(a, ret(dep = "2026-08-04T14:00:00Z")))
    }

    @Test
    fun `stays has one entry per gap`() {
        assertEquals(listOf("6 days in Hyderabad"), TripDetailUiMapper.stays(roundTrip))
        assertTrue(TripDetailUiMapper.stays(group(JourneyKind.ONE_WAY, ticket())).isEmpty())
    }

    @Test
    fun `leg roles follow the group kind`() {
        assertEquals(LegRole.Outbound, TripDetailUiMapper.legRole(roundTrip, 0))
        assertEquals(LegRole.Return, TripDetailUiMapper.legRole(roundTrip, 1))
        assertEquals(LegRole.OneWay, TripDetailUiMapper.legRole(group(JourneyKind.ONE_WAY, ticket()), 0))
        assertEquals(LegRole.Numbered(2), TripDetailUiMapper.legRole(group(JourneyKind.CUSTOM, ticket(), ret()), 1))
    }

    @Test
    fun `auto round trip offers unlink only`() {
        val g = roundTrip
        assertEquals(TripMenuUi(unlink = true, linkReturn = false, resetPairing = false), TripDetailUiMapper.menu(g))
        val c = TripDetailUiMapper.pairingCard(g)
        assertEquals(PairingVariant.AUTO, c.variant)
        assertEquals("Hyderabad", c.destinationCity)
        assertTrue(c.showUnlink)
        assertFalse(c.showReset)
        assertFalse(c.showLinkReturn)
    }

    @Test
    fun `manual group offers unlink and reset`() {
        val g = group(JourneyKind.ROUND_TRIP, ticket(manual = true), ret(), manual = true)
        assertEquals(TripMenuUi(unlink = true, linkReturn = false, resetPairing = true), TripDetailUiMapper.menu(g))
        val c = TripDetailUiMapper.pairingCard(g)
        assertEquals(PairingVariant.MANUAL, c.variant)
        assertTrue(c.showUnlink)
        assertTrue(c.showReset)
    }

    @Test
    fun `one way offers link return, and reset only when auto pairing is disabled`() {
        val plain = group(JourneyKind.ONE_WAY, ticket())
        assertEquals(TripMenuUi(unlink = false, linkReturn = true, resetPairing = false), TripDetailUiMapper.menu(plain))
        val c = TripDetailUiMapper.pairingCard(plain)
        assertEquals(PairingVariant.ONE_WAY, c.variant)
        assertTrue(c.showLinkReturn)
        assertFalse(c.showReset)

        val disabled = group(JourneyKind.ONE_WAY, ticket(autoPairDisabled = true))
        assertTrue(TripDetailUiMapper.menu(disabled).resetPairing)
        assertTrue(TripDetailUiMapper.pairingCard(disabled).showReset)
    }

    @Test
    fun `leg title, chips and ocr flag`() {
        val leg = TripDetailUiMapper.leg(ticket(method = ExtractionMethod.OCR), LegRole.Outbound)
        assertEquals("17001 · SNSI SC Express", leg.title)
        assertEquals("Third AC · 3A", leg.classChip)
        assertEquals("Tatkal · TQ", leg.quotaChip)
        assertEquals("MON, 3 AUG", leg.dateLabel)
        assertTrue(leg.viaOcr)
        assertEquals("₹1,494.05", leg.totalFare)
        assertFalse(TripDetailUiMapper.leg(ticket(), LegRole.Outbound).viaOcr)
    }

    @Test
    fun `chip falls back to whichever part exists`() {
        assertEquals("3A", TripDetailUiMapper.chip(null, "3a"))
        assertEquals("Tatkal", TripDetailUiMapper.chip("Tatkal", null))
        assertNull(TripDetailUiMapper.chip(" ", ""))
    }

    @Test
    fun `timeline shows duration and distance with times and dates`() {
        val t = TripDetailUiMapper.timeline(ticket())
        assertEquals("17:00", t.departure.time)
        assertEquals("Mon 3 Aug", t.departure.date)
        assertEquals("SNSI", t.departure.code)
        assertEquals("Sainagar Shirdi", t.departure.name)
        assertNull(t.bookedFromCode)
        assertEquals("14h 59m · 772 km", t.middleLabel)
        assertEquals("07:59", t.arrival.time)
        assertEquals("Tue 4 Aug", t.arrival.date)
        assertEquals("Begampet", t.arrival.name)
        assertFalse(t.arrivalNotAvailable)
    }

    @Test
    fun `timeline with boarding that differs shows boarding station and booked-from code`() {
        val t = TripDetailUiMapper.timeline(ret())
        assertEquals("LPI", t.departure.code)
        assertEquals("Lingampalli", t.departure.name)
        assertEquals("SC", t.bookedFromCode)
    }

    @Test
    fun `timeline with same boarding and from has no boarding chip`() {
        val t = TripDetailUiMapper.timeline(ticket(boarding = Station("snsi", "SAINAGAR SHIRDI")))
        assertNull(t.bookedFromCode)
    }

    @Test
    fun `timeline without arrival is arrival not available with distance only`() {
        val t = TripDetailUiMapper.timeline(ret())
        assertTrue(t.arrivalNotAvailable)
        assertNull(t.arrival.time)
        assertEquals("772 km", t.middleLabel)
    }

    @Test
    fun `confirmed passenger shows coach and berth with berth type`() {
        val row =
            TripDetailUiMapper.passengerRow(
                Passenger("p", 1, "MADHURI", 26, "F", "CNF/B4/33", "CNF/B4/33", BerthStatus("CNF", "B4", 33, "LOWER")),
            )
        assertEquals("M", row.initial)
        assertEquals("Madhuri", row.name)
        assertEquals("26 · F · booked CNF/B4/33", row.meta)
        assertEquals("B4 · 33", row.primary)
        assertEquals("Lower", row.secondary)
        assertEquals(StatusKind.CONFIRMED, row.kind)
        assertFalse(row.cancelled)
    }

    @Test
    fun `side berth types are sentence cased and short codes stay upper`() {
        fun type(raw: String) =
            TripDetailUiMapper.passengerRow(
                Passenger("p", 1, "A", null, null, null, null, BerthStatus("RAC", "S2", 71, raw)),
            ).secondary
        assertEquals("Side lower", type("SIDE LOWER"))
        assertEquals("LB", type("LB"))
    }

    @Test
    fun `waitlisted passenger shows code and number without berth type`() {
        val row =
            TripDetailUiMapper.passengerRow(
                Passenger("p", 1, "ANITA", 31, "F", "RLWL/12", "RLWL/5", BerthStatus("RLWL", waitlistNumber = 5)),
            )
        assertEquals("RLWL 5", row.primary)
        assertNull(row.secondary)
        assertEquals(StatusKind.WAITLIST, row.kind)
    }

    @Test
    fun `cancelled passenger has only the cancelled status`() {
        val row =
            TripDetailUiMapper.passengerRow(
                Passenger("p", 1, "MEERA", 60, "F", "WL/40", "CAN", BerthStatus("CAN")),
            )
        assertNull(row.primary)
        assertNull(row.secondary)
        assertTrue(row.cancelled)
    }

    @Test
    fun `passenger with unparsed status falls back to raw text and no chip`() {
        val row = TripDetailUiMapper.passengerRow(Passenger("p", 1, "X", null, null, null, "CNF/B4", null))
        assertEquals("CNF/B4", row.primary)
        assertNull(row.status)
        assertEquals("", row.meta)
    }

    @Test
    fun `link sheet names the destination city and ranks likely returns first with relative days`() {
        val anchor = ticket(id = "a")
        val likely =
            ticket(
                id = "l",
                from = Station("BMT", "BEGAMPET"),
                to = Station("SNSI", "SAINAGAR SHIRDI"),
                dep = "2026-08-10T16:10:00Z",
            )
        val other = ticket(id = "o", dep = "2026-08-06T11:30:00Z")
        val groups = listOf(anchor, other, likely).map { group(JourneyKind.ONE_WAY, it) }
        val sheet = TripDetailUiMapper.linkSheet(anchor, groups)
        assertEquals("Hyderabad", sheet.destinationCity)
        assertEquals(listOf("l", "o"), sheet.candidates.map { it.ticketId })
        val first = sheet.candidates.first()
        assertTrue(first.likelyReturn)
        assertEquals("BMT → SNSI", first.route)
        assertEquals("Mon, 10 Aug · 21:40 · 17001", first.meta)
        assertEquals("7 days later", first.relativeDays)
        assertFalse(sheet.candidates[1].likelyReturn)
        assertEquals("3 days later", sheet.candidates[1].relativeDays)
    }
}
