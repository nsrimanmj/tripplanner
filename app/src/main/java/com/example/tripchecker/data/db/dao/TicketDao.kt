package com.example.tripchecker.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
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

    @Transaction
    @Query("SELECT * FROM tickets WHERE id = :id")
    suspend fun getWithPassengersById(id: String): TicketWithPassengers?

    @Query("SELECT * FROM tickets WHERE pnr = :pnr LIMIT 1")
    suspend fun getByPnr(pnr: String): TicketEntity?

    @Query("SELECT * FROM tickets WHERE pdfSha256 = :sha LIMIT 1")
    suspend fun getBySha(sha: String): TicketEntity?

    @Query("SELECT rawText FROM tickets WHERE id = :id")
    suspend fun getRawText(id: String): String?

    @Query("SELECT pdfFileName FROM tickets WHERE pdfFileName IS NOT NULL")
    suspend fun getAllPdfFileNames(): List<String>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTicket(ticket: TicketEntity)

    @Update
    suspend fun updateTicket(ticket: TicketEntity): Int

    @Query("DELETE FROM passengers WHERE ticketId = :ticketId")
    suspend fun deletePassengersForTicket(ticketId: String)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPassengers(passengers: List<PassengerEntity>)

    @Transaction
    suspend fun upsertWithPassengers(ticket: TicketEntity, passengers: List<PassengerEntity>) {
        val rows = updateTicket(ticket)
        if (rows == 0) {
            insertTicket(ticket)
        }
        deletePassengersForTicket(ticket.id)
        if (passengers.isNotEmpty()) insertPassengers(passengers)
    }

    @Query("DELETE FROM tickets WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT id FROM tickets WHERE linkGroupId = :groupId")
    suspend fun getIdsInLinkGroup(groupId: String): List<String>

    @Query("UPDATE tickets SET linkGroupId = :groupId, autoPairDisabled = :autoPairDisabled, updatedAt = :updatedAt WHERE id IN (:ids)")
    suspend fun setLinkGroup(ids: List<String>, groupId: String?, autoPairDisabled: Boolean, updatedAt: Instant)

    @Query("UPDATE tickets SET isCancelled = :value, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setCancelled(id: String, value: Boolean, updatedAt: Instant)

    @Query("UPDATE tickets SET linkGroupId = NULL, updatedAt = :updatedAt WHERE id IN (:ids)")
    suspend fun clearLinkGroup(ids: List<String>, updatedAt: Instant)
}
