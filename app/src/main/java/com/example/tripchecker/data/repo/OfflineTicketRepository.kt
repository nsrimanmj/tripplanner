package com.example.tripchecker.data.repo

import com.example.tripchecker.data.db.dao.TicketDao
import com.example.tripchecker.data.db.toDomain
import com.example.tripchecker.data.db.toEntity
import com.example.tripchecker.data.file.TicketFileStore
import com.example.tripchecker.domain.model.Ticket
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Clock

class OfflineTicketRepository(
    private val dao: TicketDao,
    private val fileStore: TicketFileStore,
    private val clock: Clock,
) : TicketRepository {
    override fun observeTickets(): Flow<List<Ticket>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeTicket(id: String): Flow<Ticket?> =
        dao.observeById(id).map { it?.toDomain() }

    override suspend fun findByPnr(pnr: String): Ticket? {
        val entity = dao.getByPnr(pnr) ?: return null
        return dao.observeById(entity.id).map { it?.toDomain() }.first()
    }

    override suspend fun findBySha(sha: String): Ticket? {
        val entity = dao.getBySha(sha) ?: return null
        return dao.observeById(entity.id).map { it?.toDomain() }.first()
    }

    override suspend fun upsert(ticket: Ticket, rawText: String?) {
        val passengers = ticket.passengers.map { it.toEntity(ticket.id) }
        dao.upsertWithPassengers(ticket.toEntity(rawText), passengers)
    }

    override suspend fun delete(id: String) {
        val existing = dao.observeById(id).map { it?.ticket }.first()
        dao.deleteById(id)
        fileStore.delete(existing?.pdfFileName)
    }

    override suspend fun setCancelled(id: String, value: Boolean) {
        dao.setCancelled(id, value, clock.instant())
    }

    override suspend fun link(ids: List<String>, groupId: String) {
        dao.setLinkGroup(ids, groupId, autoPairDisabled = false, updatedAt = clock.instant())
    }

    override suspend fun unlink(ids: List<String>) {
        dao.setLinkGroup(ids, groupId = null, autoPairDisabled = true, updatedAt = clock.instant())
    }

    override suspend fun resetPairing(ids: List<String>) {
        dao.setLinkGroup(ids, groupId = null, autoPairDisabled = false, updatedAt = clock.instant())
    }

}
