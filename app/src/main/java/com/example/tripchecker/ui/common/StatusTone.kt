package com.example.tripchecker.ui.common

import com.example.tripchecker.domain.model.BerthStatus

/** Semantic status category used to pick chip colour and label. */
enum class StatusKind { CONFIRMED, RAC, WAITLIST, CANCELLED, UNKNOWN }

/** Colour tone for a chip; the UI maps it to an M3 container role. */
enum class StatusTone { PRIMARY, SECONDARY, TERTIARY, ERROR, NEUTRAL }

/** Pure mapping from a passenger [BerthStatus] to a [StatusKind] and [StatusTone]. */
object StatusTones {
    /** Classifies [status] by its code: CNF, RAC, any `WL`/`*WL`, CAN, otherwise unknown. */
    fun kindOf(status: BerthStatus?): StatusKind {
        val code = status?.code?.trim()?.uppercase().orEmpty()
        return when {
            code == "CNF" -> StatusKind.CONFIRMED
            code == "RAC" -> StatusKind.RAC
            code == "CAN" -> StatusKind.CANCELLED
            code.endsWith("WL") -> StatusKind.WAITLIST
            else -> StatusKind.UNKNOWN
        }
    }

    /** Maps a [StatusKind] to the chip colour tone. */
    fun toneOf(kind: StatusKind): StatusTone =
        when (kind) {
            StatusKind.CONFIRMED -> StatusTone.PRIMARY
            StatusKind.RAC -> StatusTone.SECONDARY
            StatusKind.WAITLIST -> StatusTone.TERTIARY
            StatusKind.CANCELLED -> StatusTone.ERROR
            StatusKind.UNKNOWN -> StatusTone.NEUTRAL
        }
}
