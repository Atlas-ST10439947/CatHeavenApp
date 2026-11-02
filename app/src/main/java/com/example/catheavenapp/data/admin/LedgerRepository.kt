package com.example.catheavenapp.data.admin

import com.example.catheavenapp.data.booking.toBooking
import com.example.catheavenapp.data.booking.toTimeslot
import com.example.catheavenapp.domain.booking.VisitDate
import com.example.catheavenapp.domain.model.Booking
import com.example.catheavenapp.domain.model.BookingStatus
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Date

/** The admin Booking Ledger: every supporter's bookings for a day. Admin only. */
interface LedgerRepository {
    suspend fun bookingsOn(date: VisitDate): List<Booking>

    /**
     * Cancels a confirmed booking and gives its seats back to the timeslot,
     * both in one transaction. Does nothing if it is already cancelled.
     */
    suspend fun cancel(bookingId: String)
}

class FirestoreLedgerRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : LedgerRepository {

    override suspend fun bookingsOn(date: VisitDate): List<Booking> {
        val dayStart = date.toEpochMillis()
        val dayEnd = VisitDate.fromEpochMillis(dayStart + DAY_AND_A_BIT).toEpochMillis()   // safe across DST changes
        // A range on one field needs no composite index. Only admins may run this
        // query, because it returns other supporters' bookings.
        return db.collection(BOOKINGS)
            .whereGreaterThanOrEqualTo("slotDate", Timestamp(Date(dayStart)))
            .whereLessThan("slotDate", Timestamp(Date(dayEnd)))
            .get()
            .await()
            .documents
            .mapNotNull { it.toBooking() }
    }

    override suspend fun cancel(bookingId: String) {
        val bookingRef = db.collection(BOOKINGS).document(bookingId)
        db.runTransaction { tx ->
            // All reads come before any writes in a Firestore transaction
            val booking = tx.get(bookingRef).toBooking() ?: return@runTransaction
            if (booking.status != BookingStatus.CONFIRMED) return@runTransaction
            val slotRef = db.collection(TIMESLOTS).document(booking.timeslotId)
            val slot = tx.get(slotRef).toTimeslot()

            tx.update(
                bookingRef, mapOf(
                    "status" to BookingStatus.CANCELLED.value,
                    "cancelledAt" to FieldValue.serverTimestamp(),
                    "cancelledBy" to "admin"
                )
            )
            if (slot != null && booking.numberOfGuests > 0) {
                val released = slot.withCancellation(booking.numberOfGuests)
                tx.update(slotRef, mapOf("bookedCount" to released.bookedCount, "status" to released.status.value))
            }
        }.await()
    }

    private companion object {
        const val BOOKINGS = "bookings"
        const val TIMESLOTS = "timeslots"
        const val DAY_AND_A_BIT = 26L * 60 * 60 * 1000
    }
}
