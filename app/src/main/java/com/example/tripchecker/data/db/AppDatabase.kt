package com.example.tripchecker.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.tripchecker.data.db.dao.TicketDao
import com.example.tripchecker.data.db.entity.PassengerEntity
import com.example.tripchecker.data.db.entity.TicketEntity

@Database(
    entities = [TicketEntity::class, PassengerEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ticketDao(): TicketDao
}
