package com.example.catheavenapp.domain.model

/**
 * A bookable window of time on a given day. Mirrors a document in the
 * Firestore `timeslots` collection.
 *
 * bookedCount only changes inside the booking and cancellation transactions,
 * which the security rules check, so the rest of the app treats it as read-only.
 */
data class Timeslot(
    val id: String,
    val dateMillis: Long,
    val startTime: String,   // e.g. "10:00"
    val endTime: String,     // e.g. "11:00"
    val slotType: SlotType,
    val capacity: Int,
    val bookedCount: Int,
    val status: TimeslotStatus
) {
    /** Seats still available. Never negative, even if the data is inconsistent. */
    val remainingCapacity: Int
        get() = (capacity - bookedCount).coerceAtLeast(0)

    /** True when the slot is open and has at least one seat left. */
    val isBookable: Boolean
        get() = status == TimeslotStatus.OPEN && remainingCapacity > 0

    /**
     * Returns the slot as it should look after [guests] have been booked:
     * bookedCount goes up, and the status flips to FULL once capacity is reached.
     * The booking transaction writes exactly this, and the security rules check it.
     */
    fun withBooking(guests: Int): Timeslot {
        require(guests > 0) { "guests must be positive" }
        require(guests <= remainingCapacity) { "not enough capacity for $guests guests" }
        val newCount = bookedCount + guests
        val newStatus = if (newCount >= capacity) TimeslotStatus.FULL else status
        return copy(bookedCount = newCount, status = newStatus)
    }

    /**
     * Returns the slot as it should look after a booking of [guests] is cancelled:
     * the seats are released, and a FULL slot opens again. A CLOSED slot stays closed,
     * because an admin closed it on purpose.
     */
    fun withCancellation(guests: Int): Timeslot {
        require(guests > 0) { "guests must be positive" }
        val newCount = (bookedCount - guests).coerceAtLeast(0)
        val newStatus = if (status == TimeslotStatus.FULL && newCount < capacity) TimeslotStatus.OPEN else status
        return copy(bookedCount = newCount, status = newStatus)
    }
}

enum class SlotType(val value: String) {
    GENERAL_VISIT("generalVisit"),
    SNACK_TIME("snackTime");

    companion object {
        fun fromValue(value: String?): SlotType =
            entries.firstOrNull { it.value == value } ?: GENERAL_VISIT
    }
}

enum class TimeslotStatus(val value: String) {
    OPEN("open"),
    FULL("full"),
    CLOSED("closed");

    companion object {
        /** Unknown or missing values are treated as CLOSED, so bad data can never be booked. */
        fun fromValue(value: String?): TimeslotStatus =
            entries.firstOrNull { it.value == value } ?: CLOSED
    }
}
