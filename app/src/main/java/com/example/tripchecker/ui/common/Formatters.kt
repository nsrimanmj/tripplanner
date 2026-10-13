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

    private val dateYearFmt = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", locale)
    private val dayMonthFmt = DateTimeFormatter.ofPattern("d MMM", locale)
    private val dayMonthYearFmt = DateTimeFormatter.ofPattern("d MMM yyyy", locale)

    private fun istDate(instant: Instant): LocalDate = instant.atZone(zone).toLocalDate()

    private fun plural(n: Long, unit: String): String = if (n == 1L) "$n $unit" else "$n ${unit}s"

    /** Date with year for the edit form, e.g. "Tue, 4 Aug 2026". */
    fun formatDateWithYear(date: LocalDate): String = date.format(dateYearFmt)

    /** Hero overline: "TODAY · 17:00", "TOMORROW · 17:00", else "MON, 3 AUG · 17:00" (IST dates). */
    fun formatHeroOverline(now: Instant, departure: Instant): String {
        val dayDiff = ChronoUnit.DAYS.between(istDate(now), istDate(departure))
        val time = formatTime(departure)
        return when (dayDiff) {
            0L -> "TODAY · $time"
            1L -> "TOMORROW · $time"
            else -> "${formatDate(istDate(departure)).uppercase(locale)} · $time"
        }
    }

    /** Countdown to departure: "in 2d 4h" (>= 24h), "in 5h 20m" (>= 1h), "in 12m"; "now" when under a minute or past. */
    fun formatCountdownIn(now: Instant, departure: Instant): String {
        val minutes = Duration.between(now, departure).toMinutes()
        return when {
            minutes < 1 -> "now"
            minutes >= 24 * 60 -> "in ${minutes / (24 * 60)}d ${(minutes / 60) % 24}h"
            minutes >= 60 -> "in ${minutes / 60}h ${minutes % 60}m"
            else -> "in ${minutes}m"
        }
    }

    /** "+N" when [arrival] falls N IST dates after [departure]; null on the same date. */
    fun formatDayOffset(departure: Instant, arrival: Instant): String? {
        val days = ChronoUnit.DAYS.between(istDate(departure), istDate(arrival))
        return if (days > 0) "+$days" else null
    }

    /** Month header in caps, e.g. "JULY 2026". */
    fun formatMonthHeaderUpper(date: LocalDate): String = date.format(monthFmt).uppercase(locale)

    /** Compact range: "3 – 10 Aug", "28 Jul – 4 Aug", "28 Dec 2026 – 4 Jan 2027"; a single date when equal. */
    fun formatDateRange(start: LocalDate, end: LocalDate): String =
        when {
            start == end -> start.format(dayMonthFmt)
            start.year != end.year -> "${start.format(dayMonthYearFmt)} – ${end.format(dayMonthYearFmt)}"
            start.month != end.month -> "${start.format(dayMonthFmt)} – ${end.format(dayMonthFmt)}"
            else -> "${start.dayOfMonth} – ${end.format(dayMonthFmt)}"
        }

    /** Detail-screen range with weekdays: "Mon, 3 Aug – Mon, 10 Aug". */
    fun formatDateRangeLong(start: LocalDate, end: LocalDate): String =
        "${formatDate(start)} – ${formatDate(end)}"

    /** Inclusive number of days from [start] to [end] (same day = 1). */
    fun tripLengthDays(start: LocalDate, end: LocalDate): Int =
        (ChronoUnit.DAYS.between(start, end) + 1).toInt().coerceAtLeast(1)

    /** Trip length label: "8 days" or "1 day". */
    fun formatTripLength(start: LocalDate, end: LocalDate): String =
        plural(tripLengthDays(start, end).toLong(), "day")

    /** Stay label between legs: "6 days in Hyderabad", "1 day in Hyderabad" or "Same-day connection" (IST dates). */
    fun formatStay(arrival: Instant, nextDeparture: Instant, city: String): String {
        val days = ChronoUnit.DAYS.between(istDate(arrival), istDate(nextDeparture))
        return if (days <= 0) "Same-day connection" else "${plural(days, "day")} in $city"
    }

    /** Offset of [other] from [base] for the link sheet: "7 days later", "3 days earlier", "Same day". */
    fun formatRelativeDays(base: LocalDate, other: LocalDate): String {
        val days = ChronoUnit.DAYS.between(base, other)
        return when {
            days > 0 -> "${plural(days, "day")} later"
            days < 0 -> "${plural(-days, "day")} earlier"
            else -> "Same day"
        }
    }

    /** Home subtitle: "Sat, 1 Aug · 2 trips ahead", "… · 1 trip ahead" or "… · No trips ahead". */
    fun formatHomeSubtitle(today: LocalDate, tripsAhead: Int): String {
        val ahead = if (tripsAhead <= 0) "No trips ahead" else "${plural(tripsAhead.toLong(), "trip")} ahead"
        return "${formatDate(today)} · $ahead"
    }

    /** Groups a 10-digit PNR as "875 165 2729"; any other input is returned unchanged. */
    fun formatPnr(pnr: String): String =
        if (pnr.length == 10 && pnr.all { it in '0'..'9' }) {
            "${pnr.substring(0, 3)} ${pnr.substring(3, 6)} ${pnr.substring(6)}"
        } else {
            pnr
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
