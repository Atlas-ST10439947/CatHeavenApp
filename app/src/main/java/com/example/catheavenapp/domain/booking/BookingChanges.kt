package com.example.catheavenapp.domain.booking

import com.example.catheavenapp.domain.model.Booking
import com.example.catheavenapp.domain.model.BookingStatus
import com.example.catheavenapp.domain.model.Timeslot
import com.example.catheavenapp.domain.model.TimeslotStatus

/** Why a supporter's change to their booking was refused. The message is shown on screen. */
sealed class ChangeRejection(val message: String) {
    object NotChangeable : ChangeRejection(
        "Bookings can be changed or cancelled up to the day before your visit. Please contact Cat Heaven."
    )
    object SameSlot : ChangeRejection("That's the time you're already booked for.")
    object NewDateTooSoon : ChangeRejection("Please choose a day from tomorrow onwards.")
    object SlotUnavailable : ChangeRejection("That time slot is no longer available. Please choose another.")
    data class NotEnoughSpace(val remaining: Int) : ChangeRejection(
        if (remaining == 1) "Only 1 place is left in that slot." else "Only $remaining places are left in that slot."
    )
}

sealed interface RescheduleDecision {
    /** The booking on its new slot, and both slots as they must look afterwards. */
    data class Accepted(val booking: Booking, val releasedSlot: Timeslot?, val takenSlot: Timeslot) : RescheduleDecision
    data class Rejected(val reason: ChangeRejection) : RescheduleDecision
}

/**
 * Supporters may cancel or move a confirmed booking up to the day before the visit.
 * The same rules are enforced by firestore.rules (ownerCanChange, isOwnerCancel,
 * isOwnerReschedule), so they can't be bypassed. Kept free of Firebase for unit tests.
 */
object BookingChanges {

    fun canChange(booking: Booking, today: VisitDate): Boolean =
        booking.status == BookingStatus.CONFIRMED && VisitDate.fromEpochMillis(booking.slotDateMillis) > today

    /**
     * [oldSlot] can be null if the original slot was deleted; then there are no seats to release.
     * Guests, visit length and price stay the same: only the day and time move.
     */
    fun reschedule(booking: Booking, oldSlot: Timeslot?, newSlot: Timeslot, today: VisitDate): RescheduleDecision {
        if (!canChange(booking, today)) return RescheduleDecision.Rejected(ChangeRejection.NotChangeable)
        if (newSlot.id == booking.timeslotId) return RescheduleDecision.Rejected(ChangeRejection.SameSlot)
        if (VisitDate.fromEpochMillis(newSlot.dateMillis) <= today) {
            return RescheduleDecision.Rejected(ChangeRejection.NewDateTooSoon)
        }
        if (newSlot.status != TimeslotStatus.OPEN || newSlot.remainingCapacity == 0) {
            return RescheduleDecision.Rejected(ChangeRejection.SlotUnavailable)
        }
        if (booking.numberOfGuests > newSlot.remainingCapacity) {
            return RescheduleDecision.Rejected(ChangeRejection.NotEnoughSpace(newSlot.remainingCapacity))
        }
        val moved = booking.copy(timeslotId = newSlot.id, slotDateMillis = newSlot.dateMillis, slotStart = newSlot.startTime)
        return RescheduleDecision.Accepted(
            booking = moved,
            releasedSlot = oldSlot?.withCancellation(booking.numberOfGuests),
            takenSlot = newSlot.withBooking(booking.numberOfGuests)
        )
    }
}
