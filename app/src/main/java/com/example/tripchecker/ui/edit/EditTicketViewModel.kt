package com.example.tripchecker.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tripchecker.data.repo.TicketRepository
import com.example.tripchecker.di.AppContainer
import com.example.tripchecker.domain.model.Ticket
import com.example.tripchecker.extraction.parser.TicketField
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock

data class EditTicketUiState(
    val isCreate: Boolean = true,
    val isLoading: Boolean = true,
    val form: EditTicketForm = EditTicketForm(),
    val missingFields: Set<TicketField> = emptySet(),
    val saveAttempted: Boolean = false,
)

/** ViewModel for manual create and edit ticket flows. */
class EditTicketViewModel(
    private val ticketId: String?,
    private val repository: TicketRepository,
    private val clock: Clock,
) : ViewModel() {
    private val existing = MutableStateFlow<Ticket?>(null)
    private val _uiState = MutableStateFlow(EditTicketUiState(isCreate = ticketId == null))
    val uiState: StateFlow<EditTicketUiState> = _uiState.asStateFlow()

    init {
        if (ticketId == null) {
            _uiState.update { it.copy(isLoading = false) }
        } else {
            viewModelScope.launch {
                repository.observeTicket(ticketId).collect { ticket ->
                    existing.value = ticket
                    if (ticket != null) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isCreate = false,
                                form = EditTicketMapper.fromTicket(ticket),
                            )
                        }
                    }
                }
            }
        }
    }

    fun updateForm(transform: (EditTicketForm) -> EditTicketForm) {
        _uiState.update { state ->
            val form = transform(state.form)
            state.copy(form = form, missingFields = EditTicketMapper.missingFields(form))
        }
    }

    fun save(onSaved: () -> Unit) {
        val state = _uiState.value
        val missing = EditTicketMapper.missingFields(state.form)
        _uiState.update { it.copy(saveAttempted = true, missingFields = missing) }
        if (missing.intersect(com.example.tripchecker.extraction.parser.REQUIRED_TICKET_FIELDS).isNotEmpty()) {
            return
        }
        viewModelScope.launch {
            val ticket = EditTicketMapper.toTicket(state.form, existing.value, clock.instant())
            repository.upsert(ticket, rawText = null)
            onSaved()
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
                    container.clock,
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel $modelClass")
        }
    }
}
