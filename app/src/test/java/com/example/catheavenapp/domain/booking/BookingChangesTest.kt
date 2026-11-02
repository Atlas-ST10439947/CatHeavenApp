package com.example.catheavenapp.domain.booking

import com.example.catheavenapp.domain.model.Booking
import com.example.catheavenapp.domain.model.BookingStatus
import com.example.catheavenapp.domain.model.SlotType
import com.example.catheavenapp.domain.model.Timeslot
import com.example.catheavenapp.domain.model.TimeslotStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookingChangesTest {

    private val today = VisitDate(2026, 11, 2)
    private fun day(d: Int) = VisitDate(2026, 11, d).toEpochMillis()

    private fun booking(onDay: Int = 10, guests: Int = 2, status: BookingStatus = BookingStatus.CONFIRMED) = Booking(
        id = "b1", bookingReference = "CH-2026-B1", supporterId = "u1", supporterName = "Sam",
        timeslotId = "old", slotDateMillis = day(onDay), slotStart = "10:00", numberOfGuests = guests,
        durationHours = 1.0, pricePerPerson = 120, totalDue = 120 * guests, specialNotes = "",
        status = status, createdAtMillis = 0L
    )

    private fun slot(id: String, onDay: Int, capacity: Int = 10, booked: Int = 0, status: TimeslotStatus = TimeslotStatus.OPEN) =
        Timeslot(id, day(onDay), "14:00", "15:00", SlotType.GENERAL_VISIT, capacity, booked, status)

    private fun reasonOf(d: RescheduleDecision) = (d as? RescheduleDecision.Rejected)?.reason

    @Test
    fun canChange_untilTheDayBeforeTheVisit() {
        assertTrue(BookingChanges.canChange(booking(onDay = 3), today))
        assertFalse(BookingChanges.canChange(booking(onDay = 2), today))   // the visit is today
        assertFalse(BookingChanges.canChange(booking(onDay = 1), today))
    }

    @Test
    fun canChange_onlyConfirmedBookings() {
        assertFalse(BookingChanges.canChange(booking(status = BookingStatus.CANCELLED), today))
    }

    @Test
    fun reschedule_movesBookingAndBothSlotCounts() {
        val old = slot("old", 10, capacity = 10, booked = 10, status = TimeslotStatus.FULL)
        val new = slot("new", 12, capacity = 10, booked = 7)
        val decision = BookingChanges.reschedule(booking(guests = 2), old, new, today) as RescheduleDecision.Accepted

        assertEquals("new", decision.booking.timeslotId)
        assertEquals(day(12), decision.booking.slotDateMillis)
        assertEquals("14:00", decision.booking.slotStart)
        assertEquals(240, decision.booking.totalDue)                 // price unchanged
        assertEquals(8, decision.releasedSlot?.bookedCount)
        assertEquals(TimeslotStatus.OPEN, decision.releasedSlot?.status)
        assertEquals(9, decision.takenSlot.bookedCount)
    }

    @Test
    fun reschedule_fillsNewSlotExactlyToCapacity() {
        val decision = BookingChanges.reschedule(booking(guests = 3), null, slot("new", 12, 10, 7), today)
        assertEquals(TimeslotStatus.FULL, (decision as RescheduleDecision.Accepted).takenSlot.status)
    }

    @Test
    fun reschedule_rejectsSameSlot() {
        assertEquals(ChangeRejection.SameSlot, reasonOf(BookingChanges.reschedule(booking(), null, slot("old", 12), today)))
    }

    @Test
    fun reschedule_rejectsTodayOrEarlier() {
        assertEquals(ChangeRejection.NewDateTooSoon, reasonOf(BookingChanges.reschedule(booking(), null, slot("new", 2), today)))
    }

    @Test
    fun reschedule_rejectsClosedFullOrTooSmallSlots() {
        assertEquals(ChangeRejection.SlotUnavailable,
            reasonOf(BookingChanges.reschedule(booking(), null, slot("new", 12, status = TimeslotStatus.CLOSED), today)))
        assertEquals(ChangeRejection.SlotUnavailable,
            reasonOf(BookingChanges.reschedule(booking(), null, slot("new", 12, 10, 10, TimeslotStatus.FULL), today)))
        assertEquals(ChangeRejection.NotEnoughSpace(1),
            reasonOf(BookingChanges.reschedule(booking(guests = 2), null, slot("new", 12, 10, 9), today)))
    }

    @Test
    fun reschedule_rejectsBookingThatCanNoLongerChange() {
        assertEquals(ChangeRejection.NotChangeable,
            reasonOf(BookingChanges.reschedule(booking(onDay = 2), null, slot("new", 12), today)))
    }
}
