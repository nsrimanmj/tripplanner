package com.example.tripchecker.domain.model

/** Parsed coach/berth or waitlist status from a passenger status string. */
data class BerthStatus(
    val code: String,
    val coach: String? = null,
    val berthNumber: Int? = null,
    val berthType: String? = null,
    val waitlistNumber: Int? = null,
)
