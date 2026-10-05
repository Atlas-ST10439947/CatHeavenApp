package com.example.catheavenapp.data.cat

import com.example.catheavenapp.domain.model.AdoptionStatus
import com.example.catheavenapp.domain.model.Cat
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

interface CatRepository {
    /** Every cat in the catalogue, sorted by name. Anyone may read cats. */
    suspend fun allCats(): List<Cat>

    /** One cat by its document ID, or null if it doesn't exist. */
    suspend fun cat(id: String): Cat?
}

class FirestoreCatRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : CatRepository {

    override suspend fun allCats(): List<Cat> =
        db.collection(CATS).get().await()
            .documents
            .mapNotNull { it.toCat() }
            .sortedBy { it.name.lowercase() }

    override suspend fun cat(id: String): Cat? =
        db.collection(CATS).document(id).get().await().toCat()

    private companion object {
        const val CATS = "cats"
    }
}

/** Tolerant mapping: a missing or wrongly typed field becomes empty instead of crashing. */
private fun DocumentSnapshot.toCat(): Cat? {
    if (!exists()) return null
    return Cat(
        id = id,
        name = text("name").ifBlank { "Unnamed cat" },
        dateOfBirthMillis = millis("dateOfBirth"),
        gender = text("gender"),
        breed = text("breed"),
        dateArrivedMillis = millis("dateArrived"),
        personality = text("personality"),
        biography = text("biography"),
        adoptionStatus = AdoptionStatus.fromValue(get("adoptionStatus") as? String),
        photoUrl = text("photoUrl")
    )
}

private fun DocumentSnapshot.text(field: String): String = (get(field) as? String).orEmpty()

private fun DocumentSnapshot.millis(field: String): Long? = (get(field) as? Timestamp)?.toDate()?.time
