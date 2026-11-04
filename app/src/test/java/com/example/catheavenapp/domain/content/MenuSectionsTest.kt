package com.example.catheavenapp.domain.content

import com.example.catheavenapp.domain.model.MenuCategory
import com.example.catheavenapp.domain.model.MenuItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MenuSectionsTest {

    private fun item(name: String, category: MenuCategory, available: Boolean = true, price: Int? = null) =
        MenuItem(id = name, name = name, category = category, price = price, isAvailable = available)

    @Test
    fun from_groupsByCategoryAndSortsByName() {
        val sections = MenuSections.from(
            listOf(
                item("Tea", MenuCategory.FOOD_AND_DRINK),
                item("cat toys", MenuCategory.GIFT_SHOP),
                item("Coffee", MenuCategory.FOOD_AND_DRINK)
            )
        )
        assertEquals(listOf("Coffee", "Tea"), sections.getValue(MenuCategory.FOOD_AND_DRINK).map { it.name })
        assertEquals(listOf("cat toys"), sections.getValue(MenuCategory.GIFT_SHOP).map { it.name })
    }

    @Test
    fun from_leavesOutHiddenItems() {
        val sections = MenuSections.from(listOf(item("Scones", MenuCategory.FOOD_AND_DRINK, available = false)))
        assertTrue(sections.getValue(MenuCategory.FOOD_AND_DRINK).isEmpty())
    }

    @Test
    fun from_alwaysHasEveryCategory() {
        assertEquals(MenuCategory.entries.toSet(), MenuSections.from(emptyList()).keys)
    }

    @Test
    fun line_showsPriceOnlyWhenSet() {
        assertEquals("Coffee  ·  R30", MenuSections.line(item("Coffee", MenuCategory.FOOD_AND_DRINK, price = 30)))
        assertEquals("Books", MenuSections.line(item("Books", MenuCategory.GIFT_SHOP)))
    }
}
