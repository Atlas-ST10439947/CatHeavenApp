package com.example.catheavenapp.domain.booking

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BookingDraftTimeslotTest {

    @Test
    fun `choosing a slot stores its id and start time`() {
        val draft = BookingDraft(date = VisitDate(2026, 10, 7)).withTimeslot("slot-1", "10:00")

        assertEquals("slot-1", draft.timeslotId)
        assertEquals("10:00", draft.startTime)
        assertTrue(draft.hasTimeslot)
    }

    @Test
    fun `changing the date clears the chosen slot`() {
        val draft = BookingDraft(date = VisitDate(2026, 10, 7))
            .withTimeslot("slot-1", "10:00")
            .withDate(VisitDate(2026, 10, 8))

        assertNull(draft.timeslotId)
        assertNull(draft.startTime)
        assertFalse(draft.hasTimeslot)
    }
}
