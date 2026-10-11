package com.example.tripchecker.extraction.parser

import com.example.tripchecker.domain.model.BerthStatus

/** Parses IRCTC booking/current status strings into structured berth data. */
object PassengerStatusParser {
    private val waitlistRx = Regex("^(RLWL|PQWL|GNWL|TQWL|RSWL|WL|RAC)/(\\d+)$", RegexOption.IGNORE_CASE)
    private val berthRx =
        Regex(
            "^(CNF|RAC)/([^/]+)/(\\d+)(?:/(.+))?$",
            RegexOption.IGNORE_CASE,
        )

    fun parse(raw: String?): BerthStatus? {
        if (raw.isNullOrBlank()) return null
        val normalized =
            raw.trim()
                .replace(Regex("\\s*/\\s*"), "/")
                .replace(Regex("/+"), "/")
        if (normalized.equals("CAN", ignoreCase = true)) {
            return BerthStatus(code = "CAN")
        }
        waitlistRx.matchEntire(normalized)?.let { m ->
            return BerthStatus(
                code = m.groupValues[1].uppercase(),
                waitlistNumber = m.groupValues[2].toInt(),
            )
        }
        berthRx.matchEntire(normalized)?.let { m ->
            val berthType = m.groupValues.getOrNull(4)?.replace('_', ' ')?.trim()?.ifEmpty { null }
            return BerthStatus(
                code = m.groupValues[1].uppercase(),
                coach = m.groupValues[2],
                berthNumber = m.groupValues[3].toInt(),
                berthType = berthType,
            )
        }
        return BerthStatus(code = normalized.uppercase())
    }
}
