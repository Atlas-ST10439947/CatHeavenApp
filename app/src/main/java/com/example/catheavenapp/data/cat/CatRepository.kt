package com.example.catheavenapp.data.cat

import com.example.catheavenapp.domain.model.AdoptionStatus
import com.example.catheavenapp.domain.model.Cat
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.util.Date

interface CatRepository {
    /** Every cat in the catalogue, sorted by name. Anyone may read cats. */
    suspend fun allCats(): List<Cat>

    /** One cat by its document ID, or null if it doesn't exist. */
    suspend fun cat(id: String): Cat?

    /**
     * Creates the cat when its id is blank, otherwise updates it. Returns the
     * document ID. Admin only: the security rules reject anyone else.
     */
    suspend fun save(cat: Cat): String

    /** Admin only. */
    suspend fun delete(id: String)
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

    override suspend fun save(cat: Cat): String {
        val doc = if (cat.id.isBlank()) db.collection(CATS).document() else db.collection(CATS).document(cat.id)
        // merge() keeps any extra fields added in the console, e.g. by an earlier seed script
        doc.set(cat.toFirestore(), SetOptions.merge()).await()
        return doc.id
    }

    override suspend fun delete(id: String) {
        db.collection(CATS).document(id).delete().await()
    }

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

/** Every field the admin form edits. Missing dates are written as null so a cleared date stays cleared. */
private fun Cat.toFirestore(): Map<String, Any?> = mapOf(
    "name" to name.trim(),
    "breed" to breed.trim(),
    "gender" to gender.trim(),
    "personality" to personality.trim(),
    "biography" to biography.trim(),
    "adoptionStatus" to adoptionStatus.value,
    "photoUrl" to photoUrl,
    "dateOfBirth" to dateOfBirthMillis?.let { Timestamp(Date(it)) },
    "dateArrived" to dateArrivedMillis?.let { Timestamp(Date(it)) },
    "updatedAt" to FieldValue.serverTimestamp()
)
