package com.example.tripchecker.importer

import com.example.tripchecker.Fixtures
import com.example.tripchecker.data.file.TicketFileOperations
import com.example.tripchecker.data.file.TicketFileStore
import com.example.tripchecker.data.repo.TicketRepository
import com.example.tripchecker.domain.model.ExtractionMethod
import com.example.tripchecker.domain.model.Ticket
import com.example.tripchecker.extraction.PdfTextExtractor
import com.example.tripchecker.extraction.model.ExtractedText
import com.example.tripchecker.extraction.parser.IrctcErsParser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class TicketImporterTest {
    private val fixedClock = Clock.fixed(Instant.parse("2026-07-01T00:00:00Z"), ZoneOffset.UTC)
    private val parser = IrctcErsParser()

    @Test
    fun `new import saved`() = runTest {
        val repo = FakeRepo()
        val importer = importer(repo, FakeExtractor(Fixtures.load("ers_sample_text_layer.txt")))
        val result = importer.process(staged("sha-new"))
        assertTrue(result is TicketImportResult.Saved)
    }

    @Test
    fun `same sha duplicate deletes staged`() = runTest {
        val repo = FakeRepo(existingSha = "dup-sha")
        val importer = importer(repo, FakeExtractor("text"))
        val stagedFile = staged("dup-sha")
        val result = importer.process(stagedFile)
        assertTrue(result is TicketImportResult.Duplicate)
        assertTrue(!stagedFile.file.exists())
    }

    @Test
    fun `same pnr updates and keeps id`() = runTest {
        val repo = FakeRepo(existingPnr = "8751652729", existingId = "keep-id")
        val importer = importer(repo, FakeExtractor(Fixtures.load("ers_sample_text_layer.txt")))
        val result = importer.process(staged("new-sha"))
        assertTrue(result is TicketImportResult.Updated)
        assertEquals("keep-id", (result as TicketImportResult.Updated).ticketId)
        assertEquals("link", repo.tickets["keep-id"]?.linkGroupId)
        assertEquals("note", repo.tickets["keep-id"]?.notes)
    }

    @Test
    fun `missing pnr needs review`() = runTest {
        val repo = FakeRepo()
        val importer = importer(repo, FakeExtractor("Electronic Reservation Slip\nTrain No"))
        val result = importer.process(staged("sha-nopnr"))
        assertTrue(result is TicketImportResult.NeedsReview)
    }

    @Test
    fun `null extractor needs review keeps pdf`() = runTest {
        val repo = FakeRepo()
        val importer = importer(repo, FakeExtractor(null))
        val stagedFile = staged("sha-blank")
        val result = importer.process(stagedFile)
        assertTrue(result is TicketImportResult.NeedsReview)
        assertTrue(File(stagedFile.file.parentFile, "${(result as TicketImportResult.NeedsReview).ticketId}.pdf").exists())
    }

    private fun importer(repo: FakeRepo, extractor: FakeExtractor) =
        TicketImporter(
            repository = repo,
            fileStore = FakeFileStore(),
            extractor = extractor,
            parsers = listOf(parser),
            clock = fixedClock,
        )

    private fun staged(sha: String): TicketFileStore.StagedFile {
        val file = File.createTempFile("ticket", ".pdf")
        file.writeText("dummy")
        return TicketFileStore.StagedFile(file, sha, "t.pdf", file.length())
    }

    private class FakeExtractor(private val text: String?) : PdfTextExtractor {
        override suspend fun extract(file: File): ExtractedText? =
            text?.let { ExtractedText(it, ExtractionMethod.TEXT_LAYER, 1) }
    }

    private class FakeFileStore : TicketFileOperations {
        override suspend fun moveToTicketId(staged: File, ticketId: String): String {
            val dest = File(staged.parentFile, "$ticketId.pdf")
            staged.renameTo(dest)
            return dest.name
        }

        override suspend fun delete(fileName: String?) {
            fileName?.let { name ->
                // no-op in tests
            }
        }
    }

    private class FakeRepo(
        private val existingSha: String? = null,
        private val existingPnr: String? = null,
        private val existingId: String = "existing",
    ) : TicketRepository {
        val tickets = mutableMapOf<String, Ticket>()

        init {
            if (existingSha != null || existingPnr != null) {
                val t =
                    Ticket(
                        id = existingId,
                        pnr = existingPnr,
                        trainNumber = null,
                        trainName = null,
                        from = null,
                        boarding = null,
                        to = null,
                        startDate = null,
                        departureAt = null,
                        arrivalAt = null,
                        travelClass = null,
                        classLabel = null,
                        quota = null,
                        quotaLabel = null,
                        distanceKm = null,
                        bookedAt = null,
                        transactionId = null,
                        totalFarePaise = null,
                        passengers = emptyList(),
                        pdfFileName = "old.pdf",
                        pdfSha256 = existingSha,
                        originalFileName = null,
                        extractionMethod = ExtractionMethod.MANUAL,
                        needsReview = false,
                        linkGroupId = "link",
                        autoPairDisabled = false,
                        isCancelled = false,
                        notes = "note",
                        createdAt = Instant.EPOCH,
                        updatedAt = Instant.EPOCH,
                    )
                tickets[t.id] = t
            }
        }

        override fun observeTickets(): Flow<List<Ticket>> = flowOf(tickets.values.toList())
        override fun observeTicket(id: String): Flow<Ticket?> = flowOf(tickets[id])
        override suspend fun findByPnr(pnr: String): Ticket? = tickets.values.firstOrNull { it.pnr == pnr }
        override suspend fun findBySha(sha: String): Ticket? = tickets.values.firstOrNull { it.pdfSha256 == sha }
        override suspend fun upsert(ticket: Ticket, rawText: String?) {
            tickets[ticket.id] = ticket
        }
        override suspend fun delete(id: String) {
            tickets.remove(id)
        }
        override suspend fun setCancelled(id: String, value: Boolean) = Unit
        override suspend fun link(ids: List<String>, groupId: String) = Unit
        override suspend fun unlink(ids: List<String>) = Unit
        override suspend fun resetPairing(ids: List<String>) = Unit
    }
}
