package com.example.tripchecker.ui.edit

import com.example.tripchecker.extraction.parser.PassengerStatusParser

/** Explains, in plain words, how a typed passenger status string will be read. Pure and JVM-testable. */
object StatusReadback {
    /** Outcome of interpreting a status string. */
    sealed interface Result {
        /** Nothing typed, so no explanation is shown. */
        data object Empty : Result

        /** The status was understood; [summary] reads like "coach B4, berth 33, lower". */
        data class Recognised(val summary: String) : Result

        /** The status could not be understood and will be stored as typed. */
        data object Unrecognised : Result
    }

    private val knownWaitlistCodes = setOf("RLWL", "PQWL", "GNWL", "TQWL", "RSWL", "WL")

    fun of(raw: String?): Result {
        val status = PassengerStatusParser.parse(raw) ?: return Result.Empty
        val code = status.code
        val waitlist = status.waitlistNumber
        val coach = status.coach
        val berth = status.berthNumber
        return when {
            code == "CAN" -> Result.Recognised("cancelled")
            waitlist != null && code == "RAC" -> Result.Recognised("RAC, position $waitlist")
            waitlist != null && code in knownWaitlistCodes -> Result.Recognised("$code, waitlist $waitlist")
            coach != null && berth != null && (code == "CNF" || code == "RAC") ->
                Result.Recognised(berthSummary(code, coach, berth, status.berthType))
            code == "CNF" -> Result.Recognised("confirmed, berth not assigned yet")
            else -> Result.Unrecognised
        }
    }

    private fun berthSummary(code: String, coach: String, berth: Int, type: String?): String {
        val parts = mutableListOf<String>()
        if (code == "RAC") parts += "RAC"
        parts += "coach $coach"
        parts += "berth $berth"
        type?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }?.let { parts += it }
        return parts.joinToString(", ")
    }
}
