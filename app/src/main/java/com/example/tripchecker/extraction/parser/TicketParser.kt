package com.example.tripchecker.extraction.parser

/** Pluggable parser for a specific ticket text format. */
interface TicketParser {
    fun canParse(text: String): Boolean
    fun parse(text: String): ParsedTicket
}
