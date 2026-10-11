package com.example.tripchecker.data.db.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.tripchecker.data.db.entity.PassengerEntity
import com.example.tripchecker.data.db.entity.TicketEntity

data class TicketWithPassengers(
    @Embedded val ticket: TicketEntity,
    @Relation(parentColumn = "id", entityColumn = "ticketId")
    val passengers: List<PassengerEntity>,
)
