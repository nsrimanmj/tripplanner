package com.example.tripchecker.ui.common

import com.example.tripchecker.extraction.parser.IrctcDates
import java.text.NumberFormat
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/** IST display helpers for ticket UI. */
object Formatters {
    private val locale = Locale.ENGLISH
    private val zone = IrctcDates.IST
    private val dateFmt = DateTimeFormatter.ofPattern("EEE, d MMM", locale)
    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm", locale)
    private val monthFmt = DateTimeFormatter.ofPattern("MMMM yyyy", locale)
    private val moneyFmt = NumberFormat.getNumberInstance(Locale.forLanguageTag("en-IN"))

    fun formatDate(date: LocalDate): String = date.format(dateFmt)

    fun formatTime(instant: Instant): String = instant.atZone(zone).format(timeFmt)

    fun formatMonthHeader(date: LocalDate): String = date.format(monthFmt)

    fun formatDuration(start: Instant, end: Instant): String {
        val minutes = Duration.between(start, end).toMinutes().coerceAtLeast(0)
        val h = minutes / 60
        val m = minutes % 60
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }

    fun formatCountdown(now: Instant, departure: Instant): String {
        val depDate = departure.atZone(zone).toLocalDate()
        val nowDate = now.atZone(zone).toLocalDate()
        val depTime = formatTime(departure)
        val until = Duration.between(now, departure)
        return when {
            depDate.isEqual(nowDate.plusDays(1)) -> "Tomorrow · $depTime"
            depDate.isEqual(nowDate) -> "Today · $depTime"
            until.isNegative -> formatDate(depDate) + " · $depTime"
            else -> {
                val days = until.toDays()
                val hours = until.toHours() % 24
                "in ${days}d ${hours}h"
            }
        }
    }

    fun formatMoneyPaise(paise: Long): String {
        val fmt = NumberFormat.getNumberInstance(Locale.forLanguageTag("en-IN"))
        fmt.minimumFractionDigits = 2
        fmt.maximumFractionDigits = 2
        val rupees = paise / 100.0
        return "₹" + fmt.format(rupees)
    }

    fun formatBerthSummary(ticket: com.example.tripchecker.domain.model.Ticket): String {
        val p = ticket.passengers.firstOrNull() ?: return ""
        val status = p.current
        if (status?.waitlistNumber != null) return "${status.code} ${status.waitlistNumber}"
        val coach = status?.coach ?: return ""
        val berth = status.berthNumber?.toString() ?: ""
        val type = status.berthType ?: ""
        return listOf(coach, berth, type).filter { it.isNotBlank() }.joinToString(" · ")
    }
}
