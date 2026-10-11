package com.example.tripchecker.domain.model

/** One passenger on a ticket. */
data class Passenger(
    val id: String,
    val serial: Int,
    val name: String,
    val age: Int?,
    val gender: String?,
    val bookingStatusRaw: String?,
    val currentStatusRaw: String?,
    val current: BerthStatus?,
)
