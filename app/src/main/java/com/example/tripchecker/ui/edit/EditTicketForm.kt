package com.example.tripchecker.ui.edit

import com.example.tripchecker.domain.model.ExtractionMethod
import com.example.tripchecker.domain.model.Passenger
import com.example.tripchecker.domain.model.Station
import com.example.tripchecker.domain.model.Ticket
import com.example.tripchecker.extraction.parser.PassengerStatusParser
import com.example.tripchecker.extraction.parser.IrctcDates
import com.example.tripchecker.extraction.parser.REQUIRED_TICKET_FIELDS
import com.example.tripchecker.extraction.parser.TicketField
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.UUID

/** Editable fields for create / edit ticket. */
data class EditTicketForm(
    val pnr: String = "",
    val trainNumber: String = "",
    val trainName: String = "",
    val fromCode: String = "",
    val fromName: String = "",
    val boardingCode: String = "",
    val boardingName: String = "",
    val toCode: String = "",
    val toName: String = "",
    val startDate: LocalDate? = null,
    val departureDate: LocalDate? = null,
    val departureTime: LocalTime? = null,
    val arrivalDate: LocalDate? = null,
    val arrivalTime: LocalTime? = null,
    val travelClass: String = "",
    val classLabel: String = "",
    val quota: String = "",
    val quotaLabel: String = "",
    val distanceKm: String = "",
    val totalFareRupees: String = "",
    val passengers: List<PassengerFormRow> = listOf(PassengerFormRow()),
    val notes: String = "",
)

data class PassengerFormRow(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val age: String = "",
    val gender: String = "",
    val currentStatus: String = "",
    val bookingStatus: String = "",
)

/** Maps between [Ticket] and [EditTicketForm]; validates required fields. */
object EditTicketMapper {
    fun fromTicket(ticket: Ticket): EditTicketForm {
        val dep = ticket.departureAt?.atZone(IrctcDates.IST)
        val arr = ticket.arrivalAt?.atZone(IrctcDates.IST)
        return EditTicketForm(
            pnr = ticket.pnr ?: "",
            trainNumber = ticket.trainNumber ?: "",
            trainName = ticket.trainName ?: "",
            fromCode = ticket.from?.code ?: "",
            fromName = ticket.from?.name ?: "",
            boardingCode = ticket.boarding?.code ?: "",
            boardingName = ticket.boarding?.name ?: "",
            toCode = ticket.to?.code ?: "",
            toName = ticket.to?.name ?: "",
            startDate = ticket.startDate,
            departureDate = dep?.toLocalDate(),
            departureTime = dep?.toLocalTime(),
            arrivalDate = arr?.toLocalDate(),
            arrivalTime = arr?.toLocalTime(),
            travelClass = ticket.travelClass ?: "",
            classLabel = ticket.classLabel ?: "",
            quota = ticket.quota ?: "",
            quotaLabel = ticket.quotaLabel ?: "",
            distanceKm = ticket.distanceKm?.toString() ?: "",
            totalFareRupees = ticket.totalFarePaise?.let { (it / 100.0).toString() } ?: "",
            passengers =
                ticket.passengers.map {
                    PassengerFormRow(
                        id = it.id,
                        name = it.name,
                        age = it.age?.toString() ?: "",
                        gender = it.gender ?: "",
                        currentStatus = it.currentStatusRaw ?: "",
                        bookingStatus = it.bookingStatusRaw ?: "",
                    )
                }.ifEmpty { listOf(PassengerFormRow()) },
            notes = ticket.notes ?: "",
        )
    }

    fun missingFields(form: EditTicketForm): Set<TicketField> {
        val missing = mutableSetOf<TicketField>()
        if (form.pnr.isBlank()) missing += TicketField.PNR
        if (form.trainNumber.isBlank()) missing += TicketField.TRAIN_NUMBER
        if (form.fromCode.isBlank()) missing += TicketField.FROM
        if (form.toCode.isBlank()) missing += TicketField.TO
        if (form.departureDate == null || form.departureTime == null) missing += TicketField.DEPARTURE_DATE
        return missing
    }

    fun toTicket(
        form: EditTicketForm,
        existing: Ticket?,
        now: Instant,
    ): Ticket {
        val id = existing?.id ?: UUID.randomUUID().toString()
        val departureAt = combineInstant(form.departureDate, form.departureTime)
        val arrivalAt = combineInstant(form.arrivalDate, form.arrivalTime)
        val passengers =
            form.passengers
                .filter { it.name.isNotBlank() }
                .mapIndexed { index, row ->
                    Passenger(
                        id = row.id,
                        serial = index + 1,
                        name = row.name.trim(),
                        age = row.age.toIntOrNull(),
                        gender = row.gender.trim().ifBlank { null },
                        bookingStatusRaw = row.bookingStatus.trim().ifBlank { null },
                        currentStatusRaw = row.currentStatus.trim().ifBlank { null },
                        current =
                            PassengerStatusParser.parse(row.currentStatus)
                                ?: PassengerStatusParser.parse(row.bookingStatus),
                    )
                }
        val farePaise =
            form.totalFareRupees.toDoubleOrNull()?.let { (it * 100).toLong() }
        val needsReview = missingFields(form).intersect(REQUIRED_TICKET_FIELDS).isNotEmpty()
        return Ticket(
            id = id,
            pnr = form.pnr.trim().ifBlank { null },
            trainNumber = form.trainNumber.trim().ifBlank { null },
            trainName = form.trainName.trim().ifBlank { null },
            from = station(form.fromCode, form.fromName),
            boarding = station(form.boardingCode, form.boardingName),
            to = station(form.toCode, form.toName),
            startDate = form.startDate,
            departureAt = departureAt,
            arrivalAt = arrivalAt,
            travelClass = form.travelClass.trim().ifBlank { null },
            classLabel = form.classLabel.trim().ifBlank { null },
            quota = form.quota.trim().ifBlank { null },
            quotaLabel = form.quotaLabel.trim().ifBlank { null },
            distanceKm = form.distanceKm.toIntOrNull(),
            bookedAt = existing?.bookedAt,
            transactionId = existing?.transactionId,
            totalFarePaise = farePaise,
            passengers = passengers,
            pdfFileName = existing?.pdfFileName,
            pdfSha256 = existing?.pdfSha256,
            originalFileName = existing?.originalFileName,
            extractionMethod = existing?.extractionMethod ?: ExtractionMethod.MANUAL,
            needsReview = needsReview,
            linkGroupId = existing?.linkGroupId,
            autoPairDisabled = existing?.autoPairDisabled ?: false,
            isCancelled = existing?.isCancelled ?: false,
            notes = form.notes.trim().ifBlank { null },
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )
    }

    fun datePickerMillisToLocalDate(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

    private fun station(code: String, name: String): Station? {
        val c = code.trim().uppercase()
        if (c.isBlank()) return null
        return Station(c, name.trim().ifBlank { null })
    }

    private fun combineInstant(date: LocalDate?, time: LocalTime?): Instant? {
        if (date == null || time == null) return null
        return ZonedDateTime.of(date, time, IrctcDates.IST).toInstant()
    }
}
