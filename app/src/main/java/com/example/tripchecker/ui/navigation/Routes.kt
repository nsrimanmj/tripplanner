package com.example.tripchecker.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object Home

@Serializable
data object Import

@Serializable
/** Trip detail for the group containing [anchorTicketId]; [openLinkSheet] opens the link-return sheet once on entry. */
data class TripDetail(val anchorTicketId: String, val openLinkSheet: Boolean = false)

@Serializable
data class EditTicket(val ticketId: String?)
