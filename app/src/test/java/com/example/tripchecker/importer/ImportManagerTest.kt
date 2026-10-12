package com.example.tripchecker.importer

import android.net.Uri
import com.example.tripchecker.Fixtures
import com.example.tripchecker.data.file.TicketFileOperations
import com.example.tripchecker.data.file.TicketFileStore
import com.example.tripchecker.data.file.TicketUriStager
import com.example.tripchecker.data.repo.TicketRepository
import com.example.tripchecker.domain.model.ExtractionMethod
import com.example.tripchecker.domain.model.Ticket
import com.example.tripchecker.extraction.PdfTextExtractor
import com.example.tripchecker.extraction.model.ExtractedText
import com.example.tripchecker.extraction.parser.IrctcErsParser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class ImportManagerTest {
    @Test
    fun `non pdf failure surfaces message`() = runTest {
        val manager =
            ImportManager(
                fileStore = FailingStager(IllegalStateException("Not a PDF")),
                importer = stubImporter(),
                scope = backgroundScope,
            )
        manager.enqueue(listOf(Uri.parse("content://test/ticket.pdf")))
        advanceUntilIdle()
        val failed = manager.items.value.single().state as ImportItemState.Failed
        assertEquals("Not a PDF", failed.reason)
    }

    @Test
    fun `saved import reaches Saved state`() = runTest {
        val staged = stagedFile("sha-new")
        val manager =
            ImportManager(
                fileStore = SuccessStager(staged),
                importer =
                    TicketImporter(
                        repository = EmptyRepo(),
                        fileStore = NoOpFileOps(),
                        extractor =
                            object : PdfTextExtractor {
                                override suspend fun extract(file: File): ExtractedText? =
                                    ExtractedText(
                                        Fixtures.load("ers_sample_text_layer.txt"),
                                        ExtractionMethod.TEXT_LAYER,
                                        1,
                                    )
                            },
                        parsers = listOf(IrctcErsParser()),
                        clock = Clock.fixed(Instant.parse("2026-07-01T00:00:00Z"), ZoneOffset.UTC),
                    ),
                scope = backgroundScope,
            )
        manager.enqueue(listOf(Uri.parse("content://test/a.pdf")))
        advanceUntilIdle()
        assertTrue(manager.items.value.single().state is ImportItemState.Saved)
    }

    private fun stubImporter(): TicketImporter =
        TicketImporter(
            repository = EmptyRepo(),
            fileStore = NoOpFileOps(),
            extractor = object : PdfTextExtractor {
                override suspend fun extract(file: File): ExtractedText? = null
            },
            parsers = listOf(IrctcErsParser()),
            clock = Clock.fixed(Instant.parse("2026-07-01T00:00:00Z"), ZoneOffset.UTC),
        )

    private fun stagedFile(sha: String): TicketFileStore.StagedFile {
        val file = File.createTempFile("staged", ".pdf")
        file.writeBytes("%PDF-1.4\n".encodeToByteArray())
        return TicketFileStore.StagedFile(file, sha, "a.pdf", file.length())
    }

    private class FailingStager(private val error: Exception) : TicketUriStager {
        override suspend fun importFromUri(uri: Uri): TicketFileStore.StagedFile = throw error
    }

    private class SuccessStager(private val staged: TicketFileStore.StagedFile) : TicketUriStager {
        override suspend fun importFromUri(uri: Uri): TicketFileStore.StagedFile = staged
    }

    private class NoOpFileOps : TicketFileOperations {
        override suspend fun moveToTicketId(staged: File, ticketId: String): String {
            val dest = File(staged.parentFile, "$ticketId.pdf")
            staged.renameTo(dest)
            return dest.name
        }

        override suspend fun delete(fileName: String?) = Unit
    }

    private class EmptyRepo : TicketRepository {
        override fun observeTickets(): Flow<List<Ticket>> = flowOf(emptyList())

        override fun observeTicket(id: String): Flow<Ticket?> = flowOf(null)

        override suspend fun findByPnr(pnr: String): Ticket? = null

        override suspend fun findBySha(sha256: String): Ticket? = null

        override suspend fun upsert(ticket: Ticket, rawText: String?) = Unit

        override suspend fun delete(id: String) = Unit

        override suspend fun setCancelled(id: String, value: Boolean) = Unit

        override suspend fun link(ids: List<String>, groupId: String) = Unit

        override suspend fun unlink(ids: List<String>) = Unit

        override suspend fun resetPairing(ids: List<String>) = Unit
    }
}
