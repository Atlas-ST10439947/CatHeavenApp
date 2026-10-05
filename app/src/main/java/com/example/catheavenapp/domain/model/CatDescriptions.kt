package com.example.catheavenapp.domain.model

import java.util.Calendar
import java.util.TimeZone

/** Display text for a cat's profile, worked out from the stored data. */
object CatDescriptions {

    /**
     * "2 years old", "5 months old", "Under 1 month old", or null if the birth date is
     * missing or in the future. Worked out from dateOfBirth, so it stays correct over time.
     */
    fun age(
        dateOfBirthMillis: Long?,
        nowMillis: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): String? {
        if (dateOfBirthMillis == null || dateOfBirthMillis > nowMillis) return null
        val birth = Calendar.getInstance(timeZone).apply { timeInMillis = dateOfBirthMillis }
        val now = Calendar.getInstance(timeZone).apply { timeInMillis = nowMillis }

        var months = (now.get(Calendar.YEAR) - birth.get(Calendar.YEAR)) * 12 +
            (now.get(Calendar.MONTH) - birth.get(Calendar.MONTH))
        if (now.get(Calendar.DAY_OF_MONTH) < birth.get(Calendar.DAY_OF_MONTH)) months--

        return when {
            months < 1 -> "Under 1 month old"
            months < 12 -> if (months == 1) "1 month old" else "$months months old"
            else -> {
                val years = months / 12
                if (years == 1) "1 year old" else "$years years old"
            }
        }
    }

    fun statusLabel(status: AdoptionStatus): String = when (status) {
        AdoptionStatus.AVAILABLE -> "Available for adoption"
        AdoptionStatus.PENDING_ADOPTION -> "Adoption pending"
        AdoptionStatus.ADOPTED -> "Adopted"
        AdoptionStatus.RESIDENT -> "Permanent resident"
    }
}
