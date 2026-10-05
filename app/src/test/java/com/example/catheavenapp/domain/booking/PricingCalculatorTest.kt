package com.example.catheavenapp.domain.booking

import org.junit.Assert.assertEquals
import org.junit.Test

class PricingCalculatorTest {

    private val tiers = DefaultPricing.TIERS

    @Test
    fun quote_multipliesPriceByGuests() {
        val quote = PricingCalculator.quote(tiers, durationHours = 1.5, guests = 3)
        assertEquals(170, quote?.pricePerPerson)
        assertEquals(510, quote?.total)
    }

    @Test
    fun quote_isNullForUnknownDuration() {
        assertEquals(null, PricingCalculator.quote(tiers, durationHours = 3.0, guests = 2))
    }

    @Test
    fun quote_ignoresInactiveTiers() {
        val tiersWith1hOff = tiers.map { if (it.durationHours == 1.0) it.copy(isActive = false) else it }
        assertEquals(null, PricingCalculator.quote(tiersWith1hOff, durationHours = 1.0, guests = 1))
    }

    @Test
    fun formatHours_dropsTrailingZero() {
        assertEquals("1", formatHours(1.0))
        assertEquals("1.5", formatHours(1.5))
        assertEquals("2", formatHours(2.0))
    }
}
