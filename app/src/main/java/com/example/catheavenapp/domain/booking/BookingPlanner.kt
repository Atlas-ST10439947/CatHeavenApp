package com.example.catheavenapp.domain.booking

import com.example.catheavenapp.domain.model.Booking
import com.example.catheavenapp.domain.model.BookingStatus
import com.example.catheavenapp.domain.model.Timeslot
import com.example.catheavenapp.domain.model.TimeslotStatus

/** Why a booking could not be made. The message is shown to the supporter. */
sealed class BookingRejection(val message: String) {
    object NoTimeslot : BookingRejection("Please choose a time slot.")
    object SlotClosed : BookingRejection("This time slot is no longer available. Please choose another.")
    object SlotFull : BookingRejection("This time slot has just been fully booked. Please choose another.")
    data class NotEnoughSpace(val remaining: Int) : BookingRejection(
        if (remaining == 1) "Only 1 place is left in this slot."
        else "Only $remaining places are left in this slot."
    )
    object NoPrice : BookingRejection("This visit length isn't available right now.")
    object InvalidGuests : BookingRejection("Bookings are for 1 to ${BookingValidator.MAX_GUESTS} guests.")
}

sealed interface BookingDecision {
    /** The booking to create and the timeslot as it must look afterwards. */
    data class Accepted(val booking: Booking, val updatedSlot: Timeslot) : BookingDecision
    data class Rejected(val reason: BookingRejection) : BookingDecision
}

/**
 * The double-booking decision, kept free of Firebase so it can be unit tested.
 * The Firestore transaction re-reads the slot, calls [decide], and writes
 * exactly what it returns. If another booking lands first, Firestore retries
 * the transaction with the fresh slot and [decide] runs again.
 */
object BookingPlanner {

    /** e.g. "CH-2026-K3F9QZ". Built from the document ID, so no shared counter is needed. */
    fun referenceFor(bookingId: String, year: Int): String =
        "CH-$year-${bookingId.take(6).uppercase()}"

    fun decide(
        slot: Timeslot,
        draft: BookingDraft,
        quote: PriceQuote?,
        bookingId: String,
        supporterId: String,
        nowMillis: Long,
        year: Int
    ): BookingDecision {
        if (draft.guests !in 1..BookingValidator.MAX_GUESTS) {
            return BookingDecision.Rejected(BookingRejection.InvalidGuests)
        }
        if (slot.status == TimeslotStatus.CLOSED) {
            return BookingDecision.Rejected(BookingRejection.SlotClosed)
        }
        if (slot.status == TimeslotStatus.FULL || slot.remainingCapacity == 0) {
            return BookingDecision.Rejected(BookingRejection.SlotFull)
        }
        if (draft.guests > slot.remainingCapacity) {
            return BookingDecision.Rejected(BookingRejection.NotEnoughSpace(slot.remainingCapacity))
        }
        if (quote == null || quote.guests != draft.guests || quote.durationHours != draft.durationHours) {
            return BookingDecision.Rejected(BookingRejection.NoPrice)
        }

        val booking = Booking(
            id = bookingId,
            bookingReference = referenceFor(bookingId, year),
            supporterId = supporterId,
            supporterName = draft.fullName,
            timeslotId = slot.id,
            slotDateMillis = slot.dateMillis,
            slotStart = slot.startTime,
            numberOfGuests = draft.guests,
            durationHours = draft.durationHours,
            pricePerPerson = quote.pricePerPerson,
            totalDue = quote.total,
            specialNotes = "",
            status = BookingStatus.CONFIRMED,
            createdAtMillis = nowMillis
        )
        return BookingDecision.Accepted(booking, slot.withBooking(draft.guests))
    }
}
