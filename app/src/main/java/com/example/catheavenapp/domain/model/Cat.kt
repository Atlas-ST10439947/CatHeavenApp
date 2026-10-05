package com.example.catheavenapp.domain.model

/**
 * A resident cat. Mirrors the Firestore `cats` collection.
 * dateOfBirth is stored rather than a fixed age, so displayed ages stay correct.
 */
data class Cat(
    val id: String,
    val name: String,
    val dateOfBirthMillis: Long?,
    val gender: String,
    val breed: String,
    val dateArrivedMillis: Long?,
    val personality: String,     // e.g. "playful", "shy", "cuddly"
    val biography: String,
    val adoptionStatus: AdoptionStatus,
    val photoUrl: String         // Cloud Storage download URL
)

enum class AdoptionStatus(val value: String) {
    AVAILABLE("available"),
    PENDING_ADOPTION("pendingAdoption"),
    ADOPTED("adopted"),
    RESIDENT("resident");

    companion object {
        fun fromValue(value: String?): AdoptionStatus =
            entries.firstOrNull { it.value == value } ?: RESIDENT
    }
}
