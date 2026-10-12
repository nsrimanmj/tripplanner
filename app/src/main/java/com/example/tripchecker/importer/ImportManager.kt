package com.example.tripchecker.importer

import android.net.Uri
import com.example.tripchecker.data.file.TicketFileStore
import com.example.tripchecker.data.file.TicketUriStager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/** App-scoped PDF import queue with immediate staging and sequential processing. */
class ImportManager(
    private val fileStore: TicketUriStager,
    private val importer: TicketImporter,
    private val scope: CoroutineScope,
) {
    private val _items = MutableStateFlow<List<ImportItem>>(emptyList())
    val items: StateFlow<List<ImportItem>> = _items.asStateFlow()

    fun enqueue(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val newItems =
            uris.map { uri ->
                ImportItem(
                    id = UUID.randomUUID().toString(),
                    displayName = uri.lastPathSegment ?: "ticket.pdf",
                    state = ImportItemState.Queued,
                    sourceUri = uri,
                )
            }
        _items.update { it + newItems }
        scope.launch {
            val staged = mutableListOf<Pair<ImportItem, TicketFileStore.StagedFile>>()
            for (item in newItems) {
                updateItem(item.id) { it.copy(state = ImportItemState.Reading) }
                try {
                    val file = fileStore.importFromUri(item.sourceUri)
                    staged.add(item to file)
                    updateItem(item.id) {
                        it.copy(displayName = file.originalName, stagedFile = file)
                    }
                } catch (e: Exception) {
                    updateItem(item.id) {
                        it.copy(state = ImportItemState.Failed(e.message ?: "Import failed"))
                    }
                }
            }
            for ((item, file) in staged) {
                updateItem(item.id) { it.copy(state = ImportItemState.Reading) }
                try {
                    when (val result = importer.process(file)) {
                        is TicketImportResult.Saved ->
                            updateItem(item.id) {
                                it.copy(state = ImportItemState.Saved(result.ticketId))
                            }
                        is TicketImportResult.Updated ->
                            updateItem(item.id) {
                                it.copy(state = ImportItemState.Updated(result.ticketId))
                            }
                        is TicketImportResult.Duplicate ->
                            updateItem(item.id) {
                                it.copy(state = ImportItemState.Duplicate(result.existingTicketId))
                            }
                        is TicketImportResult.NeedsReview ->
                            updateItem(item.id) {
                                it.copy(state = ImportItemState.NeedsReview(result.ticketId))
                            }
                    }
                } catch (e: Exception) {
                    file.file.delete()
                    updateItem(item.id) {
                        it.copy(state = ImportItemState.Failed(e.message ?: "Processing failed"))
                    }
                }
            }
        }
    }

    fun clearFinished() {
        _items.update { list ->
            list.filter { item ->
                when (item.state) {
                    ImportItemState.Queued,
                    ImportItemState.Reading,
                    -> true
                    else -> false
                }
            }
        }
    }

    private fun updateItem(id: String, transform: (ImportItem) -> ImportItem) {
        _items.update { list -> list.map { if (it.id == id) transform(it) else it } }
    }
}

data class ImportItem(
    val id: String,
    val displayName: String,
    val state: ImportItemState,
    val sourceUri: Uri,
    val stagedFile: TicketFileStore.StagedFile? = null,
)

sealed class ImportItemState {
    data object Queued : ImportItemState()
    data object Reading : ImportItemState()
    data class Saved(val ticketId: String) : ImportItemState()
    data class Updated(val ticketId: String) : ImportItemState()
    data class Duplicate(val existingTicketId: String) : ImportItemState()
    data class NeedsReview(val ticketId: String) : ImportItemState()
    data class Failed(val reason: String) : ImportItemState()
}
