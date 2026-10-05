package com.example.catheavenapp.domain.booking

import com.example.catheavenapp.domain.model.SlotType
import com.example.catheavenapp.domain.model.Timeslot
import com.example.catheavenapp.domain.model.TimeslotStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DayScheduleTest {

    private val day = VisitDate(2026, 10, 5)

    private fun existing(start: String, booked: Int = 0, capacity: Int = 8, id: String = "existing-$start") =
        Timeslot(
            id = id,
            dateMillis = day.toEpochMillis(),
            startTime = start,
            endTime = DaySchedule.endTimeFor(start),
            slotType = SlotType.GENERAL_VISIT,
            capacity = capacity,
            bookedCount = booked,
            status = TimeslotStatus.OPEN
        )

    private fun writesOf(plan: DayPlan) = (plan as DayPlan.Ready).writes

    @Test
    fun `empty day creates the six standard slots, open, at the chosen capacity`() {
        val writes = writesOf(DaySchedule.plan(day, emptyList(), capacity = 8, open = true))

        assertEquals(6, writes.size)
        val slots = writes.map { (it as SlotWrite.Create).slot }
        assertEquals(DaySchedule.STANDARD_START_TIMES, slots.map { it.startTime })
        assertTrue(slots.all { it.capacity == 8 && it.bookedCount == 0 && it.status == TimeslotStatus.OPEN })
    }

    @Test
    fun `closing an empty day creates closed slots`() {
        val writes = writesOf(DaySchedule.plan(day, emptyList(), capacity = 8, open = false))

        assertTrue(writes.all { (it as SlotWrite.Create).slot.status == TimeslotStatus.CLOSED })
    }

    @Test
    fun `an existing slot is updated in place, not created again`() {
        val writes = writesOf(DaySchedule.plan(day, listOf(existing("10:00", id = "my-slot")), 10, true))

        assertEquals(SlotWrite.Update("my-slot", 10, TimeslotStatus.OPEN), writes.first())
        assertEquals(5, writes.count { it is SlotWrite.Create })
    }

    @Test
    fun `an existing slot that is already at the new capacity becomes full`() {
        val writes = writesOf(DaySchedule.plan(day, listOf(existing("10:00", booked = 8)), 8, true))

        assertEquals(TimeslotStatus.FULL, (writes.first() as SlotWrite.Update).status)
    }

    @Test
    fun `capacity below guests already booked is rejected`() {
        val plan = DaySchedule.plan(day, listOf(existing("14:00", booked = 6)), capacity = 4, open = true)

        assertTrue(plan is DayPlan.Invalid)
    }

    @Test
    fun `capacity of zero is rejected`() {
        assertTrue(DaySchedule.plan(day, emptyList(), capacity = 0, open = true) is DayPlan.Invalid)
    }

    @Test
    fun `slot ids and end times follow the pattern`() {
        assertEquals("slot-2026-10-05-10", DaySchedule.slotId(day, "10:00"))
        assertEquals("16:00", DaySchedule.endTimeFor("15:00"))
    }

    @Test
    fun `an extra hand-made slot follows the day settings too`() {
        val writes = writesOf(DaySchedule.plan(day, listOf(existing("17:30", id = "event")), 12, false))

        assertEquals(SlotWrite.Update("event", 12, TimeslotStatus.CLOSED), writes.last())
        assertEquals(7, writes.size)
    }
}
