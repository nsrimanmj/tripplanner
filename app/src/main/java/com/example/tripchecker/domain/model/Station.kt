package com.example.tripchecker.domain.model

/** IRCTC station code and optional display name. */
data class Station(
    val code: String,
    val name: String?,
)
