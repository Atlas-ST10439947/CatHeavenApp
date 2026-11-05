package com.example.catheavenapp.data.profile

import com.example.catheavenapp.data.booking.NotSignedInException
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

    private fun myDocument(): DocumentReference {
        val user = auth.currentUser ?: throw NotSignedInException()
        return db.collection(USERS).document(user.uid)
    }

    private companion object {
        const val USERS = "users"
        const val FIELD_NAME = "fullName"
        const val FIELD_PHONE = "phone"
        const val FIELD_LOCATION = "location"
    }
}

/** Tolerant mapping: accounts created before a field existed still load. */
private fun DocumentSnapshot.toProfile(fallbackEmail: String): Profile {
    return Profile(
        fullName = getString("fullName").orEmpty(),
        email = getString("email").orEmpty().ifBlank { fallbackEmail },
        phone = getString("phone").orEmpty(),
        location = getString("location").orEmpty()
    )
}
