package com.example.catheavenapp.data.profile

import com.example.catheavenapp.data.booking.NotSignedInException
import com.example.catheavenapp.domain.profile.NotificationPrefs
import com.example.catheavenapp.domain.profile.Profile
import com.example.catheavenapp.domain.profile.ProfileDetails
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

interface ProfileRepository {
    /** The signed-in supporter's own profile. */
    suspend fun myProfile(): Profile

    /** Saves name, phone and location. Email and role are never written from here. */
    suspend fun updateDetails(details: ProfileDetails)

    suspend fun updateNotifications(prefs: NotificationPrefs)
}

class FirestoreProfileRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ProfileRepository {

    override suspend fun myProfile(): Profile {
        val user = auth.currentUser ?: throw NotSignedInException()
        val doc = myDocument().get().await()
        return doc.toProfile(fallbackEmail = user.email.orEmpty())
    }

    override suspend fun updateDetails(details: ProfileDetails) {
        // update() only touches these three fields; the rules reject any other key.
        myDocument().update(
            mapOf(
                FIELD_NAME to details.fullName,
                FIELD_PHONE to details.phone,
                FIELD_LOCATION to details.location
            )
        ).await()
    }

    override suspend fun updateNotifications(prefs: NotificationPrefs) {
        myDocument().update(
            FIELD_NOTIFICATIONS, mapOf("email" to prefs.email, "inApp" to prefs.inApp)
        ).await()
    }

    private fun myDocument(): DocumentReference {
        val user = auth.currentUser ?: throw NotSignedInException()
        return db.collection(USERS).document(user.uid)
    }

    private companion object {
        const val USERS = "users"
        const val FIELD_NAME = "fullName"
        const val FIELD_PHONE = "phone"
        const val FIELD_LOCATION = "location"
        const val FIELD_NOTIFICATIONS = "notificationPrefs"
    }
}

/** Tolerant mapping: accounts created before a field existed still load. */
private fun DocumentSnapshot.toProfile(fallbackEmail: String): Profile {
    val prefs = get("notificationPrefs") as? Map<*, *>
    return Profile(
        fullName = getString("fullName").orEmpty(),
        email = getString("email").orEmpty().ifBlank { fallbackEmail },
        phone = getString("phone").orEmpty(),
        location = getString("location").orEmpty(),
        notifications = NotificationPrefs(
            email = prefs?.get("email") as? Boolean ?: true,
            inApp = prefs?.get("inApp") as? Boolean ?: true
        )
    )
}
