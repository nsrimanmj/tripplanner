package com.example.tripchecker.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "passengers",
    foreignKeys = [
        ForeignKey(
            entity = TicketEntity::class,
            parentColumns = ["id"],
            childColumns = ["ticketId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("ticketId")],
)
data class PassengerEntity(
    @PrimaryKey val id: String,
    val ticketId: String,
    val serial: Int,
    val name: String,
    val age: Int?,
    val gender: String?,
    val bookingStatusRaw: String?,
    val currentStatusRaw: String?,
    val statusCode: String?,
    val coach: String?,
    val berthNumber: Int?,
    val berthType: String?,
    val waitlistNumber: Int?,
)
