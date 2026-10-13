package com.example.tripchecker.ui.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class FormattersTest {
    private val ist = ZoneId.of("Asia/Kolkata")

    @Test
    fun `formats money en-IN`() {
        assertEquals("₹1,494.05", Formatters.formatMoneyPaise(149405))
    }

    @Test
    fun `formats duration`() {
        val start = Instant.parse("2026-08-03T11:30:00Z")
        val end = Instant.parse("2026-08-04T02:29:00Z")
        assertEquals("14h 59m", Formatters.formatDuration(start, end))
    }

    private fun at(y: Int, m: Int, d: Int, h: Int = 0, min: Int = 0): Instant =
        LocalDateTime.of(y, m, d, h, min).atZone(ist).toInstant()

    private fun d(y: Int, m: Int, day: Int) = LocalDate.of(y, m, day)

    @Test
    fun `formats date with year`() {
        assertEquals("Tue, 4 Aug 2026", Formatters.formatDateWithYear(d(2026, 8, 4)))
    }

    @Test
    fun `hero overline says TODAY for the same IST date`() {
        assertEquals("TODAY · 17:00", Formatters.formatHeroOverline(at(2026, 8, 3, 10), at(2026, 8, 3, 17)))
    }

    @Test
    fun `hero overline says TOMORROW for the next IST date`() {
        assertEquals("TOMORROW · 17:00", Formatters.formatHeroOverline(at(2026, 8, 3, 10), at(2026, 8, 4, 17)))
    }

    @Test
    fun `hero overline uses uppercase date beyond tomorrow`() {
        assertEquals("WED, 5 AUG · 17:00", Formatters.formatHeroOverline(at(2026, 8, 3, 10), at(2026, 8, 5, 17)))
    }

    @Test
    fun `hero overline switches at IST midnight`() {
        assertEquals("TOMORROW · 00:10", Formatters.formatHeroOverline(at(2026, 8, 3, 23, 59), at(2026, 8, 4, 0, 10)))
        assertEquals("TODAY · 23:59", Formatters.formatHeroOverline(at(2026, 8, 3, 0, 0), at(2026, 8, 3, 23, 59)))
    }

    @Test
    fun `hero overline uses the date for past departures`() {
        assertEquals("SUN, 2 AUG · 17:00", Formatters.formatHeroOverline(at(2026, 8, 3, 10), at(2026, 8, 2, 17)))
    }

    @Test
    fun `countdown shows days and hours from 24 hours`() {
        assertEquals("in 2d 4h", Formatters.formatCountdownIn(at(2026, 8, 3, 10), at(2026, 8, 5, 14)))
        assertEquals("in 1d 0h", Formatters.formatCountdownIn(at(2026, 8, 3, 10), at(2026, 8, 4, 10)))
    }

    @Test
    fun `countdown shows hours and minutes below 24 hours`() {
        assertEquals("in 5h 20m", Formatters.formatCountdownIn(at(2026, 8, 3, 10), at(2026, 8, 3, 15, 20)))
        assertEquals("in 23h 59m", Formatters.formatCountdownIn(at(2026, 8, 3, 10), at(2026, 8, 4, 9, 59)))
        assertEquals("in 1h 0m", Formatters.formatCountdownIn(at(2026, 8, 3, 10), at(2026, 8, 3, 11)))
    }

    @Test
    fun `countdown shows minutes below one hour`() {
        assertEquals("in 12m", Formatters.formatCountdownIn(at(2026, 8, 3, 10), at(2026, 8, 3, 10, 12)))
        assertEquals("in 59m", Formatters.formatCountdownIn(at(2026, 8, 3, 10), at(2026, 8, 3, 10, 59)))
    }

    @Test
    fun `countdown says now when departed or under a minute away`() {
        assertEquals("now", Formatters.formatCountdownIn(at(2026, 8, 3, 10), at(2026, 8, 3, 10)))
        assertEquals("now", Formatters.formatCountdownIn(at(2026, 8, 3, 10), at(2026, 8, 3, 9)))
        assertEquals("now", Formatters.formatCountdownIn(at(2026, 8, 3, 10), at(2026, 8, 3, 10).plusSeconds(30)))
    }

    @Test
    fun `day offset is null on the same IST date`() {
        assertNull(Formatters.formatDayOffset(at(2026, 8, 3, 17), at(2026, 8, 3, 23, 59)))
    }

    @Test
    fun `day offset is plus one when arriving the next IST date`() {
        assertEquals("+1", Formatters.formatDayOffset(at(2026, 8, 3, 17), at(2026, 8, 4, 8, 29)))
        assertEquals("+1", Formatters.formatDayOffset(at(2026, 8, 3, 17), at(2026, 8, 4, 0, 0)))
    }

    @Test
    fun `day offset uses IST dates not UTC`() {
        // 18:30Z on 3 Aug is exactly 00:00 IST on 4 Aug.
        val dep = Instant.parse("2026-08-03T11:30:00Z")
        val arr = Instant.parse("2026-08-03T18:30:00Z")
        assertEquals("+1", Formatters.formatDayOffset(dep, arr))
    }

    @Test
    fun `day offset counts several days`() {
        assertEquals("+2", Formatters.formatDayOffset(at(2026, 8, 3, 17), at(2026, 8, 5, 6)))
    }

    @Test
    fun `month header is uppercased`() {
        assertEquals("JULY 2026", Formatters.formatMonthHeaderUpper(d(2026, 7, 14)))
    }

    @Test
    fun `date range within a month`() {
        assertEquals("3 – 10 Aug", Formatters.formatDateRange(d(2026, 8, 3), d(2026, 8, 10)))
    }

    @Test
    fun `date range across months`() {
        assertEquals("28 Jul – 4 Aug", Formatters.formatDateRange(d(2026, 7, 28), d(2026, 8, 4)))
    }

    @Test
    fun `date range across years`() {
        assertEquals("28 Dec 2026 – 4 Jan 2027", Formatters.formatDateRange(d(2026, 12, 28), d(2027, 1, 4)))
    }

    @Test
    fun `date range on a single day shows one date`() {
        assertEquals("3 Aug", Formatters.formatDateRange(d(2026, 8, 3), d(2026, 8, 3)))
    }

    @Test
    fun `long date range shows weekdays`() {
        assertEquals("Mon, 3 Aug – Mon, 10 Aug", Formatters.formatDateRangeLong(d(2026, 8, 3), d(2026, 8, 10)))
    }

    @Test
    fun `trip length is inclusive`() {
        assertEquals(8, Formatters.tripLengthDays(d(2026, 8, 3), d(2026, 8, 10)))
        assertEquals("8 days", Formatters.formatTripLength(d(2026, 8, 3), d(2026, 8, 10)))
        assertEquals("1 day", Formatters.formatTripLength(d(2026, 8, 3), d(2026, 8, 3)))
        assertEquals("2 days", Formatters.formatTripLength(d(2026, 12, 31), d(2027, 1, 1)))
    }

    @Test
    fun `stay label counts IST days between arrival and next departure`() {
        assertEquals(
            "6 days in Hyderabad",
            Formatters.formatStay(at(2026, 8, 4, 8, 29), at(2026, 8, 10, 17), "Hyderabad"),
        )
        assertEquals(
            "1 day in Hyderabad",
            Formatters.formatStay(at(2026, 8, 4, 8, 29), at(2026, 8, 5, 6), "Hyderabad"),
        )
    }

    @Test
    fun `stay label is same-day connection when dates match`() {
        assertEquals(
            "Same-day connection",
            Formatters.formatStay(at(2026, 8, 4, 8, 29), at(2026, 8, 4, 21), "Hyderabad"),
        )
    }

    @Test
    fun `relative days between two dates`() {
        assertEquals("7 days later", Formatters.formatRelativeDays(d(2026, 8, 3), d(2026, 8, 10)))
        assertEquals("1 day later", Formatters.formatRelativeDays(d(2026, 8, 3), d(2026, 8, 4)))
        assertEquals("3 days earlier", Formatters.formatRelativeDays(d(2026, 8, 3), d(2026, 7, 31)))
        assertEquals("1 day earlier", Formatters.formatRelativeDays(d(2026, 8, 3), d(2026, 8, 2)))
        assertEquals("Same day", Formatters.formatRelativeDays(d(2026, 8, 3), d(2026, 8, 3)))
    }

    @Test
    fun `home subtitle covers none one and many trips`() {
        assertEquals("Sat, 1 Aug · 2 trips ahead", Formatters.formatHomeSubtitle(d(2026, 8, 1), 2))
        assertEquals("Sat, 1 Aug · 1 trip ahead", Formatters.formatHomeSubtitle(d(2026, 8, 1), 1))
        assertEquals("Sat, 1 Aug · No trips ahead", Formatters.formatHomeSubtitle(d(2026, 8, 1), 0))
    }

    @Test
    fun `pnr is grouped 3 3 4`() {
        assertEquals("875 165 2729", Formatters.formatPnr("8751652729"))
    }

    @Test
    fun `pnr that is not ten digits is unchanged`() {
        assertEquals("12345", Formatters.formatPnr("12345"))
        assertEquals("87516527290", Formatters.formatPnr("87516527290"))
        assertEquals("875165272A", Formatters.formatPnr("875165272A"))
        assertEquals("875 165 2729", Formatters.formatPnr("875 165 2729"))
        assertEquals("", Formatters.formatPnr(""))
    }

    @Test
    fun `countdown today`() {
        val now = LocalDate.of(2026, 8, 3).atTime(10, 0).atZone(ist).toInstant()
        val dep = LocalDate.of(2026, 8, 3).atTime(17, 0).atZone(ist).toInstant()
        assertEquals("Today · 17:00", Formatters.formatCountdown(now, dep))
    }
}
