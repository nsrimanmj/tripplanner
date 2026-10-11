package com.example.tripchecker.importer

import com.example.tripchecker.data.file.TicketFileOperations
import com.example.tripchecker.data.file.TicketFileStore
import com.example.tripchecker.data.repo.TicketRepository
import com.example.tripchecker.domain.model.ExtractionMethod
import com.example.tripchecker.domain.model.Ticket
import com.example.tripchecker.extraction.PdfTextExtractor
import com.example.tripchecker.extraction.parser.ParsedTicket
import com.example.tripchecker.extraction.parser.TicketParser
import java.time.Clock
import java.util.UUID

/** Result of importing one staged PDF. */
sealed class TicketImportResult {
    data class Saved(val ticketId: String) : TicketImportResult()
    data class Updated(val ticketId: String) : TicketImportResult()
    data class Duplicate(val existingTicketId: String) : TicketImportResult()
    data class NeedsReview(val ticketId: String) : TicketImportResult()
}

/** Imports one PDF: extract, parse, dedupe, and persist. */
class TicketImporter(
    private val repository: TicketRepository,
    private val fileStore: TicketFileOperations,
    private val extractor: PdfTextExtractor,
    private val parsers: List<TicketParser>,
    private val clock: Clock,
) {
    suspend fun process(staged: TicketFileStore.StagedFile): TicketImportResult {
        val existingBySha = repository.findBySha(staged.sha256)
        if (existingBySha != null) {
            staged.file.delete()
            return TicketImportResult.Duplicate(existingBySha.id)
        }
        val extracted = extractor.extract(staged.file)
        val rawText = extracted?.text
        val method = extracted?.method ?: ExtractionMethod.OCR
        if (rawText.isNullOrBlank()) {
            val id = UUID.randomUUID().toString()
            val fileName = fileStore.moveToTicketId(staged.file, id)
            val ticket = emptyTicket(id, fileName, staged, method, needsReview = true)
            repository.upsert(ticket, rawText = "")
            return TicketImportResult.NeedsReview(id)
        }
        val parser = parsers.firstOrNull { it.canParse(rawText) }
        if (parser == null) {
            val id = UUID.randomUUID().toString()
            val fileName = fileStore.moveToTicketId(staged.file, id)
            val ticket = emptyTicket(id, fileName, staged, method, needsReview = true)
            repository.upsert(ticket, rawText = rawText)
            return TicketImportResult.NeedsReview(id)
        }
        val parsed = parser.parse(rawText)
        val existingByPnr = parsed.pnr?.let { repository.findByPnr(it) }
        if (existingByPnr != null) {
            val fileName = fileStore.moveToTicketId(staged.file, existingByPnr.id)
            fileStore.delete(existingByPnr.pdfFileName)
            val updated =
                ParsedTicketMapper.mergeParsedIntoExisting(
                    existing = existingByPnr,
                    parsed = parsed,
                    pdfFileName = fileName,
                    pdfSha256 = staged.sha256,
                    originalFileName = staged.originalName,
                    extractionMethod = method,
                    now = clock.instant(),
                )
            repository.upsert(updated, rawText = rawText)
            return if (updated.needsReview) {
                TicketImportResult.NeedsReview(existingByPnr.id)
            } else {
                TicketImportResult.Updated(existingByPnr.id)
            }
        }
        val id = UUID.randomUUID().toString()
        val fileName = fileStore.moveToTicketId(staged.file, id)
        val ticket =
            ParsedTicketMapper.toNewTicket(
                parsed = parsed,
                ticketId = id,
                pdfFileName = fileName,
                pdfSha256 = staged.sha256,
                originalFileName = staged.originalName,
                extractionMethod = method,
                now = clock.instant(),
            )
        repository.upsert(ticket, rawText = rawText)
        return if (ticket.needsReview) {
            TicketImportResult.NeedsReview(id)
        } else {
            TicketImportResult.Saved(id)
        }
    }

    private fun emptyTicket(
        id: String,
        fileName: String,
        staged: TicketFileStore.StagedFile,
        method: ExtractionMethod,
        needsReview: Boolean,
    ): Ticket {
        val now = clock.instant()
        return ParsedTicketMapper.toNewTicket(
            parsed = ParsedTicket(),
            ticketId = id,
            pdfFileName = fileName,
            pdfSha256 = staged.sha256,
            originalFileName = staged.originalName,
            extractionMethod = method,
            now = now,
        ).copy(needsReview = needsReview)
    }
}
