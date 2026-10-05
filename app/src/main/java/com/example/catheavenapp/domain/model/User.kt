package com.example.catheavenapp.domain.model

/**
 * A registered account. Mirrors the Firestore `users` collection,
 * where the document ID equals the Firebase Auth UID.
 *
 * Note: [role] is for display only. Admin rights are enforced by the
 * `admin` custom claim on the Firebase Auth token, which the security
 * rules check, so editing this field cannot grant admin access.
 */
data class User(
    val uid: String,
    val fullName: String,
    val email: String,
    val phone: String,
    val role: UserRole,
    val dateJoinedMillis: Long
)

enum class UserRole(val value: String) {
    SUPPORTER("supporter"),
    ADMIN("admin");

    companion object {
        fun fromValue(value: String?): UserRole =
            entries.firstOrNull { it.value == value } ?: SUPPORTER
    }
}
