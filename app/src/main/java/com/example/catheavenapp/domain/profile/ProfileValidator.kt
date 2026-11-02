package com.example.catheavenapp.domain.profile

enum class ProfileField { NAME, PHONE, LOCATION }

sealed interface ProfileCheck {
    data class Valid(val details: ProfileDetails) : ProfileCheck
    data class Invalid(val field: ProfileField, val message: String) : ProfileCheck
}

/**
 * Checks profile edits before they are saved, so the supporter gets instant
 * feedback. The Firestore security rules apply the same limits again on the
 * server (see validProfileChange in firestore.rules), because app-side checks
 * alone can be bypassed. Kept free of Android and Firebase so it can be unit tested.
 */
object ProfileValidator {

    const val MAX_NAME_LENGTH = 80
    const val MAX_LOCATION_LENGTH = 100

    /** Optional leading +, then 7 to 20 digits or spaces. Must match the rule in firestore.rules. */
    private val PHONE_PATTERN = Regex("^[+]?[0-9 ]{7,20}$")
    private val WHITESPACE = Regex("\\s+")

    fun check(fullName: String, phone: String, location: String): ProfileCheck {
        val name = fullName.trim().replace(WHITESPACE, " ")
        val cleanPhone = phone.trim().replace(WHITESPACE, " ")
        val cleanLocation = location.trim().replace(WHITESPACE, " ")

        if (name.isEmpty()) {
            return ProfileCheck.Invalid(ProfileField.NAME, "Please enter your name.")
        }
        if (name.length > MAX_NAME_LENGTH) {
            return ProfileCheck.Invalid(ProfileField.NAME, "Names can be up to $MAX_NAME_LENGTH characters.")
        }
        if (cleanPhone.isNotEmpty() && !PHONE_PATTERN.matches(cleanPhone)) {
            return ProfileCheck.Invalid(ProfileField.PHONE, "Enter a phone number using digits only, e.g. 082 123 4567.")
        }
        if (cleanLocation.length > MAX_LOCATION_LENGTH) {
            return ProfileCheck.Invalid(
                ProfileField.LOCATION, "Locations can be up to $MAX_LOCATION_LENGTH characters."
            )
        }
        return ProfileCheck.Valid(ProfileDetails(name, cleanPhone, cleanLocation))
    }
}
