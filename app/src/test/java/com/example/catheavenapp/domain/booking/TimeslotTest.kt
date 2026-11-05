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

    // ---- Start time (so earlier times today can't be booked) ----

    private val utc = java.util.TimeZone.getTimeZone("UTC")
    private val midnight5Nov2026Utc = 1_793_836_800_000L   // Thu 5 Nov 2026, 00:00 UTC
    private fun slotAt(start: String) =
        Timeslot("slot-t", midnight5Nov2026Utc, start, "11:00", SlotType.GENERAL_VISIT, 10, 0, TimeslotStatus.OPEN)

    @Test
    fun startMillis_isTheDayPlusTheStartTime() {
        val expected = midnight5Nov2026Utc + (10 * 60 + 30) * 60_000L
        assertEquals(expected, slotAt("10:30").startMillis(utc))
    }

    @Test
    fun hasStarted_onlyOnceTheStartTimeIsReached() {
        val start = slotAt("10:00").startMillis(utc)
        assertFalse(slotAt("10:00").hasStarted(start - 60_000, utc))
        assertTrue(slotAt("10:00").hasStarted(start, utc))
        assertTrue(slotAt("10:00").hasStarted(start + 7 * 3_600_000L, utc))   // 17:00 the same day
    }

    @Test
    fun unreadableStartTime_usesTheStartOfTheDay() {
        assertEquals(midnight5Nov2026Utc, slotAt("soon").startMillis(utc))
    }
}
