package com.example.catheavenapp.domain.model

/**
 * A reservation. Mirrors a document in the Firestore `bookings` collection.
 * Slot and pricing details are denormalised so booking lists render
 * without extra reads. Bookings are confirmed on creation.
 */
data class Booking(
    val id: String,
    val bookingReference: String,   // e.g. "CH-2026-00042"
    val supporterId: String,
    val supporterName: String,
    val timeslotId: String,
    val slotDateMillis: Long,
    val slotStart: String,
    val numberOfGuests: Int,
    val durationHours: Double,      // 1, 1.5 or 2
    val pricePerPerson: Int,        // ZAR, from the pricing tier
    val totalDue: Int,              // ZAR, payable at the venue on arrival
    val specialNotes: String,
    val status: BookingStatus,
    val createdAtMillis: Long
)

/** What the supporter has chosen in the booking flow, before it is submitted. */
data class BookingRequest(
    val timeslotId: String,
    val numberOfGuests: Int,
    val durationHours: Double,
    val specialNotes: String = ""
)

enum class BookingStatus(val value: String) {
    CONFIRMED("confirmed"),
    CANCELLED("cancelled"),
    COMPLETED("completed");

    companion object {
        /** Bookings are confirmed on creation, so that is the default for missing values. */
        fun fromValue(value: String?): BookingStatus =
            entries.firstOrNull { it.value == value } ?: CONFIRMED
    }
}
