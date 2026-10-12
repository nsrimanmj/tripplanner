package com.example.tripchecker.domain

/** Thrown when saving a ticket whose PNR is already stored under another id. */
class DuplicatePnrException(val existingTicketId: String) : Exception("Duplicate PNR")
