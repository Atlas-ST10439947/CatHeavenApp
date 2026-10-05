package com.example.catheavenapp.domain.booking

import com.example.catheavenapp.domain.model.BookingRequest
import com.example.catheavenapp.domain.model.PricingTier
import com.example.catheavenapp.domain.model.Timeslot
import com.example.catheavenapp.domain.model.TimeslotStatus

/** Every reason a booking request can be rejected. */
sealed interface BookingError {
    data class InvalidGuestCount(val requested: Int, val max: Int) : BookingError
    data object SlotClosed : BookingError
    data object SlotFull : BookingError
    data class NotEnoughSpace(val requested: Int, val remaining: Int) : BookingError
    data class UnknownDuration(val durationHours: Double) : BookingError
    data class NotesTooLong(val length: Int, val max: Int) : BookingError
}

sealed interface ValidationResult {
    data class Valid(val pricePerPerson: Int, val totalDue: Int) : ValidationResult
    data class Invalid(val error: BookingError) : ValidationResult
}

/**
 * Applies Cat Heaven's booking business rules to a request.
 *
 * The app uses this for instant feedback before submitting. The booking
 * Cloud Function enforces the same rules again on the server inside a
 * transaction, because client-side checks alone can be bypassed.
 */
class BookingValidator(
    private val maxGuests: Int = MAX_GUESTS,
    private val maxNotesLength: Int = MAX_NOTES_LENGTH
) {

    fun validate(
        request: BookingRequest,
        timeslot: Timeslot,
        pricingTiers: List<PricingTier>
    ): ValidationResult {
        val guests = request.numberOfGuests

        if (guests !in 1..maxGuests) {
            return ValidationResult.Invalid(BookingError.InvalidGuestCount(guests, maxGuests))
        }
        if (timeslot.status == TimeslotStatus.CLOSED) {
            return ValidationResult.Invalid(BookingError.SlotClosed)
        }
        if (timeslot.status == TimeslotStatus.FULL || timeslot.remainingCapacity == 0) {
            return ValidationResult.Invalid(BookingError.SlotFull)
        }
        if (guests > timeslot.remainingCapacity) {
            return ValidationResult.Invalid(
                BookingError.NotEnoughSpace(guests, timeslot.remainingCapacity)
            )
        }
        if (request.specialNotes.length > maxNotesLength) {
            return ValidationResult.Invalid(
                BookingError.NotesTooLong(request.specialNotes.length, maxNotesLength)
            )
        }
        val tier = PricingCalculator.tierFor(pricingTiers, request.durationHours)
            ?: return ValidationResult.Invalid(BookingError.UnknownDuration(request.durationHours))

        return ValidationResult.Valid(
            pricePerPerson = tier.pricePerPerson,
            totalDue = tier.pricePerPerson * guests
        )
    }

    companion object {
        const val MAX_GUESTS = 8
        const val MAX_NOTES_LENGTH = 500
    }
}