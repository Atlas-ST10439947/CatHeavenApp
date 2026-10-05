package com.example.catheavenapp.domain.booking

import com.example.catheavenapp.domain.model.SlotType
import com.example.catheavenapp.domain.model.Timeslot
import com.example.catheavenapp.domain.model.TimeslotStatus
import java.util.Locale

/** One change to the timeslots collection. */
sealed interface SlotWrite {
    /** A slot that doesn't exist yet for this day. */
    data class Create(val slot: Timeslot) : SlotWrite

    /** An existing slot: only capacity and status change, so bookedCount is never touched. */
    data class Update(val id: String, val capacity: Int, val status: TimeslotStatus) : SlotWrite
}

sealed interface DayPlan {
    data class Ready(val writes: List<SlotWrite>) : DayPlan
    data class Invalid(val message: String) : DayPlan
}

/**
 * Turns the admin's "capacity + open/closed" choice for one day into timeslot writes.
 * Kept free of Firebase so it can be unit tested.
 */
object DaySchedule {

    /** The café's standard hourly visit slots (the same times the booking screen used to hard-code). */
    val STANDARD_START_TIMES = listOf("10:00", "11:00", "12:00", "13:00", "14:00", "15:00")

    const val MAX_CAPACITY = 50

    /** e.g. "slot-2026-10-05-10". Predictable, so saving the same day twice never creates duplicates. */
    fun slotId(date: VisitDate, startTime: String): String =
        String.format(
            Locale.US, "slot-%04d-%02d-%02d-%s",
            date.year, date.month, date.day, startTime.substringBefore(":")
        )

    /** "10:00" -> "11:00". */
    fun endTimeFor(startTime: String): String {
        val hour = startTime.substringBefore(":").toInt()
        return String.format(Locale.US, "%02d:%s", hour + 1, startTime.substringAfter(":"))
    }

    fun statusFor(bookedCount: Int, capacity: Int, open: Boolean): TimeslotStatus = when {
        !open -> TimeslotStatus.CLOSED
        bookedCount >= capacity -> TimeslotStatus.FULL
        else -> TimeslotStatus.OPEN
    }

    fun plan(date: VisitDate, existing: List<Timeslot>, capacity: Int, open: Boolean): DayPlan {
        if (capacity !in 1..MAX_CAPACITY) {
            return DayPlan.Invalid("Capacity must be between 1 and $MAX_CAPACITY guests.")
        }

        // Never drop capacity below guests who have already booked
        val overbooked = existing.filter { it.bookedCount > capacity }.minByOrNull { it.startTime }
        if (overbooked != null) {
            return DayPlan.Invalid(
                "The ${overbooked.startTime} slot already has ${overbooked.bookedCount} guests booked, " +
                    "so capacity can't be lower than that."
            )
        }

        val existingByStart = existing.associateBy { it.startTime }

        val standardSlots = STANDARD_START_TIMES.map { start ->
            val slot = existingByStart[start]
            if (slot != null) {
                SlotWrite.Update(slot.id, capacity, statusFor(slot.bookedCount, capacity, open))
            } else {
                SlotWrite.Create(
                    Timeslot(
                        id = slotId(date, start),
                        dateMillis = date.toEpochMillis(),
                        startTime = start,
                        endTime = endTimeFor(start),
                        slotType = SlotType.GENERAL_VISIT,
                        capacity = capacity,
                        bookedCount = 0,
                        status = statusFor(0, capacity, open)
                    )
                )
            }
        }

        // Any extra slots added by hand (e.g. a special event) follow the same day settings
        val extraSlots = existing
            .filter { it.startTime !in STANDARD_START_TIMES }
            .map { SlotWrite.Update(it.id, capacity, statusFor(it.bookedCount, capacity, open)) }

        return DayPlan.Ready(standardSlots + extraSlots)
    }
}
