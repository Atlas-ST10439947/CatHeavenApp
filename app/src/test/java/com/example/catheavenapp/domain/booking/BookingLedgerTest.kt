package com.example.catheavenapp.domain.booking

import com.example.catheavenapp.domain.model.Booking
import com.example.catheavenapp.domain.model.BookingStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class BookingLedgerTest {

    private fun booking(name: String, start: String, guests: Int, status: BookingStatus = BookingStatus.CONFIRMED) =
        Booking(
            id = name, bookingReference = "CH-2026-$name", supporterId = "u-$name", supporterName = name,
            timeslotId = "slot-$start", slotDateMillis = 0L, slotStart = start, numberOfGuests = guests,
            durationHours = 1.0, pricePerPerson = 120, totalDue = 120 * guests, specialNotes = "",
            status = status, createdAtMillis = 0L
        )

    @Test
    fun forDay_ordersByStartTimeThenName() {
        val ledger = BookingLedger.forDay(
            listOf(booking("Zoe", "14:00", 1), booking("bob", "10:00", 1), booking("Amy", "10:00", 1))
        )
        assertEquals(listOf("Amy", "bob", "Zoe"), ledger.bookings.map { it.supporterName })
    }

    @Test
    fun forDay_totalsOnlyConfirmedBookings() {
        val ledger = BookingLedger.forDay(
            listOf(
                booking("Amy", "10:00", 2),
                booking("Bob", "11:00", 3),
                booking("Cat", "12:00", 4, BookingStatus.CANCELLED)
            )
        )
        assertEquals(2, ledger.confirmedCount)
        assertEquals(5, ledger.guestCount)
        assertEquals(600, ledger.expectedTakings)
        assertEquals(3, ledger.bookings.size)   // cancelled bookings are still listed
    }

    @Test
    fun summary_usesSingularsAndPlurals() {
        assertEquals("1 booking  ·  1 guest  ·  R120 expected", BookingLedger.summary(BookingLedger.forDay(listOf(booking("Amy", "10:00", 1)))))
        assertEquals("0 bookings  ·  0 guests  ·  R0 expected", BookingLedger.summary(BookingLedger.forDay(emptyList())))
    }
}
