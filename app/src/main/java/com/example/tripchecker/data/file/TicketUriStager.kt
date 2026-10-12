package com.example.tripchecker.data.file

/** Stages a content URI into app-private ticket storage. */
interface TicketUriStager {
    suspend fun importFromUri(uriString: String): StagedFile
}
