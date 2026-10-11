package com.example.tripchecker.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object Home

@Serializable
data object Import

@Serializable
data class TripDetail(val anchorTicketId: String)

@Serializable
data class EditTicket(val ticketId: String?)
