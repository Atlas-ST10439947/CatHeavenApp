package com.example.catheavenapp.domain.content

import com.example.catheavenapp.domain.model.MenuCategory
import com.example.catheavenapp.domain.model.MenuItem

/** The gift shop range shown before any items were managed in the app (same list as seedMenu.js). */
object DefaultMenu {

    private val GIFT_SHOP = listOf(
        "Books", "Arts & Crafts", "Cat Toys", "Stationery", "Decor", "Jewelry",
        "Accessories", "Framed Photos", "Scarves", "Apparel"
    )

    /** Fixed IDs, so adding the defaults twice overwrites them instead of creating duplicates. */
    fun giftShopItems(): List<MenuItem> = GIFT_SHOP.map { name ->
        MenuItem(id = "gift-" + slug(name), name = name, category = MenuCategory.GIFT_SHOP, price = null, isAvailable = true)
    }

    /** "Arts & Crafts" -> "arts-crafts". */
    fun slug(text: String): String = text.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
}
