package com.example.tripchecker.importer

import com.example.tripchecker.Fixtures
import com.example.tripchecker.data.file.StagedFile
import com.example.tripchecker.data.file.TicketFileOperations
import com.example.tripchecker.data.file.ticketPdfFileName
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
import org.junit.Assert.assertFalse
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
        val dir = File.createTempFile("tickets", "").apply { delete(); mkdirs() }
        val repo = FakeRepo()
        val files = TrackingFileStore(dir)
        val importer = importer(repo, files, FakeExtractor(Fixtures.load("ers_sample_text_layer.txt")))
        val result = importer.process(staged("sha-new", dir))
        assertTrue(result is TicketImportResult.Saved)
    }

    @Test
    fun `same sha duplicate deletes staged`() = runTest {
        val dir = File.createTempFile("tickets", "").apply { delete(); mkdirs() }
        val repo = FakeRepo(existingSha = "dup-sha")
        val importer = importer(repo, TrackingFileStore(dir), FakeExtractor("text"))
        val stagedFile = staged("dup-sha", dir)
        val result = importer.process(stagedFile)
        assertTrue(result is TicketImportResult.Duplicate)
        assertFalse(stagedFile.file.exists())
    }

    @Test
    fun `same pnr updates keeps id and new pdf`() = runTest {
        val dir = File.createTempFile("tickets", "").apply { delete(); mkdirs() }
        val oldSha = "aaaaaaaaaaaa"
        val newSha = "bbbbbbbbbbbb"
        val repo = FakeRepo(existingPnr = "8751652729", existingId = "keep-id", existingSha = oldSha, dir = dir)
        val files = TrackingFileStore(dir)
        val importer = importer(repo, files, FakeExtractor(Fixtures.load("ers_sample_text_layer.txt")))
        val result = importer.process(staged(newSha, dir))
        assertTrue(result is TicketImportResult.Updated)
        assertEquals("keep-id", (result as TicketImportResult.Updated).ticketId)
        val saved = repo.tickets["keep-id"]!!
        val newName = ticketPdfFileName("keep-id", newSha)
        assertEquals(newName, saved.pdfFileName)
        assertTrue(File(dir, newName).exists())
        assertTrue(files.deleted.contains(ticketPdfFileName("keep-id", oldSha)))
    }

    @Test
    fun `missing pnr needs review`() = runTest {
        val dir = File.createTempFile("tickets", "").apply { delete(); mkdirs() }
        val repo = FakeRepo()
        val importer = importer(repo, TrackingFileStore(dir), FakeExtractor("Electronic Reservation Slip\nTrain No"))
        val result = importer.process(staged("sha-nopnr", dir))
        assertTrue(result is TicketImportResult.NeedsReview)
    }

    @Test
    fun `null extractor needs review keeps pdf`() = runTest {
        val dir = File.createTempFile("tickets", "").apply { delete(); mkdirs() }
        val repo = FakeRepo()
        val files = TrackingFileStore(dir)
        val importer = importer(repo, files, FakeExtractor(null))
        val result = importer.process(staged("sha-blank", dir))
        assertTrue(result is TicketImportResult.NeedsReview)
        val id = (result as TicketImportResult.NeedsReview).ticketId
        assertTrue(File(dir, ticketPdfFileName(id, "sha-blank")).exists())
    }

    private fun importer(repo: FakeRepo, files: TicketFileOperations, extractor: FakeExtractor) =
        TicketImporter(
            repository = repo,
            fileStore = files,
            extractor = extractor,
            parsers = listOf(parser),
            clock = fixedClock,
        )

    private fun staged(sha: String, dir: File): StagedFile {
        val file = File(dir, "staging-${System.nanoTime()}.pdf")
        file.writeBytes("%PDF-1.4\ncontent".encodeToByteArray())
        return StagedFile(file, sha, "t.pdf", file.length())
    }

    private class FakeExtractor(private val text: String?) : PdfTextExtractor {
        override suspend fun extract(file: File): ExtractedText? =
            text?.let { ExtractedText(it, ExtractionMethod.TEXT_LAYER, 1) }
    }

    private class TrackingFileStore(private val dir: File) : TicketFileOperations {
        val deleted = mutableListOf<String>()

        override suspend fun moveStagedToTicket(staged: File, ticketId: String, sha256: String): String {
            val dest = File(dir, ticketPdfFileName(ticketId, sha256))
            staged.copyTo(dest, overwrite = true)
            staged.delete()
            return dest.name
        }

        override suspend fun delete(fileName: String?) {
            if (fileName != null) {
                deleted.add(fileName)
                File(dir, fileName).delete()
            }
        }
    }

    private class FakeRepo(
        private val existingSha: String? = null,
        private val existingPnr: String? = null,
        private val existingId: String = "existing",
        private val dir: File? = null,
    ) : TicketRepository {
        val tickets = mutableMapOf<String, Ticket>()

        init {
            if (existingSha != null || existingPnr != null) {
                val sha = existingSha ?: "oldsha"
                val pdfName = ticketPdfFileName(existingId, sha)
                dir?.let { File(it, pdfName).writeBytes(byteArrayOf(1, 2, 3)) }
                val t =
                    Ticket(
                        id = existingId,
                        pnr = existingPnr,
                        trainNumber = "17001",
                        trainName = null,
                        from = null,
                        boarding = null,
                        to = null,
                        startDate = java.time.LocalDate.of(2026, 8, 3),
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
                        pdfFileName = pdfName,
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
        override suspend fun getRawText(ticketId: String): String? = null
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
