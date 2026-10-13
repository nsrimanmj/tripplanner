package com.example.tripchecker.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tripchecker.data.repo.TicketRepository
import com.example.tripchecker.data.file.TicketFileStore
import com.example.tripchecker.di.AppContainer
import com.example.tripchecker.domain.model.ExtractionMethod
import com.example.tripchecker.domain.model.Ticket
import com.example.tripchecker.extraction.parser.REQUIRED_TICKET_FIELDS
import com.example.tripchecker.extraction.parser.TicketField
import com.example.tripchecker.extraction.parser.TicketParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock

/** UI state for the edit / review / create ticket screen. */
data class EditTicketUiState(
    val isCreate: Boolean = true,
    val isLoading: Boolean = true,
    val form: EditTicketForm = EditTicketForm(),
    /** The form as first shown; [isDirty] compares against it. */
    val original: EditTicketForm = form,
    val missingFields: Set<TicketField> = emptySet(),
    val saveAttempted: Boolean = false,
    val saveErrorMessage: String? = null,
    /** True when the stored ticket was flagged for review (drives the "Review ticket" title). */
    val needsReview: Boolean = false,
    val pdfFileName: String? = null,
    val originalFileName: String? = null,
    val extractionMethod: ExtractionMethod? = null,
) {
    val isDirty: Boolean get() = form != original

    /** Required fields that are still empty, in display order. */
    val missingRequired: List<TicketField>
        get() = REQUIRED_TICKET_FIELDS.filter { it in missingFields }.sortedBy { it.ordinal }
}

/** ViewModel for manual create and edit ticket flows. */
class EditTicketViewModel(
    private val ticketId: String?,
    private val repository: TicketRepository,
    private val parsers: List<TicketParser>,
    private val clock: Clock,
    /** Used by the screen to open the ticket's PDF from the banner. */
    val fileStore: TicketFileStore? = null,
) : ViewModel() {
    private val existing = MutableStateFlow<Ticket?>(null)
    private var formHydratedFromDb = false
    private val _uiState = MutableStateFlow(EditTicketUiState(isCreate = ticketId == null))
    val uiState: StateFlow<EditTicketUiState> = _uiState.asStateFlow()

    init {
        if (ticketId == null) {
            _uiState.update {
                it.copy(isLoading = false, missingFields = EditTicketMapper.missingFields(it.form))
            }
        } else {
            viewModelScope.launch {
                repository.observeTicket(ticketId).collect { ticket ->
                    existing.value = ticket
                    if (ticket != null && !formHydratedFromDb) {
                        formHydratedFromDb = true
                        val raw = repository.getRawText(ticketId)
                        val form = EditTicketHydrator.hydrate(ticket, raw, parsers)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isCreate = false,
                                form = form,
                                original = form,
                                missingFields = EditTicketMapper.missingFields(form),
                                saveAttempted = false,
                                needsReview = ticket.needsReview,
                                pdfFileName = ticket.pdfFileName,
                                originalFileName = ticket.originalFileName,
                                extractionMethod = ticket.extractionMethod,
                            )
                        }
                    } else if (ticket == null && formHydratedFromDb) {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
            }
        }
    }

    fun updateForm(transform: (EditTicketForm) -> EditTicketForm) {
        _uiState.update { state ->
            val form = transform(state.form)
            state.copy(
                form = form,
                missingFields = EditTicketMapper.missingFields(form),
            )
        }
    }

    /** Saves the form. Never blocked by missing fields: the ticket stays `needsReview` until they are filled. */
    fun save(onSaved: () -> Unit) {
        val state = _uiState.value
        if (state.isLoading) {
            return
        }
        _uiState.update {
            it.copy(
                saveAttempted = true,
                missingFields = EditTicketMapper.missingFields(state.form),
                saveErrorMessage = null,
            )
        }
        viewModelScope.launch {
            try {
                val ticket = EditTicketMapper.toTicket(state.form, existing.value, clock.instant())
                repository.upsert(ticket, rawText = null)
                onSaved()
            } catch (e: Exception) {
                _uiState.update { it.copy(saveErrorMessage = e.message ?: "") }
            }
        }
    }

    class Factory(
        private val container: AppContainer,
        private val ticketId: String?,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(EditTicketViewModel::class.java)) {
                return EditTicketViewModel(
                    ticketId,
                    container.ticketRepository,
                    listOf(container.ticketParser),
                    container.clock,
                    container.ticketFileStore,
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel $modelClass")
        }
    }
}
