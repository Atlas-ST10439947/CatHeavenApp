package com.example.catheavenapp.domain.booking

import com.example.catheavenapp.domain.model.AdoptionStatus
import com.example.catheavenapp.domain.model.CatDescriptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CatDescriptionsTest {

    private val now = VisitDate(2026, 10, 5).toEpochMillis()

    private fun ageOn(year: Int, month: Int, day: Int) =
        CatDescriptions.age(VisitDate(year, month, day).toEpochMillis(), now)

    @Test
    fun `age is given in whole years once a cat is over one`() {
        assertEquals("2 years old", ageOn(2024, 3, 10))
    }

    @Test
    fun `exactly one year uses the singular`() {
        assertEquals("1 year old", ageOn(2025, 10, 1))
    }

    @Test
    fun `age is given in months under a year, counting only full months`() {
        assertEquals("5 months old", ageOn(2026, 4, 20))
    }

    @Test
    fun `a kitten younger than a full month says so`() {
        assertEquals("Under 1 month old", ageOn(2026, 9, 20))
    }

    @Test
    fun `missing or future birth dates give no age`() {
        assertNull(CatDescriptions.age(null, now))
        assertNull(ageOn(2027, 1, 1))
    }

    @Test
    fun `status labels are written for supporters`() {
        assertEquals("Adoption pending", CatDescriptions.statusLabel(AdoptionStatus.PENDING_ADOPTION))
    }
}
