package com.example.catheavenapp.domain.booking

import com.example.catheavenapp.domain.model.Booking
import com.example.catheavenapp.domain.model.BookingStatus

/** One day of the admin Booking Ledger. */
data class DayLedger(
    val bookings: List<Booking>,   // in visit order
    val confirmedCount: Int,
    val guestCount: Int,
    val expectedTakings: Int       // ZAR, paid at the venue
)

/** Builds the ledger summary. Kept free of Firebase so it can be unit tested. */
object BookingLedger {

    fun forDay(bookings: List<Booking>): DayLedger {
        // slotStart is zero-padded "HH:mm", so text order is time order
        val ordered = bookings.sortedWith(compareBy<Booking>({ it.slotStart }, { it.supporterName.lowercase() }))
        val confirmed = ordered.filter { it.status == BookingStatus.CONFIRMED }
        return DayLedger(
            bookings = ordered,
            confirmedCount = confirmed.size,
            guestCount = confirmed.sumOf { it.numberOfGuests },
            expectedTakings = confirmed.sumOf { it.totalDue }
        )
    }

    /** e.g. "3 bookings · 7 guests · R840 expected". */
    fun summary(ledger: DayLedger): String {
        val bookings = if (ledger.confirmedCount == 1) "1 booking" else "${ledger.confirmedCount} bookings"
        val guests = if (ledger.guestCount == 1) "1 guest" else "${ledger.guestCount} guests"
        return "$bookings  ·  $guests  ·  R${ledger.expectedTakings} expected"
    }
}
