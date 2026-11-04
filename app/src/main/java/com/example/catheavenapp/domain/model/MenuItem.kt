package com.example.catheavenapp.domain.model

/**
 * Something sold at the café or gift shop. Mirrors the Firestore `menuItems`
 * collection, which administrators manage from the admin Content screen.
 */
data class MenuItem(
    val id: String,
    val name: String,
    val category: MenuCategory,
    val price: Int?,            // ZAR; null when the price isn't listed in the app
    val isAvailable: Boolean    // hidden from supporters when false
)

enum class MenuCategory(val value: String, val label: String) {
    FOOD_AND_DRINK("foodAndDrink", "Beverages & Food"),
    GIFT_SHOP("giftShop", "Gift Shop");

    companion object {
        fun fromValue(value: String?): MenuCategory =
            entries.firstOrNull { it.value == value } ?: FOOD_AND_DRINK
    }
}
