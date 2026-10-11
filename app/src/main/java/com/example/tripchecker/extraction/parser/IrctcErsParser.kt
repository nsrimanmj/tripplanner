package com.example.tripchecker.extraction.parser

import com.example.tripchecker.domain.model.Station
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

/** Parser for IRCTC Electronic Reservation Slip text. */
class IrctcErsParser : TicketParser {
    override fun canParse(text: String): Boolean {
        if (!text.contains("PNR", ignoreCase = true)) return false
        return listOf(
            "Electronic Reservation Slip",
            "ERS",
            "IRCTC",
            "Train No",
        ).any { text.contains(it, ignoreCase = true) }
    }

    override fun parse(text: String): ParsedTicket {
        val normalized = text.replace("\r\n", "\n").replace('\t', ' ')
        val rows = normalized.lines()
        val missing = mutableSetOf<TicketField>()
        val warnings = mutableListOf<String>()

        val stations = parseStations(rows, missing)
        val startDate = parseStartDate(normalized, missing)
        val departure = parseDeparture(normalized, startDate, missing, warnings)
        val arrival = parseArrival(normalized, missing)
        val (pnr, pnrDataRowIndex) = parsePnr(rows, missing)
        val pnrLabelRow = rows.indexOfFirst { it.contains(Regex("\\bPNR\\b", RegexOption.IGNORE_CASE)) }
        val detailRow = if (pnrDataRowIndex >= 0) pnrDataRowIndex else pnrLabelRow
        val (travelClass, classLabel) = parseClass(normalized, detailRow, rows, missing)
        val (trainNumber, trainName) = parseTrain(rows, detailRow, travelClass, missing)
        val quotaRange = quotaSearchRange(rows, pnrLabelRow)
        val (quota, quotaLabel) = parseQuota(normalized, quotaRange, missing)
        val distanceKm = parseDistance(normalized, quotaRange, missing)
        val bookedAt = parseBookedAt(normalized, quotaRange, missing)
        val passengers = parsePassengers(rows, warnings)
        if (passengers.isEmpty()) missing.add(TicketField.PASSENGERS)
        val transactionId = parseTransactionId(normalized)
        val totalFarePaise = parseTotalFare(normalized, missing)

        return ParsedTicket(
            pnr = pnr,
            trainNumber = trainNumber,
            trainName = trainName,
            from = stations.first,
            boarding = stations.second,
            to = stations.third,
            startDate = startDate,
            departureAt = departure,
            arrivalAt = arrival,
            travelClass = travelClass,
            classLabel = classLabel,
            quota = quota,
            quotaLabel = quotaLabel,
            distanceKm = distanceKm,
            bookedAt = bookedAt,
            transactionId = transactionId,
            totalFarePaise = totalFarePaise,
            passengers = passengers,
            missingFields = missing,
            warnings = warnings,
        )
    }

    private data class StationTriple(
        val first: Station?,
        val second: Station?,
        val third: Station?,
    )

    private fun parseStations(rows: List<String>, missing: MutableSet<TicketField>): StationTriple {
        val stationRx =
            Regex(
                """([A-Z][A-Z0-9 .'&-]*?)\s*\(\s*([A-Z]{1,5})\s*\)""",
            )
        val skipCodes = CLASS_CODES + QUOTA_CODES
        var startRow =
            rows.indexOfFirst { it.contains(Regex("Booked\\s*From", RegexOption.IGNORE_CASE)) }
        if (startRow < 0) {
            startRow = rows.indexOfFirst { row ->
                stationRx.findAll(row).count { m ->
                    !skipCodes.contains(m.groupValues[2].uppercase())
                } >= 2
            }
        }
        if (startRow < 0) {
            missing.add(TicketField.FROM)
            missing.add(TicketField.TO)
            return StationTriple(null, null, null)
        }
        val matches = mutableListOf<Station>()
        for (i in startRow..minOf(startRow + 2, rows.lastIndex)) {
            for (m in stationRx.findAll(rows[i])) {
                val code = m.groupValues[2].uppercase()
                if (skipCodes.contains(code)) continue
                val name = m.groupValues[1].trim().uppercase()
                matches.add(Station(code = code, name = name))
            }
        }
        return when (matches.size) {
            0 -> {
                missing.add(TicketField.FROM)
                missing.add(TicketField.TO)
                StationTriple(null, null, null)
            }
            1 -> {
                missing.add(TicketField.TO)
                StationTriple(matches[0], matches[0], null)
            }
            2 -> StationTriple(matches[0], matches[0], matches[1])
            else -> StationTriple(matches[0], matches[1], matches[2])
        }
    }

    private fun parseStartDate(text: String, missing: MutableSet<TicketField>): LocalDate? {
        val rx = Regex("""Start\s*Date\*?\s*(\d{1,2}-[A-Za-z]{3}-\d{4})""", RegexOption.IGNORE_CASE)
        val m = rx.find(text) ?: run {
            missing.add(TicketField.START_DATE)
            return null
        }
        return try {
            IrctcDates.parseDate(m.groupValues[1])
        } catch (_: Exception) {
            missing.add(TicketField.START_DATE)
            null
        }
    }

    private fun parseDeparture(
        text: String,
        startDate: LocalDate?,
        missing: MutableSet<TicketField>,
        warnings: MutableList<String>,
    ): Instant? {
        val rx =
            Regex(
                """Departure\*?\s*(\d{1,2}:\d{2})\s*(\d{1,2}-[A-Za-z]{3}-\d{4})""",
                RegexOption.IGNORE_CASE,
            )
        val m = rx.find(text)
        if (m != null) {
            return try {
                IrctcDates.parseInstant(m.groupValues[2], m.groupValues[1])
            } catch (_: Exception) {
                missing.add(TicketField.DEPARTURE_DATE)
                null
            }
        }
        if (startDate != null) {
            warnings.add("Departure time missing; used start of start date")
            return ZonedDateTime.of(startDate, LocalTime.MIDNIGHT, IrctcDates.IST).toInstant()
        }
        missing.add(TicketField.DEPARTURE_DATE)
        return null
    }

    private fun parseArrival(text: String, missing: MutableSet<TicketField>): Instant? {
        val naRx = Regex("""Arrival\*?\s*N\.?\s*A\.?""", RegexOption.IGNORE_CASE)
        if (naRx.containsMatchIn(text)) return null
        val rx =
            Regex(
                """Arrival\*?\s*(\d{1,2}:\d{2})\s*(\d{1,2}-[A-Za-z]{3}-\d{4})""",
                RegexOption.IGNORE_CASE,
            )
        val m = rx.find(text) ?: run {
            missing.add(TicketField.ARRIVAL)
            return null
        }
        return try {
            IrctcDates.parseInstant(m.groupValues[2], m.groupValues[1])
        } catch (_: Exception) {
            missing.add(TicketField.ARRIVAL)
            null
        }
    }

    private fun parsePnr(rows: List<String>, missing: MutableSet<TicketField>): Pair<String?, Int> {
        val txnIndex = rows.indexOfFirst { it.contains(Regex("Transaction\\s*ID", RegexOption.IGNORE_CASE)) }
        val searchEnd = if (txnIndex >= 0) txnIndex else rows.size
        val pnrRow = rows.indexOfFirst { it.contains(Regex("\\bPNR\\b", RegexOption.IGNORE_CASE)) }
        if (pnrRow < 0 || pnrRow >= searchEnd) {
            missing.add(TicketField.PNR)
            return null to -1
        }
        val digitRx = Regex("""(?<!\d)([0-9OoIlS|]{10})(?!\d)""")
        for (i in pnrRow..minOf(pnrRow + 2, searchEnd - 1)) {
            for (m in digitRx.findAll(rows[i])) {
                val capture = m.groupValues[1]
                val realDigits = capture.count { it.isDigit() }
                if (realDigits < 8) continue
                val digits = OcrNormalizer.digits(capture)
                if (digits.length == 10) return digits to i
            }
        }
        missing.add(TicketField.PNR)
        return null to -1
    }

    private fun parseClass(
        text: String,
        pnrRowIndex: Int,
        rows: List<String>,
        missing: MutableSet<TicketField>,
    ): Pair<String?, String?> {
        val classRx = Regex("""\((1A|2A|3A|3E|SL|CC|EC|2S|FC|EA|EV|VS)\)""", RegexOption.IGNORE_CASE)
        val searchBlocks =
            buildList {
                if (pnrRowIndex in rows.indices) add(rows[pnrRowIndex])
                add(text)
            }
        for (block in searchBlocks) {
            val m = classRx.find(block) ?: continue
            val code = m.groupValues[1].uppercase()
            return code to CLASS_LABELS[code]
        }
        missing.add(TicketField.CLASS)
        return null to null
    }

    private fun parseTrain(
        rows: List<String>,
        pnrRowIndex: Int,
        travelClass: String?,
        missing: MutableSet<TicketField>,
    ): Pair<String?, String?> {
        if (pnrRowIndex !in rows.indices) {
            missing.add(TicketField.TRAIN_NUMBER)
            missing.add(TicketField.TRAIN_NAME)
            return null to null
        }
        val row = rows[pnrRowIndex]
        val numRx = Regex("""(\d{5})\s*/\s*""")
        val numMatch = numRx.find(row) ?: run {
            missing.add(TicketField.TRAIN_NUMBER)
            missing.add(TicketField.TRAIN_NAME)
            return null to null
        }
        val trainNumber = numMatch.groupValues[1]
        var namePart = row.substring(numMatch.range.last + 1)
        val classParen =
            travelClass?.let { Regex("""\s*\([^)]*$it[^)]*\)""", RegexOption.IGNORE_CASE).find(namePart) }
        if (classParen != null) {
            namePart = namePart.substring(0, classParen.range.first)
        } else {
            val genericClass = Regex("""\s*\([^)]+\)""").find(namePart)
            if (genericClass != null) namePart = namePart.substring(0, genericClass.range.first)
        }
        val gap = Regex("""\s{2,}""").find(namePart)
        if (gap != null) namePart = namePart.substring(0, gap.range.first)
        val stripSuffix =
            Regex(
                """(\s+(FIRST|SECOND|THIRD|AC|SLEEPER|CLASS|CHAIR|CAR|EXEC\.?|EXECUTIVE|ECONOMY|SITTING|ANUBHUTI|VISTADOME|NON|3))+\s*$""",
                RegexOption.IGNORE_CASE,
            )
        namePart = stripSuffix.replace(namePart, "").trim()
        val trainName = namePart.uppercase().ifEmpty { null }
        if (trainName == null) missing.add(TicketField.TRAIN_NAME)
        return trainNumber to trainName
    }

    private fun quotaSearchRange(rows: List<String>, pnrRowIndex: Int): IntRange {
        val passengerIdx =
            rows.indexOfFirst { it.contains(Regex("Passenger\\s*Details", RegexOption.IGNORE_CASE)) }
        val end = if (passengerIdx >= 0) passengerIdx else rows.size
        val start = if (pnrRowIndex >= 0) pnrRowIndex + 1 else 0
        return start..(end - 1).coerceAtLeast(start)
    }

    private fun parseQuota(text: String, range: IntRange, missing: MutableSet<TicketField>): Pair<String?, String?> {
        val slice = text.lines().filterIndexed { index, _ -> index in range }.joinToString("\n")
        val rx = Regex("""\((GN|TQ|PT|LD|SS|HP|DF|FT|YU|DP|PH|RE)\)""", RegexOption.IGNORE_CASE)
        val m = rx.find(slice) ?: run {
            missing.add(TicketField.QUOTA)
            return null to null
        }
        val code = m.groupValues[1].uppercase()
        val label = QUOTA_LABELS[code] ?: run {
            val before = slice.substring(0, m.range.first).takeLastWhile { it != '(' }.trim()
            before.ifEmpty { null }
        }
        return code to label
    }

    private fun parseDistance(text: String, range: IntRange, missing: MutableSet<TicketField>): Int? {
        val slice = text.lines().filterIndexed { index, _ -> index in range }.joinToString("\n")
        val m = Regex("""(\d{1,4})\s*KM\b""", RegexOption.IGNORE_CASE).find(slice) ?: run {
            missing.add(TicketField.DISTANCE)
            return null
        }
        return m.groupValues[1].toInt()
    }

    private fun parseBookedAt(text: String, range: IntRange, missing: MutableSet<TicketField>): Instant? {
        val slice = text.lines().filterIndexed { index, _ -> index in range }.joinToString("\n")
        val rx =
            Regex(
                """(\d{1,2}-[A-Za-z]{3}-\d{4})\s+(\d{1,2}:\d{2}:\d{2})\s*HRS""",
                RegexOption.IGNORE_CASE,
            )
        val m = rx.find(slice) ?: run {
            missing.add(TicketField.BOOKED_AT)
            return null
        }
        return try {
            IrctcDates.parseInstantWithSeconds(m.groupValues[1], m.groupValues[2])
        } catch (_: Exception) {
            missing.add(TicketField.BOOKED_AT)
            null
        }
    }

    private fun parsePassengers(rows: List<String>, warnings: MutableList<String>): List<ParsedPassengerRow> {
        val headerIdx =
            rows.indexOfFirst { it.contains(Regex("Passenger\\s*Details", RegexOption.IGNORE_CASE)) }
        if (headerIdx < 0) return emptyList()
        val endIdx =
            rows.drop(headerIdx + 1).indexOfFirst { row ->
                row.contains(Regex("Acronyms|Transaction\\s*ID|Payment\\s*Details", RegexOption.IGNORE_CASE))
            }
        val last = if (endIdx >= 0) headerIdx + 1 + endIdx else rows.size
        val rowRx =
            Regex(
                """^\s*(\d{1,2})\s*[.)]?\s+(.+?)\s+(\d{1,3})\s+(M|F|T|MALE|FEMALE|TRANSGENDER)\s+(.+)$""",
                RegexOption.IGNORE_CASE,
            )
        val result = mutableListOf<ParsedPassengerRow>()
        for (i in (headerIdx + 2)..<last) {
            val line = rows[i].trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            val m = rowRx.matchEntire(line) ?: run {
                warnings.add("Skipped passenger row: $line")
                continue
            }
            val tail = normalizeStatusTail(m.groupValues[5])
            val (booking, current) = splitBookingCurrent(tail)
            val gender = m.groupValues[4].uppercase().take(1)
            result.add(
                ParsedPassengerRow(
                    serial = m.groupValues[1].toInt(),
                    name = m.groupValues[2].trim().uppercase(),
                    age = m.groupValues[3].toInt(),
                    gender = gender,
                    bookingStatusRaw = booking,
                    currentStatusRaw = current,
                ),
            )
        }
        return result
    }

    private fun normalizeStatusTail(tail: String): String {
        var s = tail.trim().replace(Regex("\\s*/\\s*"), "/")
        for (phrase in MULTI_WORD_BERTH_TYPES) {
            s = s.replace(phrase, phrase.replace(' ', '_'))
        }
        return s
    }

    private fun splitBookingCurrent(tail: String): Pair<String, String> {
        val columnParts = tail.split(Regex("\\s{2,}")).map { it.trim() }.filter { it.isNotEmpty() }
        if (columnParts.size >= 2) {
            val booking = restoreBerthPhrases(columnParts.first().replace(Regex("\\s*/\\s*"), "/"))
            val current = restoreBerthPhrases(columnParts.last().replace(Regex("\\s*/\\s*"), "/"))
            return booking to current
        }
        val tokens = tail.split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return "" to ""
        if (tokens.size == 1) return restoreBerthPhrases(tokens[0]) to restoreBerthPhrases(tokens[0])
        val booking = restoreBerthPhrases(tokens.first())
        val current = restoreBerthPhrases(tokens.last())
        return booking to current
    }

    private fun restoreBerthPhrases(s: String): String = s.replace('_', ' ')

    private fun parseTransactionId(text: String): String? {
        val rx = Regex("""Transaction\s*ID\s*:?\s*([0-9OoIl]{10,20})""", RegexOption.IGNORE_CASE)
        val m = rx.find(text) ?: return null
        return OcrNormalizer.digits(m.groupValues[1]).ifEmpty { null }
    }

    private fun parseTotalFare(text: String, missing: MutableSet<TicketField>): Long? {
        val rx = Regex("""Total\s*Fare[^\n]*?([\d,]+\.\d{2})""", RegexOption.IGNORE_CASE)
        val m = rx.find(text) ?: run {
            missing.add(TicketField.TOTAL_FARE)
            return null
        }
        val amount = m.groupValues[1].replace(",", "").toBigDecimal()
        return (amount * 100.toBigDecimal()).toLong()
    }

    companion object {
        private val CLASS_CODES =
            setOf("1A", "2A", "3A", "3E", "SL", "CC", "EC", "2S", "FC", "EA", "EV", "VS")
        private val QUOTA_CODES =
            setOf("GN", "TQ", "PT", "LD", "SS", "HP", "DF", "FT", "YU", "DP", "PH", "RE")

        private val CLASS_LABELS =
            mapOf(
                "1A" to "FIRST AC",
                "2A" to "SECOND AC",
                "3A" to "THIRD AC",
                "3E" to "AC 3 ECONOMY",
                "SL" to "SLEEPER CLASS",
                "CC" to "AC CHAIR CAR",
                "EC" to "EXEC. CHAIR CAR",
                "2S" to "SECOND SITTING",
                "FC" to "FIRST CLASS",
                "EA" to "ANUBHUTI CLASS",
                "EV" to "VISTADOME AC",
                "VS" to "VISTADOME NON AC",
            )

        private val QUOTA_LABELS =
            mapOf(
                "GN" to "GENERAL",
                "TQ" to "TATKAL",
                "PT" to "PREMIUM TATKAL",
                "LD" to "LADIES",
                "SS" to "LOWER BERTH/SR.CITIZEN",
                "HP" to "PERSON WITH DISABILITY",
                "DF" to "DEFENCE",
                "FT" to "FOREIGN TOURIST",
                "YU" to "YUVA",
                "DP" to "DUTY PASS",
                "PH" to "PARLIAMENT HOUSE",
                "RE" to "RAILWAY EMPLOYEE",
            )

        private val MULTI_WORD_BERTH_TYPES =
            listOf(
                "SIDE UPPER",
                "SIDE LOWER",
                "SIDE MIDDLE",
                "WINDOW SIDE",
                "NO CHOICE",
            )
    }
}
