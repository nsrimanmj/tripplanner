package com.example.tripchecker.data.file

import java.io.File

/** File operations needed by the import pipeline (testable). */
interface TicketFileOperations {
    suspend fun moveStagedToTicket(staged: File, ticketId: String, sha256: String): String

    suspend fun delete(fileName: String?)
}

/** Builds the stored PDF file name for a ticket. */
fun ticketPdfFileName(ticketId: String, sha256: String): String =
    "$ticketId-${sha256.take(12)}.pdf"
