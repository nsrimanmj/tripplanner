package com.example.tripchecker.data.repo

import com.example.tripchecker.data.db.dao.TicketDao
import com.example.tripchecker.data.db.toDomain
import com.example.tripchecker.data.db.toEntity
import com.example.tripchecker.data.file.TicketFileStore
import com.example.tripchecker.domain.DuplicatePnrException
import com.example.tripchecker.domain.model.Ticket
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock

/** Room-backed [TicketRepository] with PDF file cleanup on delete. */
class OfflineTicketRepository(
    private val dao: TicketDao,
    private val fileStore: TicketFileStore,
    private val clock: Clock,
) : TicketRepository {
    override fun observeTickets(): Flow<List<Ticket>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeTicket(id: String): Flow<Ticket?> =
        dao.observeById(id).map { it?.toDomain() }

    override suspend fun findByPnr(pnr: String): Ticket? =
        dao.getByPnr(pnr)?.let { dao.getWithPassengersById(it.id)?.toDomain() }

    override suspend fun findBySha(sha: String): Ticket? =
        dao.getBySha(sha)?.let { dao.getWithPassengersById(it.id)?.toDomain() }

    override suspend fun getRawText(ticketId: String): String? = dao.getRawText(ticketId)

    override suspend fun upsert(ticket: Ticket, rawText: String?) {
        ticket.pnr?.let { pnr ->
            dao.getByPnr(pnr)?.takeIf { it.id != ticket.id }?.let {
                throw DuplicatePnrException(it.id)
            }
        }
        val passengers = ticket.passengers.map { it.toEntity(ticket.id) }
        val rawToStore = rawText ?: dao.getRawText(ticket.id)
        dao.upsertWithPassengers(ticket.toEntity(rawToStore), passengers)
    }

    override suspend fun delete(id: String) {
        val existing = dao.getWithPassengersById(id)
        val linkGroup = existing?.ticket?.linkGroupId
        val siblings =
            if (linkGroup != null) {
                dao.getIdsInLinkGroup(linkGroup).filter { it != id }
            } else {
                emptyList()
            }
        dao.deleteById(id)
        fileStore.delete(existing?.ticket?.pdfFileName)
        if (siblings.size == 1) {
            dao.clearLinkGroup(siblings, clock.instant())
        }
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

    override suspend fun referencedPdfFileNames(): Set<String> =
        dao.getAllPdfFileNames().filterNotNull().toSet()
}
