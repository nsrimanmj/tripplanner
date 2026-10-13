package com.example.tripchecker.ui.importflow

import com.example.tripchecker.domain.model.ExtractionMethod
import com.example.tripchecker.domain.model.JourneyGroup
import com.example.tripchecker.domain.model.JourneyKind
import com.example.tripchecker.domain.model.JourneyStatus
import com.example.tripchecker.domain.model.Station
import com.example.tripchecker.domain.model.Ticket
import com.example.tripchecker.importer.ImportItem
import com.example.tripchecker.importer.ImportItemState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class ImportRowTextTest {
    private fun item(id: String, state: ImportItemState, name: String = "$id.pdf") =
        ImportItem(id = id, displayName = name, state = state, uriString = "content://$id")

    private fun ticket(
        id: String,
        from: String? = "SNSI",
        to: String? = "BMT",
        boarding: String? = null,
        startDate: LocalDate? = LocalDate.of(2026, 8, 3),
        departureAt: Instant? = null,
        method: ExtractionMethod = ExtractionMethod.OCR,
    ): Ticket =
        Ticket(
            id = id, pnr = null, trainNumber = null, trainName = null,
            from = from?.let { Station(it, null) },
            boarding = boarding?.let { Station(it, null) },
            to = to?.let { Station(it, null) },
            startDate = startDate, departureAt = departureAt, arrivalAt = null,
            travelClass = null, classLabel = null, quota = null, quotaLabel = null,
            distanceKm = null, bookedAt = null, transactionId = null, totalFarePaise = null,
            passengers = emptyList(), pdfFileName = null, pdfSha256 = null, originalFileName = null,
            extractionMethod = method, needsReview = false, linkGroupId = null,
            autoPairDisabled = false, isCancelled = false, notes = null,
            createdAt = Instant.EPOCH, updatedAt = Instant.EPOCH,
        )

    private fun group(kind: JourneyKind, vararg legs: Ticket) =
        JourneyGroup("k", kind, legs.toList(), isManual = false, status = JourneyStatus.UPCOMING)

    private fun rows(items: List<ImportItem>, tickets: List<Ticket> = emptyList(), groups: List<JourneyGroup> = emptyList()) =
        ImportRowText.rows(items, tickets, groups)

    @Test
    fun `queued and reading rows are not tappable and carry no ticket`() {
        val r = rows(listOf(item("a", ImportItemState.Queued), item("b", ImportItemState.Reading)))
        assertEquals(ImportRowKind.QUEUED, r[0].kind)
        assertEquals(ImportRowKind.READING, r[1].kind)
        r.forEach {
            assertFalse(it.clickable)
            assertFalse(it.showChevron)
            assertNull(it.targetTicketId)
        }
    }

    @Test
    fun `saved ocr ticket shows route date and via ocr`() {
        val r = rows(listOf(item("a", ImportItemState.Saved("t1"))), listOf(ticket("t1")))[0]
        assertEquals(ImportRowKind.SAVED, r.kind)
        assertEquals("SNSI → BMT, 3 Aug", r.route)
        assertEquals(ExtractionMethod.OCR, r.method)
        assertEquals(ImportRowTap.OPEN_TRIP, r.tap)
        assertEquals("t1", r.targetTicketId)
        assertTrue(r.showChevron)
    }

    @Test
    fun `saved text layer ticket carries text method`() {
        val r = rows(
            listOf(item("a", ImportItemState.Saved("t1"))),
            listOf(ticket("t1", method = ExtractionMethod.TEXT_LAYER)),
        )[0]
        assertEquals(ExtractionMethod.TEXT_LAYER, r.method)
    }

    @Test
    fun `saved ticket uses boarding station as the origin`() {
        val r = rows(listOf(item("a", ImportItemState.Saved("t1"))), listOf(ticket("t1", boarding = "PUNE")))[0]
        assertEquals("PUNE → BMT, 3 Aug", r.route)
    }

    @Test
    fun `saved ticket without start date falls back to the IST departure date`() {
        // 2026-08-02T20:00Z is 3 Aug 01:30 in IST.
        val dep = Instant.parse("2026-08-02T20:00:00Z")
        val r = rows(
            listOf(item("a", ImportItemState.Saved("t1"))),
            listOf(ticket("t1", startDate = null, departureAt = dep)),
        )[0]
        assertEquals("SNSI → BMT, 3 Aug", r.route)
    }

    @Test
    fun `saved ticket without any date shows the route only`() {
        val r = rows(listOf(item("a", ImportItemState.Saved("t1"))), listOf(ticket("t1", startDate = null)))[0]
        assertEquals("SNSI → BMT", r.route)
    }

    @Test
    fun `saved ticket without destination has no route text`() {
        val r = rows(listOf(item("a", ImportItemState.Saved("t1"))), listOf(ticket("t1", to = null)))[0]
        assertNull(r.route)
        assertEquals(ImportRowKind.SAVED, r.kind)
    }

    @Test
    fun `saved ticket missing from the repository still gets a plain saved row`() {
        val r = rows(listOf(item("a", ImportItemState.Saved("gone"))))[0]
        assertEquals(ImportRowKind.SAVED, r.kind)
        assertNull(r.route)
        assertNull(r.method)
        assertEquals("gone", r.targetTicketId)
    }

    @Test
    fun `saved ticket that is a leg of a round trip is shown as paired`() {
        val out = ticket("t1")
        val back = ticket("t2", from = "BMT", to = "SNSI")
        val r = rows(
            listOf(item("a", ImportItemState.Saved("t2"))),
            listOf(out, back),
            listOf(group(JourneyKind.ROUND_TRIP, out, back)),
        )[0]
        assertEquals(ImportRowKind.SAVED_PAIRED, r.kind)
        assertNull(r.route)
        assertNull(r.method)
        assertEquals(ImportRowTap.OPEN_TRIP, r.tap)
    }

    @Test
    fun `saved ticket in a one way group is not paired`() {
        val t = ticket("t1")
        val r = rows(
            listOf(item("a", ImportItemState.Saved("t1"))),
            listOf(t),
            listOf(group(JourneyKind.ONE_WAY, t)),
        )[0]
        assertEquals(ImportRowKind.SAVED, r.kind)
    }

    @Test
    fun `updated row opens the trip`() {
        val r = rows(listOf(item("a", ImportItemState.Updated("t1"))))[0]
        assertEquals(ImportRowKind.UPDATED, r.kind)
        assertEquals(ImportRowTap.OPEN_TRIP, r.tap)
        assertTrue(r.showChevron)
    }

    @Test
    fun `duplicate row opens the existing ticket without a chevron`() {
        val r = rows(listOf(item("a", ImportItemState.Duplicate("old"))))[0]
        assertEquals(ImportRowKind.DUPLICATE, r.kind)
        assertEquals(ImportRowTap.OPEN_TRIP, r.tap)
        assertEquals("old", r.targetTicketId)
        assertTrue(r.clickable)
        assertFalse(r.showChevron)
    }

    @Test
    fun `needs review row opens the editor`() {
        val r = rows(listOf(item("a", ImportItemState.NeedsReview("t9"))))[0]
        assertEquals(ImportRowKind.NEEDS_REVIEW, r.kind)
        assertEquals(ImportRowTap.EDIT_TICKET, r.tap)
        assertEquals("t9", r.targetTicketId)
        assertTrue(r.showChevron)
    }

    @Test
    fun `failed row keeps the reason and is not tappable`() {
        val r = rows(listOf(item("a", ImportItemState.Failed("Not a PDF"))))[0]
        assertEquals(ImportRowKind.FAILED, r.kind)
        assertEquals("Not a PDF", r.failureReason)
        assertFalse(r.clickable)
    }

    @Test
    fun `row order and file names are preserved`() {
        val r = rows(listOf(item("a", ImportItemState.Queued, "x.pdf"), item("b", ImportItemState.Reading, "y.pdf")))
        assertEquals(listOf("a", "b"), r.map { it.id })
        assertEquals(listOf("x.pdf", "y.pdf"), r.map { it.fileName })
    }

    @Test
    fun `summary counts each state and finished excludes queued and reading`() {
        val s = ImportRowText.summarize(
            listOf(
                item("1", ImportItemState.Saved("a")),
                item("2", ImportItemState.Saved("b")),
                item("3", ImportItemState.Updated("c")),
                item("4", ImportItemState.Duplicate("d")),
                item("5", ImportItemState.NeedsReview("e")),
                item("6", ImportItemState.Failed("x")),
                item("7", ImportItemState.Reading),
                item("8", ImportItemState.Queued),
            ),
        )
        assertEquals(8, s.total)
        assertEquals(6, s.finished)
        assertEquals(2, s.saved)
        assertEquals(1, s.updated)
        assertEquals(1, s.duplicate)
        assertEquals(1, s.needsReview)
        assertEquals(1, s.failed)
        assertTrue(s.inProgress)
        assertEquals(0.75f, s.fraction, 0.0001f)
    }

    @Test
    fun `summary is done when nothing is queued or reading`() {
        val s = ImportRowText.summarize(listOf(item("1", ImportItemState.Saved("a"))))
        assertFalse(s.inProgress)
        assertEquals(1f, s.fraction, 0f)
    }

    @Test
    fun `empty summary has zero progress and is not in progress`() {
        val s = ImportRowText.summarize(emptyList())
        assertEquals(0f, s.fraction, 0f)
        assertFalse(s.inProgress)
    }
}
