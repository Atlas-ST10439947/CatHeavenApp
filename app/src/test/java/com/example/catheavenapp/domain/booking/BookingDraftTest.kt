package com.example.catheavenapp.domain.booking

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

class BookingDraftTest {

    private val day1 = VisitDate(2026, 10, 10)
    private val day2 = VisitDate(2026, 10, 11)

    @Test
    fun newDraft_startsWithOneGuestAndOneHour() {
        val draft = BookingDraft()
        assertEquals(1, draft.guests)
        assertEquals(1.0, draft.durationHours, 0.0)
        assertFalse(draft.hasDate)
        assertFalse(draft.hasTime)
    }

    @Test
    fun changingTheDate_clearsTheChosenTime() {
        val draft = BookingDraft().withDate(day1).withStartTime("10:00").withDate(day2)
        assertEquals(day2, draft.date)
        assertEquals(null, draft.startTime)
        assertFalse(draft.hasTime)
    }

    @Test
    fun reselectingTheSameDate_keepsTheChosenTime() {
        val draft = BookingDraft().withDate(day1).withStartTime("10:00").withDate(day1)
        assertEquals("10:00", draft.startTime)
        assertTrue(draft.hasTime)
    }

    @Test
    fun addGuest_stopsAtEight() {
        var draft = BookingDraft()
        repeat(20) { draft = draft.addGuest() }
        assertEquals(8, draft.guests)
    }

    @Test
    fun removeGuest_neverGoesBelowOne() {
        val draft = BookingDraft().removeGuest().removeGuest()
        assertEquals(1, draft.guests)
    }

    @Test
    fun withContact_trimsWhitespace() {
        val draft = BookingDraft().withContact("  Jane  ", " jane@example.com ", " 0821234567 ")
        assertEquals("Jane", draft.fullName)
        assertEquals("jane@example.com", draft.email)
        assertEquals("0821234567", draft.phone)
    }

    @Test
    fun visitDate_roundTripsThroughEpochMillis() {
        val sast = TimeZone.getTimeZone("Africa/Johannesburg")
        val millis = day1.toEpochMillis(sast)
        assertEquals(day1, VisitDate.fromEpochMillis(millis, sast))
    }

    @Test
    fun visitDates_compareChronologically() {
        assertTrue(VisitDate(2026, 10, 10) < VisitDate(2026, 10, 11))
        assertTrue(VisitDate(2026, 9, 30) < VisitDate(2026, 10, 1))
        assertTrue(VisitDate(2025, 12, 31) < VisitDate(2026, 1, 1))
    }
}
