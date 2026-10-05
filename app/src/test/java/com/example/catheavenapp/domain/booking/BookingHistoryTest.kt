package com.example.catheavenapp.domain.booking

import com.example.catheavenapp.domain.model.Booking
import com.example.catheavenapp.domain.model.BookingStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class BookingHistoryTest {

    private val today = VisitDate(2026, 10, 5)

    private fun booking(
        id: String,
        date: VisitDate,
        start: String = "10:00",
        status: BookingStatus = BookingStatus.CONFIRMED
    ) = Booking(
        id = id,
        bookingReference = "CH-2026-$id",
        supporterId = "user-1",
        supporterName = "Wiehan",
        timeslotId = "slot-$id",
        slotDateMillis = date.toEpochMillis(),
        slotStart = start,
        numberOfGuests = 2,
        durationHours = 1.0,
        pricePerPerson = 120,
        totalDue = 240,
        specialNotes = "",
        status = status,
        createdAtMillis = 0L
    )

    @Test
    fun `future confirmed booking is upcoming`() {
        val history = BookingHistory.from(listOf(booking("a", VisitDate(2026, 10, 9))), today)

        assertEquals(listOf("a"), history.upcoming.map { it.id })
        assertEquals(emptyList<String>(), history.past.map { it.id })
    }

    @Test
    fun `booking for today is still upcoming`() {
        val history = BookingHistory.from(listOf(booking("a", today)), today)

        assertEquals(listOf("a"), history.upcoming.map { it.id })
    }

    @Test
    fun `booking from yesterday is past`() {
        val history = BookingHistory.from(listOf(booking("a", VisitDate(2026, 10, 4))), today)

        assertEquals(listOf("a"), history.past.map { it.id })
        assertEquals(emptyList<String>(), history.upcoming.map { it.id })
    }

    @Test
    fun `cancelled future booking goes to past`() {
        val cancelled = booking("a", VisitDate(2026, 10, 9), status = BookingStatus.CANCELLED)
        val history = BookingHistory.from(listOf(cancelled), today)

        assertEquals(listOf("a"), history.past.map { it.id })
    }

    @Test
    fun `upcoming is sorted soonest first, by date then time`() {
        val history = BookingHistory.from(
            listOf(
                booking("later-day", VisitDate(2026, 10, 9)),
                booking("same-day-afternoon", VisitDate(2026, 10, 6), start = "14:00"),
                booking("same-day-morning", VisitDate(2026, 10, 6), start = "10:00")
            ),
            today
        )

        assertEquals(
            listOf("same-day-morning", "same-day-afternoon", "later-day"),
            history.upcoming.map { it.id }
        )
    }

    @Test
    fun `past is sorted most recent first`() {
        val history = BookingHistory.from(
            listOf(
                booking("august", VisitDate(2026, 8, 28)),
                booking("october", VisitDate(2026, 10, 1)),
                booking("september", VisitDate(2026, 9, 15))
            ),
            today
        )

        assertEquals(listOf("october", "september", "august"), history.past.map { it.id })
    }
}