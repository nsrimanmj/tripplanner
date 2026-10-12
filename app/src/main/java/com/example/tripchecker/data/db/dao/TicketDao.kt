package com.example.tripchecker.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.example.tripchecker.data.db.entity.PassengerEntity
import com.example.tripchecker.data.db.entity.TicketEntity
import com.example.tripchecker.data.db.relation.TicketWithPassengers
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface TicketDao {
    @Transaction
    @Query("SELECT * FROM tickets ORDER BY departureAt ASC")
    fun observeAll(): Flow<List<TicketWithPassengers>>

    @Transaction
    @Query("SELECT * FROM tickets WHERE id = :id")
    fun observeById(id: String): Flow<TicketWithPassengers?>

    @Query("SELECT * FROM tickets WHERE pnr = :pnr LIMIT 1")
    suspend fun getByPnr(pnr: String): TicketEntity?

    @Query("SELECT * FROM tickets WHERE pdfSha256 = :sha LIMIT 1")
    suspend fun getBySha(sha: String): TicketEntity?

    @Query("SELECT rawText FROM tickets WHERE id = :id")
    suspend fun getRawText(id: String): String?

    @Upsert
    suspend fun upsertTicket(ticket: TicketEntity)

    @Query("DELETE FROM passengers WHERE ticketId = :ticketId")
    suspend fun deletePassengersForTicket(ticketId: String)

    @Upsert
    suspend fun insertPassengers(passengers: List<PassengerEntity>)

    @Transaction
    suspend fun upsertWithPassengers(ticket: TicketEntity, passengers: List<PassengerEntity>) {
        upsertTicket(ticket)
        deletePassengersForTicket(ticket.id)
        if (passengers.isNotEmpty()) insertPassengers(passengers)
    }

    @Query("DELETE FROM tickets WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE tickets SET linkGroupId = :groupId, autoPairDisabled = :autoPairDisabled, updatedAt = :updatedAt WHERE id IN (:ids)")
    suspend fun setLinkGroup(ids: List<String>, groupId: String?, autoPairDisabled: Boolean, updatedAt: Instant)

    @Query("UPDATE tickets SET isCancelled = :value, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setCancelled(id: String, value: Boolean, updatedAt: Instant)
}
