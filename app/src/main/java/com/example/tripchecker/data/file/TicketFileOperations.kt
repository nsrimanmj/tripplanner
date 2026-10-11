package com.example.tripchecker.data.file

import java.io.File

/** File operations needed by the import pipeline (testable). */
interface TicketFileOperations {
    suspend fun moveToTicketId(staged: File, ticketId: String): String
    suspend fun delete(fileName: String?)
}
