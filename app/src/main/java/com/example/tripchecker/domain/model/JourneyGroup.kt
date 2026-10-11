package com.example.tripchecker.domain.model

/** Derived grouping of one or more tickets into a journey card. */
enum class JourneyKind {
    ROUND_TRIP,
    ONE_WAY,
    CUSTOM,
}

enum class JourneyStatus {
    UPCOMING,
    IN_PROGRESS,
    PAST,
}

data class JourneyGroup(
    val key: String,
    val kind: JourneyKind,
    val legs: List<Ticket>,
    val isManual: Boolean,
    val status: JourneyStatus,
)
