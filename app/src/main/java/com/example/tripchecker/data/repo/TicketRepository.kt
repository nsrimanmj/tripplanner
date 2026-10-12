package com.example.tripchecker.data.repo

import com.example.tripchecker.domain.model.Ticket
import kotlinx.coroutines.flow.Flow

/** Persistence boundary for tickets. */
interface TicketRepository {
    fun observeTickets(): Flow<List<Ticket>>
    fun observeTicket(id: String): Flow<Ticket?>
    suspend fun findByPnr(pnr: String): Ticket?
    suspend fun findBySha(sha: String): Ticket?
    suspend fun getRawText(ticketId: String): String?
    suspend fun upsert(ticket: Ticket, rawText: String? = null)
    suspend fun delete(id: String)
    suspend fun setCancelled(id: String, value: Boolean)
    suspend fun link(ids: List<String>, groupId: String)
    suspend fun unlink(ids: List<String>)
    suspend fun resetPairing(ids: List<String>)
}
