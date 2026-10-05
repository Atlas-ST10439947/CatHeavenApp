package com.example.catheavenapp.domain.model

/**
 * A visit length and its price per person. Mirrors the Firestore
 * `pricingTiers` collection (e.g. 1 hour at R120 per person).
 * Tiers are maintained by administrators and can be switched off with isActive.
 */
data class PricingTier(
    val id: String,
    val durationHours: Double,   // 1, 1.5 or 2
    val pricePerPerson: Int,     // ZAR
    val isActive: Boolean
)
