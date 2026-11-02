package com.example.catheavenapp.data.menu

import com.example.catheavenapp.domain.model.MenuCategory
import com.example.catheavenapp.domain.model.MenuItem
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

interface MenuRepository {
    /** Every menu item, including hidden ones, sorted by name. Anyone may read the menu. */
    suspend fun allItems(): List<MenuItem>

    /** Creates the item when its id is blank, otherwise replaces it. Admin only. */
    suspend fun save(item: MenuItem): String

    /** Admin only. */
    suspend fun delete(id: String)
}

class FirestoreMenuRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : MenuRepository {

    override suspend fun allItems(): List<MenuItem> =
        db.collection(MENU_ITEMS).get().await()
            .documents
            .mapNotNull { it.toMenuItem() }
            .sortedBy { it.name.lowercase() }

    override suspend fun save(item: MenuItem): String {
        val collection = db.collection(MENU_ITEMS)
        val doc = if (item.id.isBlank()) collection.document() else collection.document(item.id)
        doc.set(item.toFirestore()).await()
        return doc.id
    }

    override suspend fun delete(id: String) {
        db.collection(MENU_ITEMS).document(id).delete().await()
    }

    private companion object {
        const val MENU_ITEMS = "menuItems"
    }
}

private fun DocumentSnapshot.toMenuItem(): MenuItem? {
    if (!exists()) return null
    val name = getString("name") ?: return null
    return MenuItem(
        id = id,
        name = name,
        category = MenuCategory.fromValue(getString("category")),
        price = getLong("price")?.toInt(),
        isAvailable = getBoolean("isAvailable") ?: true
    )
}

private fun MenuItem.toFirestore(): Map<String, Any?> = mapOf(
    "name" to name.trim(),
    "category" to category.value,
    "price" to price,
    "isAvailable" to isAvailable,
    "updatedAt" to FieldValue.serverTimestamp()
)
