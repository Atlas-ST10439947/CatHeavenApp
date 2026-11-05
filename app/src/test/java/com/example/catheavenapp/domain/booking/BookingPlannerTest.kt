package com.example.catheavenapp.domain.booking

import com.example.catheavenapp.domain.model.BookingStatus
import com.example.catheavenapp.domain.model.SlotType
import com.example.catheavenapp.domain.model.Timeslot
import com.example.catheavenapp.domain.model.TimeslotStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class BookingPlannerTest {

    private fun slot(capacity: Int = 10, booked: Int = 0, status: TimeslotStatus = TimeslotStatus.OPEN) =
        Timeslot(
            id = "slot-1",
            dateMillis = 1_791_331_200_000L,
            startTime = "10:00",
            endTime = "11:00",
            slotType = SlotType.GENERAL_VISIT,
            capacity = capacity,
            bookedCount = booked,
            status = status
        )

    private fun draft(guests: Int = 2, hours: Double = 1.0) = BookingDraft(
        durationHours = hours,
        startTime = "10:00",
        guests = guests,
        fullName = "Wiehan",
        timeslotId = "slot-1"
    )

    private fun quote(guests: Int = 2, hours: Double = 1.0, price: Int = 120) =
        PriceQuote(durationHours = hours, pricePerPerson = price, guests = guests)

    private fun decide(slot: Timeslot, draft: BookingDraft, quote: PriceQuote?) =
        BookingPlanner.decide(
            slot, draft, quote,
            bookingId = "k3f9qzAbCdEf",
            supporterId = "user-1",
            nowMillis = 1L,
            year = 2026
        )

    @Test
    fun `open slot with space is accepted and the seats are taken`() {
        val result = decide(slot(), draft(), quote()) as BookingDecision.Accepted

        assertEquals(2, result.updatedSlot.bookedCount)
        assertEquals(TimeslotStatus.OPEN, result.updatedSlot.status)
        assertEquals("slot-1", result.booking.timeslotId)
        assertEquals("user-1", result.booking.supporterId)
        assertEquals(240, result.booking.totalDue)
        assertEquals(BookingStatus.CONFIRMED, result.booking.status)
    }

    @Test
    fun `taking the last seats marks the slot full`() {
        val result = decide(slot(capacity = 10, booked = 8), draft(guests = 2), quote(guests = 2))
            as BookingDecision.Accepted

        assertEquals(10, result.updatedSlot.bookedCount)
        assertEquals(TimeslotStatus.FULL, result.updatedSlot.status)
    }

    @Test
    fun `more guests than seats left is rejected`() {
        val result = decide(slot(booked = 9), draft(guests = 2), quote(guests = 2))

        assertEquals(BookingDecision.Rejected(BookingRejection.NotEnoughSpace(1)), result)
    }

    @Test
    fun `full slot is rejected`() {
        val result = decide(slot(booked = 10, status = TimeslotStatus.FULL), draft(), quote())

        assertEquals(BookingDecision.Rejected(BookingRejection.SlotFull), result)
    }

    @Test
    fun `closed slot is rejected even when it has space`() {
        val result = decide(slot(status = TimeslotStatus.CLOSED), draft(), quote())

        assertEquals(BookingDecision.Rejected(BookingRejection.SlotClosed), result)
    }

    @Test
    fun `missing price is rejected`() {
        val result = decide(slot(), draft(), null)

        assertEquals(BookingDecision.Rejected(BookingRejection.NoPrice), result)
    }

    @Test
    fun `quote for a different group size is rejected`() {
        val result = decide(slot(), draft(guests = 2), quote(guests = 3))

        assertEquals(BookingDecision.Rejected(BookingRejection.NoPrice), result)
    }

    @Test
    fun `more than the guest limit is rejected`() {
        val result = decide(slot(capacity = 20), draft(guests = 9), quote(guests = 9))

        assertEquals(BookingDecision.Rejected(BookingRejection.InvalidGuests), result)
    }

    @Test
    fun `booking reference uses the year and the start of the booking id`() {
        assertEquals("CH-2026-K3F9QZ", BookingPlanner.referenceFor("k3f9qzAbCdEf", 2026))
    }

    @Test
    fun `a time that has already passed today is rejected`() {
        val slot = slot()
        val result = BookingPlanner.decide(
            slot, draft(), quote(),
            bookingId = "k3f9qzAbCdEf", supporterId = "user-1",
            nowMillis = slot.startMillis() + 60_000, year = 2026
        )
        assertEquals(BookingDecision.Rejected(BookingRejection.SlotStarted), result)
    }

    @Test
    fun `a later time today can still be booked`() {
        val slot = slot()
        val result = BookingPlanner.decide(
            slot, draft(), quote(),
            bookingId = "k3f9qzAbCdEf", supporterId = "user-1",
            nowMillis = slot.startMillis() - 60_000, year = 2026
        )
        assertEquals(true, result is BookingDecision.Accepted)
    }
}
