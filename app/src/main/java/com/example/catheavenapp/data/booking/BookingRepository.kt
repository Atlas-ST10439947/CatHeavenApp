package com.example.catheavenapp.data.booking

import com.example.catheavenapp.domain.booking.BookingDecision
import com.example.catheavenapp.domain.booking.BookingDraft
import com.example.catheavenapp.domain.booking.BookingPlanner
import com.example.catheavenapp.domain.booking.BookingRejection
import com.example.catheavenapp.domain.booking.PriceQuote
import com.example.catheavenapp.domain.booking.VisitDate
import com.example.catheavenapp.domain.model.Booking
import com.example.catheavenapp.domain.model.BookingStatus
import com.example.catheavenapp.domain.model.SlotType
import com.example.catheavenapp.domain.model.Timeslot
import com.example.catheavenapp.domain.model.TimeslotStatus
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Date

interface BookingRepository {
    /** All timeslots on [date], sorted by start time. */
    suspend fun timeslotsOn(date: VisitDate): List<Timeslot>

    /** Books the draft's timeslot atomically. Throws [BookingRejectedException] if it can't. */
    suspend fun createBooking(draft: BookingDraft, quote: PriceQuote?): Booking

    /** Every booking made by the signed-in supporter, in no particular order. */
    suspend fun myBookings(): List<Booking>
}

class BookingRejectedException(val rejection: BookingRejection) : Exception(rejection.message)

class NotSignedInException : Exception("Please sign in to book a visit.")

class FirestoreBookingRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : BookingRepository {

    override suspend fun timeslotsOn(date: VisitDate): List<Timeslot> {
        val dayStart = date.toEpochMillis()
        val dayEnd = dayStart + ONE_DAY_MILLIS
        // A range on one field needs no composite index; sorting happens on the phone.
        val snapshot = db.collection(TIMESLOTS)
            .whereGreaterThanOrEqualTo(FIELD_DATE, Timestamp(Date(dayStart)))
            .whereLessThan(FIELD_DATE, Timestamp(Date(dayEnd)))
            .get()
            .await()
        // startTime is zero-padded "HH:mm", so text order is time order.
        return snapshot.documents.mapNotNull { it.toTimeslot() }.sortedBy { it.startTime }
    }

    override suspend fun createBooking(draft: BookingDraft, quote: PriceQuote?): Booking {
        val user = auth.currentUser ?: throw NotSignedInException()
        val slotId = draft.timeslotId ?: throw BookingRejectedException(BookingRejection.NoTimeslot)

        val slotRef = db.collection(TIMESLOTS).document(slotId)
        val bookingRef = db.collection(BOOKINGS).document()   // new ID, nothing written yet
        val year = Calendar.getInstance().get(Calendar.YEAR)

        try {
            return db.runTransaction { tx ->
                // 1. Re-read the slot inside the transaction, so we see the latest bookedCount.
                val slot = tx.get(slotRef).toTimeslot()
                    ?: throw BookingRejectedException(BookingRejection.SlotClosed)

                // 2. Decide with the pure, unit-tested rules.
                when (val decision = BookingPlanner.decide(
                    slot, draft, quote, bookingRef.id, user.uid, System.currentTimeMillis(), year
                )) {
                    is BookingDecision.Rejected -> throw BookingRejectedException(decision.reason)

                    // 3. Write the booking and the new slot count together, or not at all.
                    //    lastBookingId lets the security rules check the two writes match.
                    is BookingDecision.Accepted -> {
                        tx.set(bookingRef, decision.booking.toFirestore())
                        tx.update(
                            slotRef, mapOf(
                                "bookedCount" to decision.updatedSlot.bookedCount,
                                "status" to decision.updatedSlot.status.value,
                                "lastBookingId" to bookingRef.id
                            )
                        )
                        decision.booking
                    }
                }
            }.await()
        } catch (e: Exception) {
            // Firestore may wrap our exception; unwrap it so the screen gets the real reason.
            throw (e as? BookingRejectedException) ?: (e.cause as? BookingRejectedException) ?: e
        }
    }

    override suspend fun myBookings(): List<Booking> {
        val user = auth.currentUser ?: throw NotSignedInException()
        // The supporterId filter is required: the rules only let supporters read their own
        // bookings, and Firestore rejects a query that could return anyone else's.
        // Sorting happens on the phone, so no composite index is needed.
        val snapshot = db.collection(BOOKINGS)
            .whereEqualTo("supporterId", user.uid)
            .get()
            .await()
        return snapshot.documents.mapNotNull { it.toBooking() }
    }

    private companion object {
        const val TIMESLOTS = "timeslots"
        const val BOOKINGS = "bookings"
        const val FIELD_DATE = "date"
        const val ONE_DAY_MILLIS = 24L * 60 * 60 * 1000
    }
}

/** Maps a timeslots document to the domain model, or null if it is missing or malformed. */
internal fun DocumentSnapshot.toTimeslot(): Timeslot? {
    if (!exists()) return null
    val date = getTimestamp("date") ?: return null
    val start = getString("startTime") ?: return null
    return Timeslot(
        id = id,
        dateMillis = date.toDate().time,
        startTime = start,
        endTime = getString("endTime").orEmpty(),
        slotType = SlotType.fromValue(getString("slotType")),
        capacity = getLong("capacity")?.toInt() ?: 0,
        bookedCount = getLong("bookedCount")?.toInt() ?: 0,
        status = TimeslotStatus.fromValue(getString("status"))
    )
}

/** Maps a bookings document to the domain model, or null if it is missing or malformed. */
internal fun DocumentSnapshot.toBooking(): Booking? {
    if (!exists()) return null
    val slotDate = getTimestamp("slotDate") ?: return null
    return Booking(
        id = id,
        bookingReference = getString("bookingReference").orEmpty(),
        supporterId = getString("supporterId").orEmpty(),
        supporterName = getString("supporterName").orEmpty(),
        timeslotId = getString("timeslotId").orEmpty(),
        slotDateMillis = slotDate.toDate().time,
        slotStart = getString("slotStart").orEmpty(),
        numberOfGuests = getLong("numberOfGuests")?.toInt() ?: 0,
        durationHours = getDouble("durationHours") ?: 1.0,
        pricePerPerson = getLong("pricePerPerson")?.toInt() ?: 0,
        totalDue = getLong("totalDue")?.toInt() ?: 0,
        specialNotes = getString("specialNotes").orEmpty(),
        status = BookingStatus.fromValue(getString("status")),
        createdAtMillis = getTimestamp("createdAt")?.toDate()?.time ?: 0L
    )
}

private fun Booking.toFirestore(): Map<String, Any> = mapOf(
    "bookingReference" to bookingReference,
    "supporterId" to supporterId,
    "supporterName" to supporterName,
    "timeslotId" to timeslotId,
    "slotDate" to Timestamp(Date(slotDateMillis)),
    "slotStart" to slotStart,
    "numberOfGuests" to numberOfGuests,
    "durationHours" to durationHours,
    "pricePerPerson" to pricePerPerson,
    "totalDue" to totalDue,
    "specialNotes" to specialNotes,
    "status" to status.value,
    "createdAt" to FieldValue.serverTimestamp()   // the rules check this equals request.time
)