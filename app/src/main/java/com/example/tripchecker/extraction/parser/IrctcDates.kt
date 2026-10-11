package com.example.tripchecker.extraction.parser

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.util.Locale

/** IRCTC date/time parsing; all ticket times are interpreted in IST. */
object IrctcDates {
    val IST: ZoneId = ZoneId.of("Asia/Kolkata")

    private val dateFormatter: DateTimeFormatter =
        DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("d-MMM-yyyy")
            .toFormatter(Locale.ENGLISH)

    private val timeHm: DateTimeFormatter =
        DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("H:mm")
            .toFormatter(Locale.ENGLISH)

    private val timeHms: DateTimeFormatter =
        DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("H:mm:ss")
            .toFormatter(Locale.ENGLISH)

    fun parseDate(text: String): LocalDate = LocalDate.parse(text.trim(), dateFormatter)

    fun parseInstant(dateText: String, timeText: String): Instant {
        val date = parseDate(dateText)
        val time = LocalTime.parse(timeText.trim(), timeHm)
        return ZonedDateTime.of(date, time, IST).toInstant()
    }

    fun parseInstantWithSeconds(dateText: String, timeText: String): Instant {
        val date = parseDate(dateText)
        val time = LocalTime.parse(timeText.trim(), timeHms)
        return ZonedDateTime.of(date, time, IST).toInstant()
    }
}
