package com.example.tripchecker.ui.importflow

import com.example.tripchecker.domain.model.ExtractionMethod
import com.example.tripchecker.domain.model.JourneyGroup
import com.example.tripchecker.domain.model.JourneyKind
import com.example.tripchecker.domain.model.Ticket
import com.example.tripchecker.extraction.parser.IrctcDates
import com.example.tripchecker.importer.ImportItem
import com.example.tripchecker.importer.ImportItemState
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** What a row shows; the UI maps each kind to a subtitle string, tile and tap behaviour. */
enum class ImportRowKind { QUEUED, READING, SAVED, SAVED_PAIRED, UPDATED, DUPLICATE, NEEDS_REVIEW, FAILED }

/** What tapping a row does. */
enum class ImportRowTap { NONE, OPEN_TRIP, EDIT_TICKET }

/**
 * Pure presentation model of one import row. [route] ("SNSI → BMT, 3 Aug") and [method] are only set
 * for [ImportRowKind.SAVED] rows whose ticket is known; [failureReason] only for FAILED rows.
 */
data class ImportRowModel(
    val id: String,
    val fileName: String,
    val kind: ImportRowKind,
    val tap: ImportRowTap,
    val targetTicketId: String? = null,
    val route: String? = null,
    val method: ExtractionMethod? = null,
    val failureReason: String? = null,
) {
    /** True when the row is tappable. */
    val clickable: Boolean get() = tap != ImportRowTap.NONE

    /** True when the row shows a trailing chevron (tappable rows except duplicates). */
    val showChevron: Boolean get() = clickable && kind != ImportRowKind.DUPLICATE
}

/** Aggregate numbers for the summary card. */
data class ImportSummary(
    val total: Int = 0,
    val finished: Int = 0,
    val saved: Int = 0,
    val updated: Int = 0,
    val duplicate: Int = 0,
    val needsReview: Int = 0,
    val failed: Int = 0,
) {
    /** True while something is still queued or being read. */
    val inProgress: Boolean get() = finished < total

    /** Progress in 0..1; 0 when there are no items. */
    val fraction: Float get() = if (total == 0) 0f else finished.toFloat() / total
}

/** Builds row models and the summary from import items, stored tickets and derived journeys. */
object ImportRowText {
    private val dateFmt = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)

    /** Counts per state; finished = everything that is neither queued nor reading. */
    fun summarize(items: List<ImportItem>): ImportSummary {
        var saved = 0
        var updated = 0
        var duplicate = 0
        var review = 0
        var failed = 0
        var active = 0
        items.forEach {
            when (it.state) {
                is ImportItemState.Saved -> saved++
                is ImportItemState.Updated -> updated++
                is ImportItemState.Duplicate -> duplicate++
                is ImportItemState.NeedsReview -> review++
                is ImportItemState.Failed -> failed++
                ImportItemState.Queued, ImportItemState.Reading -> active++
            }
        }
        return ImportSummary(items.size, items.size - active, saved, updated, duplicate, review, failed)
    }

    /** Row models for [items]; a ticket missing from [tickets] simply has no route text. */
    fun rows(items: List<ImportItem>, tickets: List<Ticket>, journeys: List<JourneyGroup>): List<ImportRowModel> {
        val byId = tickets.associateBy { it.id }
        val pairedIds =
            journeys.filter { it.kind == JourneyKind.ROUND_TRIP }
                .flatMapTo(HashSet()) { g -> g.legs.map { it.id } }
        return items.map { row(it, byId, pairedIds) }
    }

    private fun row(item: ImportItem, byId: Map<String, Ticket>, pairedIds: Set<String>): ImportRowModel {
        val base = ImportRowModel(item.id, item.displayName, ImportRowKind.QUEUED, ImportRowTap.NONE)
        return when (val s = item.state) {
            ImportItemState.Queued -> base
            ImportItemState.Reading -> base.copy(kind = ImportRowKind.READING)
            is ImportItemState.Saved -> {
                val ticket = byId[s.ticketId]
                val paired = s.ticketId in pairedIds
                base.copy(
                    kind = if (paired) ImportRowKind.SAVED_PAIRED else ImportRowKind.SAVED,
                    tap = ImportRowTap.OPEN_TRIP,
                    targetTicketId = s.ticketId,
                    route = if (paired || ticket == null) null else routeText(ticket),
                    method = if (paired) null else ticket?.extractionMethod,
                )
            }
            is ImportItemState.Updated ->
                base.copy(kind = ImportRowKind.UPDATED, tap = ImportRowTap.OPEN_TRIP, targetTicketId = s.ticketId)
            is ImportItemState.Duplicate ->
                base.copy(
                    kind = ImportRowKind.DUPLICATE,
                    tap = ImportRowTap.OPEN_TRIP,
                    targetTicketId = s.existingTicketId,
                )
            is ImportItemState.NeedsReview ->
                base.copy(
                    kind = ImportRowKind.NEEDS_REVIEW,
                    tap = ImportRowTap.EDIT_TICKET,
                    targetTicketId = s.ticketId,
                )
            is ImportItemState.Failed -> base.copy(kind = ImportRowKind.FAILED, failureReason = s.reason)
        }
    }

    /** "SNSI → BMT, 3 Aug" using station codes and the IST start date; null when no station code is known. */
    fun routeText(ticket: Ticket): String? {
        val from = ticket.origin?.code?.trim()?.uppercase()?.ifEmpty { null }
        val to = ticket.to?.code?.trim()?.uppercase()?.ifEmpty { null }
        if (from == null || to == null) return null
        val stations = "$from → $to"
        val date: LocalDate? = ticket.startDate ?: ticket.departureAt?.atZone(IrctcDates.IST)?.toLocalDate()
        return if (date == null) stations else "$stations, ${date.format(dateFmt)}"
    }
}
