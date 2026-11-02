package com.example.catheavenapp.domain.booking

import com.example.catheavenapp.domain.model.SlotType
import com.example.catheavenapp.domain.model.Timeslot
import com.example.catheavenapp.domain.model.TimeslotStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class TimeslotCancellationTest {

    private fun slot(capacity: Int, booked: Int, status: TimeslotStatus) = Timeslot(
        id = "s1", dateMillis = 0L, startTime = "10:00", endTime = "11:00",
        slotType = SlotType.GENERAL_VISIT, capacity = capacity, bookedCount = booked, status = status
    )

    @Test
    fun withCancellation_releasesSeats() {
        val after = slot(10, 6, TimeslotStatus.OPEN).withCancellation(2)
        assertEquals(4, after.bookedCount)
        assertEquals(TimeslotStatus.OPEN, after.status)
    }

    @Test
    fun withCancellation_reopensFullSlot() {
        val after = slot(10, 10, TimeslotStatus.FULL).withCancellation(3)
        assertEquals(7, after.bookedCount)
        assertEquals(TimeslotStatus.OPEN, after.status)
    }

    @Test
    fun withCancellation_keepsClosedSlotClosed() {
        assertEquals(TimeslotStatus.CLOSED, slot(10, 4, TimeslotStatus.CLOSED).withCancellation(4).status)
    }

    @Test
    fun withCancellation_neverGoesBelowZero() {
        assertEquals(0, slot(10, 1, TimeslotStatus.OPEN).withCancellation(3).bookedCount)
    }

    @Test(expected = IllegalArgumentException::class)
    fun withCancellation_rejectsZeroGuests() {
        slot(10, 1, TimeslotStatus.OPEN).withCancellation(0)
    }
}
