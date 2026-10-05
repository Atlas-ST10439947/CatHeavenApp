package com.example.catheavenapp.domain.booking

import com.example.catheavenapp.domain.model.SlotType
import com.example.catheavenapp.domain.model.Timeslot
import com.example.catheavenapp.domain.model.TimeslotStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeslotTest {

    private fun slot(capacity: Int = 10, bookedCount: Int = 0, status: TimeslotStatus = TimeslotStatus.OPEN) =
        Timeslot("slot-1", 0L, "10:00", "11:00", SlotType.GENERAL_VISIT, capacity, bookedCount, status)

    @Test
    fun withBooking_increasesBookedCount_andStaysOpen() {
        val updated = slot(capacity = 10, bookedCount = 2).withBooking(3)
        assertEquals(5, updated.bookedCount)
        assertEquals(TimeslotStatus.OPEN, updated.status)
    }

    @Test
    fun withBooking_marksSlotFull_whenCapacityReached() {
        val updated = slot(capacity = 10, bookedCount = 7).withBooking(3)
        assertEquals(10, updated.bookedCount)
        assertEquals(TimeslotStatus.FULL, updated.status)
        assertFalse(updated.isBookable)
    }

    @Test(expected = IllegalArgumentException::class)
    fun withBooking_refusesToOverbook() {
        slot(capacity = 10, bookedCount = 8).withBooking(3)
    }

    @Test
    fun remainingCapacity_neverGoesNegative() {
        assertEquals(0, slot(capacity = 10, bookedCount = 12).remainingCapacity)
    }

    @Test
    fun openSlotWithSeats_isBookable() {
        assertTrue(slot(capacity = 10, bookedCount = 4).isBookable)
    }

    @Test
    fun unknownStatusValue_isTreatedAsClosed() {
        assertEquals(TimeslotStatus.CLOSED, TimeslotStatus.fromValue("something-else"))
        assertEquals(TimeslotStatus.CLOSED, TimeslotStatus.fromValue(null))
    }
}
