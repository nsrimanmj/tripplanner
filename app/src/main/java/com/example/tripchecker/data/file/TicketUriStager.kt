package com.example.tripchecker.data.file

import android.net.Uri

/** Stages a content URI into app-private ticket storage. */
interface TicketUriStager {
    suspend fun importFromUri(uri: Uri): TicketFileStore.StagedFile
}
