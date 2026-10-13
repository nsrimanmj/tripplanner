package com.example.tripchecker.ui.home

import com.example.tripchecker.domain.grouping.TripGrouper
import com.example.tripchecker.domain.model.BerthStatus
import com.example.tripchecker.domain.model.ExtractionMethod
import com.example.tripchecker.domain.model.JourneyKind
import com.example.tripchecker.domain.model.Passenger
import com.example.tripchecker.domain.model.Station
import com.example.tripchecker.domain.model.Ticket
import com.example.tripchecker.extraction.parser.TicketField
import com.example.tripchecker.ui.common.StatusKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class HomeUiMapperTest {
    private val ist = ZoneId.of("Asia/Kolkata")

    private fun map(tickets: List<Ticket>, now: Instant): HomeUiState =
        HomeUiMapper.map(TripGrouper.group(tickets, now), tickets, now)

    @Test
    fun `needs review tickets excluded from upcoming and hero`() {
        val now = instant(2026, 8, 1, 12, 0)
        val good = ticket("g", needsReview = false, dep = instant(2026, 8, 10, 10, 0))
        val bad = ticket("b", needsReview = true, dep = instant(2026, 8, 5, 10, 0))
        val groups = TripGrouper.group(listOf(good, bad), now)
        val state = HomeUiMapper.map(groups, listOf(good, bad), now)
        assertEquals(1, state.needsReview.size)
        assertEquals("g", state.nextJourney?.ticketId)
        assertTrue(state.upcoming.all { group -> group.legs.none { it.id == "b" } })
    }

    @Test
    fun `empty when no tickets`() {
        val state = HomeUiMapper.map(emptyList(), emptyList(), Instant.EPOCH)
        assertTrue(state.isEmpty)
        assertNull(state.nextJourney)
        assertNull(state.subtitle)
    }

    @Test
    fun `subtitle counts upcoming and in progress groups`() {
        val now = instant(2026, 8, 1, 12, 0)
        val tickets =
            listOf(
                ticket("a", dep = instant(2026, 8, 3, 17, 0)),
                ticket("b", dep = instant(2026, 8, 22, 17, 0), from = "X", to = "Y"),
                ticket("old", dep = instant(2026, 7, 5, 17, 0), from = "P", to = "Q"),
            )
        assertEquals("Sat, 1 Aug · 2 trips ahead", map(tickets, now).subtitle)
    }

    @Test
    fun `subtitle says one trip ahead and no trips ahead`() {
        val now = instant(2026, 8, 1, 12, 0)
        val one = listOf(ticket("a", dep = instant(2026, 8, 3, 17, 0)))
        assertEquals("Sat, 1 Aug · 1 trip ahead", map(one, now).subtitle)
        val none = listOf(ticket("old", dep = instant(2026, 7, 5, 17, 0)))
        assertEquals("Sat, 1 Aug · No trips ahead", map(none, now).subtitle)
    }

    @Test
    fun `hero shows countdown codes title cased names duration and next day offset`() {
        val now = instant(2026, 8, 1, 12, 0)
        val t =
            ticket(
                "a",
                dep = instant(2026, 8, 3, 17, 0),
                arr = instant(2026, 8, 4, 7, 59),
                from = "SNSI",
                fromName = "SAINAGAR SHIRDI",
                to = "BMT",
                toName = "BEGAMPET",
                trainNumber = "17001",
                trainName = "SNSI SC EXPRESS",
                classLabel = "THIRD AC",
            )
        val hero = map(listOf(t), now).nextJourney!!
        assertEquals("MON, 3 AUG · 17:00", hero.overline)
        assertEquals("in 2d 5h", hero.countdown)
        assertEquals("SNSI", hero.fromCode)
        assertEquals("Sainagar Shirdi", hero.fromName)
        assertEquals("BMT", hero.toCode)
        assertEquals("Begampet", hero.toName)
        assertEquals("17:00", hero.departureTime)
        assertEquals("07:59", hero.arrivalTime)
        assertEquals("+1", hero.dayOffset)
        assertEquals("14h 59m", hero.duration)
        assertEquals("17001 · SNSI SC Express · Third AC", hero.trainLine)
    }

    @Test
    fun `hero without arrival shows dash and no offset or duration`() {
        val now = instant(2026, 8, 1, 12, 0)
        val hero = map(listOf(ticket("a", dep = instant(2026, 8, 3, 17, 0))), now).nextJourney!!
        assertEquals("—", hero.arrivalTime)
        assertNull(hero.dayOffset)
        assertNull(hero.duration)
    }

    @Test
    fun `hero same day arrival has no offset`() {
        val now = instant(2026, 8, 1, 12, 0)
        val t = ticket("a", dep = instant(2026, 8, 3, 6, 0), arr = instant(2026, 8, 3, 9, 30))
        assertNull(map(listOf(t), now).nextJourney!!.dayOffset)
    }

    @Test
    fun `hero seat panel uses first non cancelled passenger and counts others`() {
        val now = instant(2026, 8, 1, 12, 0)
        val t =
            ticket(
                "a",
                dep = instant(2026, 8, 3, 17, 0),
                passengers =
                    listOf(
                        passenger(1, "GONE ONE", BerthStatus("CAN")),
                        passenger(2, "MADHURI", BerthStatus("CNF", "B4", 33, "LOWER")),
                        passenger(3, "RAVI", BerthStatus("CNF", "B4", 34, "MIDDLE")),
                    ),
            )
        val seat = map(listOf(t), now).nextJourney!!.seat!!
        assertEquals("Madhuri", seat.passengerName)
        assertEquals(1, seat.othersCount)
        assertEquals(StatusKind.CONFIRMED, seat.statusKind)
        assertEquals("B4", seat.coach)
        assertEquals("33", seat.berth)
        assertEquals("Lower", seat.berthType)
        assertNull(seat.bigValue)
    }

    @Test
    fun `hero seat panel waitlist variant has one large value`() {
        val now = instant(2026, 8, 1, 12, 0)
        val t =
            ticket(
                "a",
                dep = instant(2026, 8, 3, 17, 0),
                passengers = listOf(passenger(1, "MADHURI", BerthStatus("GNWL", waitlistNumber = 12))),
            )
        val seat = map(listOf(t), now).nextJourney!!.seat!!
        assertEquals("WL 12", seat.bigValue)
        assertEquals(StatusKind.WAITLIST, seat.statusKind)
    }

    @Test
    fun `hero seat panel rac variant uses berth number`() {
        val now = instant(2026, 8, 1, 12, 0)
        val t =
            ticket(
                "a",
                dep = instant(2026, 8, 3, 17, 0),
                passengers = listOf(passenger(1, "MADHURI", BerthStatus("RAC", "S3", 23, "SIDE LOWER"))),
            )
        assertEquals("RAC 23", map(listOf(t), now).nextJourney!!.seat!!.bigValue)
    }

    @Test
    fun `hero seat panel hidden without passengers or when all cancelled`() {
        val now = instant(2026, 8, 1, 12, 0)
        assertNull(map(listOf(ticket("a", dep = instant(2026, 8, 3, 17, 0))), now).nextJourney!!.seat)
        val cancelled =
            ticket(
                "b",
                dep = instant(2026, 8, 3, 17, 0),
                passengers = listOf(passenger(1, "X", BerthStatus("CAN"))),
            )
        assertNull(map(listOf(cancelled), now).nextJourney!!.seat)
    }

    @Test
    fun `hero skips cancelled legs and picks earliest future departure`() {
        val now = instant(2026, 8, 1, 12, 0)
        val cancelled = ticket("c", dep = instant(2026, 8, 2, 8, 0), isCancelled = true, from = "X", to = "Y")
        val later = ticket("l", dep = instant(2026, 8, 6, 8, 0), from = "M", to = "N")
        val sooner = ticket("s", dep = instant(2026, 8, 4, 8, 0), from = "P", to = "Q")
        assertEquals("s", map(listOf(cancelled, later, sooner), now).nextJourney?.ticketId)
    }

    @Test
    fun `needs review row lists file name and first two missing fields then more`() {
        val now = instant(2026, 8, 1, 12, 0)
        val bad =
            ticket("b", needsReview = true, dep = null, pnr = null, trainNumber = null, fileName = "scan_0812.pdf")
        val row = map(listOf(bad), now).needsReview.single()
        assertEquals("scan_0812.pdf", row.fileName)
        assertEquals(listOf(TicketField.PNR, TicketField.TRAIN_NUMBER), row.missing)
        assertEquals(1, row.moreMissing)
    }

    @Test
    fun `needs review row names pnr and departure only`() {
        val now = instant(2026, 8, 1, 12, 0)
        val bad = ticket("b", needsReview = true, dep = null, pnr = null, fileName = "scan.pdf")
        val row = map(listOf(bad), now).needsReview.single()
        assertEquals(listOf(TicketField.PNR, TicketField.DEPARTURE_DATE), row.missing)
        assertEquals(0, row.moreMissing)
    }

    @Test
    fun `needs review row with nothing missing has empty list and blank file name becomes null`() {
        val now = instant(2026, 8, 1, 12, 0)
        val bad = ticket("b", needsReview = true, dep = instant(2026, 8, 5, 10, 0), fileName = " ")
        val row = map(listOf(bad), now).needsReview.single()
        assertTrue(row.missing.isEmpty())
        assertEquals(0, row.moreMissing)
        assertNull(row.fileName)
    }

    @Test
    fun `round trip card has title date range leg rows and highlights only the next leg`() {
        val now = instant(2026, 8, 1, 12, 0)
        val out =
            ticket(
                "o", dep = instant(2026, 8, 3, 17, 0), arr = instant(2026, 8, 4, 7, 59),
                from = "SNSI", to = "BMT", trainNumber = "17001", travelClass = "3A",
                passengers = listOf(passenger(1, "MADHURI", BerthStatus("CNF", "B4", 33, "LOWER"))),
            )
        val back =
            ticket(
                "r", dep = instant(2026, 8, 10, 21, 40), arr = instant(2026, 8, 11, 12, 0),
                from = "SC", boardingCode = "LPI", to = "SNSI", trainNumber = "17002", travelClass = "SL",
                passengers =
                    listOf(
                        passenger(1, "A", BerthStatus("CNF", "S7", 49, "UPPER")),
                        passenger(2, "B", BerthStatus("CNF", "S7", 50, "LOWER")),
                        passenger(3, "C", BerthStatus("CNF", "S7", 51, "MIDDLE")),
                        passenger(4, "D", BerthStatus("CNF", "S7", 52, "UPPER")),
                    ),
            )
        val card = map(listOf(out, back), now).upcoming.single()
        assertEquals(JourneyKind.ROUND_TRIP, card.kind)
        assertEquals("3 – 10 Aug · 8 days", card.dateRange)
        assertEquals("o", card.anchorTicketId)
        assertFalse(card.inProgress)
        val (first, second) = card.legs
        assertTrue(first.highlighted)
        assertFalse(second.highlighted)
        assertEquals("SNSI → BMT", first.route)
        assertEquals("17:00 · 17001 · 3A", first.meta)
        assertEquals("B4 · 33", first.seat.primary)
        assertEquals("Lower", first.seat.berthType)
        assertEquals(0, first.seat.othersCount)
        assertEquals("SC → SNSI", second.route)
        assertEquals("21:40 · 17002 · SL · boards LPI", second.meta)
        assertEquals(3, second.seat.othersCount)
    }

    @Test
    fun `boards suffix is absent when boarding equals from`() {
        val now = instant(2026, 8, 1, 12, 0)
        val t = ticket("a", dep = instant(2026, 8, 3, 17, 0), from = "SNSI", boardingCode = "SNSI")
        assertFalse(map(listOf(t), now).upcoming.single().legs.single().meta.contains("boards"))
    }

    @Test
    fun `one way card has no title and single date and waitlist chip`() {
        val now = instant(2026, 8, 1, 12, 0)
        val t =
            ticket(
                "a", dep = instant(2026, 8, 22, 17, 0),
                passengers = listOf(passenger(1, "M", BerthStatus("GNWL", waitlistNumber = 12))),
            )
        val card = map(listOf(t), now).upcoming.single()
        assertEquals(JourneyKind.ONE_WAY, card.kind)
        assertNull(card.title)
        assertEquals("Sat, 22 Aug", card.dateRange)
        val seat = card.legs.single().seat
        assertEquals(LegSeatKind.STATUS_CHIP, seat.kind)
        assertEquals("WL 12", seat.primary)
        assertEquals(StatusKind.WAITLIST, seat.statusKind)
    }

    @Test
    fun `cancelled leg is flagged and has no seat`() {
        val now = instant(2026, 8, 1, 12, 0)
        val t =
            ticket(
                "a", dep = instant(2026, 8, 22, 17, 0), isCancelled = true,
                passengers = listOf(passenger(1, "M", BerthStatus("CNF", "B4", 33, "LOWER"))),
            )
        val leg = map(listOf(t), now).upcoming.single().legs.single()
        assertTrue(leg.cancelled)
        assertEquals(LegSeatKind.NONE, leg.seat.kind)
    }

    @Test
    fun `in progress group is flagged`() {
        val now = instant(2026, 8, 5, 12, 0)
        val out = ticket("o", dep = instant(2026, 8, 3, 17, 0), arr = instant(2026, 8, 4, 7, 0), from = "A", to = "B")
        val back = ticket("r", dep = instant(2026, 8, 10, 17, 0), arr = instant(2026, 8, 11, 7, 0), from = "B", to = "A")
        val card = map(listOf(out, back), now).upcoming.single()
        assertTrue(card.inProgress)
        assertEquals("r", map(listOf(out, back), now).nextJourney?.ticketId)
    }

    @Test
    fun `past trips group by month most recent first with subtitles`() {
        val now = instant(2026, 8, 1, 12, 0)
        val julOut = ticket("jo", dep = instant(2026, 7, 17, 17, 0), arr = instant(2026, 7, 18, 7, 0), from = "SNSI", to = "BMT", trainNumber = "17001")
        val julBack = ticket("jb", dep = instant(2026, 7, 24, 17, 0), arr = instant(2026, 7, 25, 7, 0), from = "BMT", to = "SNSI", trainNumber = "17002")
        val julCancelled = ticket("jc", dep = instant(2026, 7, 5, 17, 0), from = "SNSI", to = "BMT", trainNumber = "17001", isCancelled = true)
        val june = ticket("ju", dep = instant(2026, 6, 19, 17, 0), from = "P", to = "Q", trainNumber = "11")
        val state = map(listOf(julOut, julBack, julCancelled, june), now)
        assertEquals(listOf("JULY 2026", "JUNE 2026"), state.pastByMonth.map { it.header })
        val july = state.pastByMonth[0].rows
        assertEquals(2, july.size)
        assertEquals(JourneyKind.ROUND_TRIP, july[0].kind)
        assertEquals("Shirdi ⇄ Hyderabad", july[0].title)
        assertEquals("17 – 24 Jul · 17001 / 17002", july[0].subtitle)
        assertFalse(july[0].cancelled)
        assertEquals("SNSI → BMT", july[1].title)
        assertEquals("Sun, 5 Jul · 17001", july[1].subtitle)
        assertTrue(july[1].cancelled)
        assertEquals("Fri, 19 Jun · 11", state.pastByMonth[1].rows.single().subtitle)
    }

    @Test
    fun `missing required fields follows canonical order`() {
        val t = ticket("x", dep = null, pnr = null, trainNumber = null, from = "", to = "")
        assertEquals(
            listOf(
                TicketField.PNR,
                TicketField.TRAIN_NUMBER,
                TicketField.FROM,
                TicketField.TO,
                TicketField.DEPARTURE_DATE,
            ),
            HomeUiMapper.missingRequiredFields(t),
        )
        assertEquals(2, HomeUiMapper.MAX_MISSING_SHOWN)
    }

    private fun instant(y: Int, m: Int, d: Int, h: Int, min: Int): Instant =
        java.time.ZonedDateTime.of(y, m, d, h, min, 0, 0, ist).toInstant()

    private fun passenger(serial: Int, name: String, status: BerthStatus) =
        Passenger(
            id = "p$serial",
            serial = serial,
            name = name,
            age = 30,
            gender = null,
            bookingStatusRaw = null,
            currentStatusRaw = null,
            current = status,
        )

    private fun ticket(
        id: String,
        needsReview: Boolean = false,
        dep: Instant?,
        arr: Instant? = null,
        from: String = "A",
        fromName: String? = null,
        boardingCode: String? = null,
        to: String = "B",
        toName: String? = null,
        pnr: String? = "1234567890",
        trainNumber: String? = "1",
        trainName: String? = "EXP",
        travelClass: String? = "SL",
        classLabel: String? = null,
        isCancelled: Boolean = false,
        fileName: String? = null,
        passengers: List<Passenger> = emptyList(),
    ): Ticket =
        Ticket(
            id = id,
            pnr = pnr,
            trainNumber = trainNumber,
            trainName = trainName,
            from = Station(from, fromName),
            boarding = boardingCode?.let { Station(it, null) },
            to = Station(to, toName),
            startDate = null,
            departureAt = dep,
            arrivalAt = arr,
            travelClass = travelClass,
            classLabel = classLabel,
            quota = "GN",
            quotaLabel = null,
            distanceKm = null,
            bookedAt = null,
            transactionId = null,
            totalFarePaise = null,
            passengers = passengers,
            pdfFileName = "$id.pdf",
            pdfSha256 = null,
            originalFileName = fileName,
            extractionMethod = ExtractionMethod.TEXT_LAYER,
            needsReview = needsReview,
            linkGroupId = null,
            autoPairDisabled = false,
            isCancelled = isCancelled,
            notes = null,
            createdAt = dep ?: Instant.EPOCH,
            updatedAt = dep ?: Instant.EPOCH,
        )
}
