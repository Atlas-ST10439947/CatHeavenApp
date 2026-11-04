package com.example.catheavenapp.domain.profile

/**
 * The signed-in supporter's own account details, as shown on the Profile and
 * Notifications settings screens. Mirrors fields on their `users/{uid}` document.
 */
data class Profile(
    val fullName: String,
    val email: String,
    val phone: String,
    val location: String,
    val notifications: NotificationPrefs
)

/**
 * What the supporter wants to hear about. Both default to on, because that is
 * what the switches showed before the preference was ever saved.
 */
data class NotificationPrefs(
    val email: Boolean = true,
    val inApp: Boolean = true
)

/** The editable profile fields, already cleaned up by [ProfileValidator]. */
data class ProfileDetails(
    val fullName: String,
    val phone: String,
    val location: String
)
