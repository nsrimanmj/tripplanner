package com.example.tripchecker.domain.text

import com.example.tripchecker.domain.grouping.StationClusters
import com.example.tripchecker.domain.model.Ticket

/** Display-time text helpers; the DB keeps the raw ALL-CAPS ticket text. */
object DisplayText {
    private val alwaysUpper = setOf("JN", "AC", "SF", "SL", "CC", "EC", "CNF", "RAC")
    private val word = Regex("[\\p{L}\\p{N}]+(?:['’][\\p{L}]+)*")

    /**
     * Title-cases ALL-CAPS ticket text ("SAINAGAR SHIRDI" -> "Sainagar Shirdi").
     *
     * A word stays uppercase when it is in [keepUpper] (case-insensitive), a known
     * `StationClusters` code, one of JN/AC/SF/SL/CC/EC/CNF/RAC, has at most two
     * letters, or contains a digit. Null or blank input gives an empty string.
     */
    fun titleCase(raw: String?, keepUpper: Set<String> = emptySet()): String {
        if (raw.isNullOrBlank()) return ""
        val upper = keepUpper.mapTo(HashSet()) { it.uppercase() }
        return word.replace(raw) { m ->
            val w = m.value
            val key = w.uppercase()
            val keep =
                key in upper ||
                    key in alwaysUpper ||
                    isClusterCode(key) ||
                    w.count { it.isLetter() } <= 2 ||
                    w.any { it.isDigit() }
            if (keep) key else titleWord(w)
        }
    }

    /** Station codes of [ticket] (from, boarding, to), uppercased, for use as `keepUpper`. */
    fun keepUpperFor(ticket: Ticket): Set<String> =
        listOfNotNull(ticket.from, ticket.boarding, ticket.to)
            .mapNotNull { it.code.trim().uppercase().ifEmpty { null } }
            .toSet()

    /** [titleCase] with the station codes of [ticket] preserved in uppercase. */
    fun forTicket(raw: String?, ticket: Ticket): String = titleCase(raw, keepUpperFor(ticket))

    private fun isClusterCode(upper: String): Boolean = StationClusters.isKnownCode(upper)

    private fun titleWord(w: String): String = w.lowercase().replaceFirstChar { it.uppercaseChar() }
}
