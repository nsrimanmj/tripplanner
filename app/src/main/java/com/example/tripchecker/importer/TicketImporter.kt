package com.example.tripchecker.importer

import com.example.tripchecker.data.file.StagedFile
import com.example.tripchecker.data.file.TicketFileOperations
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
    suspend fun process(staged: StagedFile): TicketImportResult {
        val existingBySha = repository.findBySha(staged.sha256)
        if (existingBySha != null) {
            staged.file.delete()
            return TicketImportResult.Duplicate(existingBySha.id)
        }
        val extracted = extractor.extract(staged.file)
        val rawText = extracted?.text
        val method = extracted?.method ?: ExtractionMethod.OCR
        if (rawText.isNullOrBlank()) {
            return persistNew(staged, ParsedTicket(), method, rawText ?: "", needsReview = true)
        }
        val parser = parsers.firstOrNull { it.canParse(rawText) }
        if (parser == null) {
            return persistNew(staged, ParsedTicket(), method, rawText, needsReview = true)
        }
        val parsed = parser.parse(rawText)
        val existingByPnr = parsed.pnr?.let { repository.findByPnr(it) }
        if (existingByPnr != null && isSameBooking(existingByPnr, parsed)) {
            return persistUpdate(existingByPnr, staged, parsed, method, rawText)
        }
        return persistNew(
            staged,
            parsed,
            method,
            rawText,
            needsReview = parsed.missingFields.intersect(com.example.tripchecker.extraction.parser.REQUIRED_TICKET_FIELDS).isNotEmpty(),
        )
    }

    private suspend fun persistNew(
        staged: StagedFile,
        parsed: ParsedTicket,
        method: ExtractionMethod,
        rawText: String,
        needsReview: Boolean,
    ): TicketImportResult {
        val id = UUID.randomUUID().toString()
        var movedName: String? = null
        try {
            movedName = fileStore.moveStagedToTicket(staged.file, id, staged.sha256)
            val ticket =
                ParsedTicketMapper.toNewTicket(
                    parsed = parsed,
                    ticketId = id,
                    pdfFileName = movedName,
                    pdfSha256 = staged.sha256,
                    originalFileName = staged.originalName,
                    extractionMethod = method,
                    now = clock.instant(),
                ).copy(needsReview = needsReview)
            repository.upsert(ticket, rawText = rawText)
            return if (ticket.needsReview) TicketImportResult.NeedsReview(id) else TicketImportResult.Saved(id)
        } catch (e: Exception) {
            movedName?.let { fileStore.delete(it) }
            throw e
        }
    }

    private suspend fun persistUpdate(
        existing: Ticket,
        staged: StagedFile,
        parsed: ParsedTicket,
        method: ExtractionMethod,
        rawText: String,
    ): TicketImportResult {
        val oldPdf = existing.pdfFileName
        var movedName: String? = null
        try {
            movedName = fileStore.moveStagedToTicket(staged.file, existing.id, staged.sha256)
            val updated =
                ParsedTicketMapper.mergeParsedIntoExisting(
                    existing = existing,
                    parsed = parsed,
                    pdfFileName = movedName,
                    pdfSha256 = staged.sha256,
                    originalFileName = staged.originalName,
                    extractionMethod = method,
                    now = clock.instant(),
                )
            repository.upsert(updated, rawText = rawText)
            if (oldPdf != null && oldPdf != movedName) {
                fileStore.delete(oldPdf)
            }
            return if (updated.needsReview) {
                TicketImportResult.NeedsReview(existing.id)
            } else {
                TicketImportResult.Updated(existing.id)
            }
        } catch (e: Exception) {
            movedName?.let { fileStore.delete(it) }
            throw e
        }
    }

    private fun isSameBooking(existing: Ticket, parsed: ParsedTicket): Boolean {
        if (parsed.trainNumber != null && existing.trainNumber != null &&
            parsed.trainNumber != existing.trainNumber
        ) {
            return false
        }
        if (parsed.startDate != null && existing.startDate != null &&
            parsed.startDate != existing.startDate
        ) {
            return false
        }
        return true
    }
}
