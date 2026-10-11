package com.example.tripchecker.importer

import com.example.tripchecker.Fixtures
import com.example.tripchecker.domain.model.ExtractionMethod
import com.example.tripchecker.domain.model.JourneyKind
import com.example.tripchecker.domain.grouping.TripGrouper
import com.example.tripchecker.extraction.parser.IrctcErsParser
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class ParsedTicketMapperTest {
    private val parser = IrctcErsParser()
    private val now = Instant.parse("2026-07-01T00:00:00Z")

    @Test
    fun `parsed fixtures form one round trip`() {
        val outbound =
            ParsedTicketMapper.toNewTicket(
                parser.parse(Fixtures.load("ers_sample_text_layer.txt")),
                ticketId = "out",
                pdfFileName = "out.pdf",
                pdfSha256 = "sha1",
                originalFileName = "out.pdf",
                extractionMethod = ExtractionMethod.TEXT_LAYER,
                now = now,
            )
        val ret =
            ParsedTicketMapper.toNewTicket(
                parser.parse(Fixtures.load("ers_multi_passenger.txt")),
                ticketId = "ret",
                pdfFileName = "ret.pdf",
                pdfSha256 = "sha2",
                originalFileName = "ret.pdf",
                extractionMethod = ExtractionMethod.TEXT_LAYER,
                now = now,
            )
        val groups = TripGrouper.group(listOf(outbound, ret), now)
        assertEquals(1, groups.size)
        assertEquals(JourneyKind.ROUND_TRIP, groups[0].kind)
    }
}
