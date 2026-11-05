package com.example.catheavenapp.domain.profile

/**
 * The signed-in supporter's own account details, as shown on the Personal Profile
 * screen. Mirrors fields on their `users/{uid}` document.
 */
data class Profile(
    val fullName: String,
    val email: String,
    val phone: String,
    val location: String
)

/** The editable profile fields, already cleaned up by [ProfileValidator]. */
data class ProfileDetails(
    val fullName: String,
    val phone: String,
    val location: String
)
