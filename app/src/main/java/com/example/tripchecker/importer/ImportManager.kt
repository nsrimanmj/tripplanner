package com.example.tripchecker.importer

import com.example.tripchecker.data.file.StagedFile
import com.example.tripchecker.data.file.TicketUriStager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

/** Request to import one PDF (URI resolved only in the Android stager). */
data class ImportEnqueueRequest(
    val uriString: String,
    val displayName: String,
)

/** App-scoped PDF import queue: immediate enqueue, single worker processes sequentially. */
class ImportManager(
    private val fileStore: TicketUriStager,
    private val importer: TicketImporter,
    scope: CoroutineScope,
) {
    private val _items = MutableStateFlow<List<ImportItem>>(emptyList())
    val items: StateFlow<List<ImportItem>> = _items.asStateFlow()

    private val work = Channel<String>(Channel.UNLIMITED)

    init {
        scope.launch {
            for (itemId in work) {
                processItem(itemId)
            }
        }
    }

    fun enqueue(requests: List<ImportEnqueueRequest>) {
        if (requests.isEmpty()) return
        val newItems =
            requests.map { req ->
                ImportItem(
                    id = UUID.randomUUID().toString(),
                    displayName = req.displayName,
                    state = ImportItemState.Queued,
                    uriString = req.uriString,
                )
            }
        _items.update { it + newItems }
        newItems.forEach { work.trySend(it.id) }
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

    private suspend fun processItem(itemId: String) {
        val item = _items.value.firstOrNull { it.id == itemId } ?: return
        var staged: StagedFile? = null
        try {
            staged = fileStore.importFromUri(item.uriString)
            updateItem(itemId) {
                it.copy(displayName = staged!!.originalName, state = ImportItemState.Reading)
            }
            when (val result = importer.process(staged)) {
                is TicketImportResult.Saved ->
                    updateItem(itemId) { it.copy(state = ImportItemState.Saved(result.ticketId), stagedFile = null) }
                is TicketImportResult.Updated ->
                    updateItem(itemId) { it.copy(state = ImportItemState.Updated(result.ticketId), stagedFile = null) }
                is TicketImportResult.Duplicate ->
                    updateItem(itemId) {
                        it.copy(state = ImportItemState.Duplicate(result.existingTicketId), stagedFile = null)
                    }
                is TicketImportResult.NeedsReview ->
                    updateItem(itemId) {
                        it.copy(state = ImportItemState.NeedsReview(result.ticketId), stagedFile = null)
                    }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            staged?.file?.delete()
            updateItem(itemId) {
                it.copy(state = ImportItemState.Failed(mapFailure(e)), stagedFile = null)
            }
        }
    }

    private fun mapFailure(e: Exception): String =
        when (e.message) {
            "Not a PDF" -> "Not a PDF"
            "File too large" -> "File too large"
            "Empty file" -> "Empty file"
            else -> "Import failed"
        }

    private fun updateItem(id: String, transform: (ImportItem) -> ImportItem) {
        _items.update { list -> list.map { if (it.id == id) transform(it) else it } }
    }
}

data class ImportItem(
    val id: String,
    val displayName: String,
    val state: ImportItemState,
    val uriString: String,
    val stagedFile: StagedFile? = null,
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
