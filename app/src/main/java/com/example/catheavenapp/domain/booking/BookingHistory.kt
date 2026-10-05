package com.example.catheavenapp.domain.booking

import com.example.catheavenapp.domain.model.Booking
import com.example.catheavenapp.domain.model.BookingStatus

/** A supporter's bookings, split into the My Bookings tabs. */
data class BookingHistory(
    val upcoming: List<Booking>,
    val past: List<Booking>
) {
    companion object {
        /**
         * Upcoming: confirmed bookings on [today] or later, soonest first.
         * Past: everything else (earlier days, cancelled or completed), most recent first.
         * Kept free of Firebase so it can be unit tested.
         */
        fun from(bookings: List<Booking>, today: VisitDate): BookingHistory {
            val (upcoming, past) = bookings.partition {
                it.status == BookingStatus.CONFIRMED &&
                    VisitDate.fromEpochMillis(it.slotDateMillis) >= today
            }
            // slotStart is zero-padded "HH:mm", so text order is time order
            val byWhen = compareBy<Booking>({ it.slotDateMillis }, { it.slotStart })
            return BookingHistory(
                upcoming = upcoming.sortedWith(byWhen),
                past = past.sortedWith(byWhen.reversed())
            )
        }
    }
}
