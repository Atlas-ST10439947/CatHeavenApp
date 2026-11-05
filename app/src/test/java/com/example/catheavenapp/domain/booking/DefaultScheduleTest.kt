package com.example.catheavenapp.domain.booking

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultScheduleTest {

    @Test
    fun openingDays_skipsSundayAndMonday() {
        // Mon 2 Nov 2026 for one week: Tue 3 to Sat 7 are open; Sun 8 is closed
        val days = DefaultSchedule.openingDays(VisitDate(2026, 11, 2), 7)
        assertEquals((3..7).map { VisitDate(2026, 11, it) }, days)
    }

    @Test
    fun openingDays_crossesMonthEnds() {
        // Sat 28 Nov 2026 + 4 days: Sat 28, (Sun, Mon closed), Tue 1 Dec
        val days = DefaultSchedule.openingDays(VisitDate(2026, 11, 28), 4)
        assertEquals(listOf(VisitDate(2026, 11, 28), VisitDate(2026, 12, 1)), days)
    }

    @Test
    fun openingDays_fourWeeksGivesTwentyDays() {
        assertEquals(20, DefaultSchedule.openingDays(VisitDate(2026, 11, 2), 28).size)
    }

    @Test
    fun isOpeningDay_matchesTheOpeningHours() {
        assertTrue(DefaultSchedule.isOpeningDay(VisitDate(2026, 11, 7)))    // Saturday
        assertFalse(DefaultSchedule.isOpeningDay(VisitDate(2026, 11, 8)))   // Sunday
        assertFalse(DefaultSchedule.isOpeningDay(VisitDate(2026, 11, 9)))   // Monday
    }
}
