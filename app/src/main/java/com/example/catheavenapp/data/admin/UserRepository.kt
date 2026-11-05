package com.example.catheavenapp.data.admin

import com.example.catheavenapp.domain.model.User
import com.example.catheavenapp.domain.model.UserRole
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

interface UserRepository {
    /** Every registered account, sorted by name. Only admins are allowed to read this. */
    suspend fun allUsers(): List<User>

    /**
     * Blocks or unblocks a supporter. Only the `blocked` field changes: the profile and
     * bookings stay. The security rules refuse this for admin accounts.
     */
    suspend fun setBlocked(uid: String, blocked: Boolean)
}

class FirestoreUserRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : UserRepository {

    override suspend fun allUsers(): List<User> =
        db.collection("users").get().await()
            .documents
            .mapNotNull { it.toUser() }
            .sortedBy { it.fullName.lowercase() }

    override suspend fun setBlocked(uid: String, blocked: Boolean) {
        db.collection("users").document(uid).update("blocked", blocked).await()
    }
}

private fun DocumentSnapshot.toUser(): User? {
    if (!exists()) return null
    return User(
        uid = id,
        fullName = (get("fullName") as? String).orEmpty(),
        email = (get("email") as? String).orEmpty(),
        phone = (get("phone") as? String).orEmpty(),
        role = UserRole.fromValue(get("role") as? String),
        dateJoinedMillis = (get("dateJoined") as? Timestamp)?.toDate()?.time ?: 0L,
        blocked = get("blocked") as? Boolean ?: false
    )
}
