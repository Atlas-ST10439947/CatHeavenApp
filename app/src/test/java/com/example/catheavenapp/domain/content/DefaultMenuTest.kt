package com.example.catheavenapp.domain.content

import com.example.catheavenapp.domain.model.MenuCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultMenuTest {

    @Test
    fun giftShopItems_areValidVisibleGiftShopItems() {
        val items = DefaultMenu.giftShopItems()
        assertEquals(10, items.size)
        assertTrue(items.all { it.category == MenuCategory.GIFT_SHOP && it.isAvailable })
        assertTrue(items.all { ContentValidator.checkMenuItem(it) == ContentCheck.Valid })
    }

    @Test
    fun giftShopItems_haveStableUniqueIds() {
        val ids = DefaultMenu.giftShopItems().map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertTrue("gift-arts-crafts" in ids)
    }

    @Test
    fun slug_keepsOnlyLettersAndDigits() {
        assertEquals("arts-crafts", DefaultMenu.slug("Arts & Crafts"))
        assertEquals("framed-photos", DefaultMenu.slug("  Framed Photos "))
    }
}
