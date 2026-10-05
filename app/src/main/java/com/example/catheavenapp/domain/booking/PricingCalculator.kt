package com.example.catheavenapp.domain.booking

import com.example.catheavenapp.domain.model.PricingTier

/** The price for a chosen visit length and group size. */
data class PriceQuote(
    val durationHours: Double,
    val pricePerPerson: Int,   // ZAR
    val guests: Int
) {
    val total: Int get() = pricePerPerson * guests
}

object PricingCalculator {

    /** The active tier for [durationHours], or null if there is none. */
    fun tierFor(tiers: List<PricingTier>, durationHours: Double): PricingTier? =
        tiers.firstOrNull { it.isActive && it.durationHours == durationHours }

    fun quote(tiers: List<PricingTier>, durationHours: Double, guests: Int): PriceQuote? =
        tierFor(tiers, durationHours)?.let { PriceQuote(durationHours, it.pricePerPerson, guests) }
}

/**
 * The Task 1 pricing tiers. Used until the live tiers have loaded
 * from the Firestore pricingTiers collection.
 */
object DefaultPricing {
    val TIERS: List<PricingTier> = listOf(
        PricingTier(id = "tier-1h", durationHours = 1.0, pricePerPerson = 120, isActive = true),
        PricingTier(id = "tier-1.5h", durationHours = 1.5, pricePerPerson = 170, isActive = true),
        PricingTier(id = "tier-2h", durationHours = 2.0, pricePerPerson = 220, isActive = true)
    )
}

/** 1.0 -> "1", 1.5 -> "1.5", 2.0 -> "2". */
fun formatHours(hours: Double): String =
    if (hours % 1.0 == 0.0) hours.toInt().toString() else hours.toString()
