package com.example.catheavenapp.domain.content

import com.example.catheavenapp.domain.model.MenuCategory
import com.example.catheavenapp.domain.model.MenuItem

/**
 * Builds the supporter-facing menu: hidden items are left out and each
 * category is sorted by name. Categories with nothing available map to an
 * empty list, so the screen can fall back to its default text.
 */
object MenuSections {

    fun from(items: List<MenuItem>): Map<MenuCategory, List<MenuItem>> {
        val available = items.filter { it.isAvailable && it.name.isNotBlank() }
        return MenuCategory.entries.associateWith { category ->
            available.filter { it.category == category }.sortedBy { it.name.lowercase() }
        }
    }

    /** "Coffee  ·  R30" when there's a price, otherwise just the name. */
    fun line(item: MenuItem): String =
        item.price?.let { "${item.name}  ·  R$it" } ?: item.name
}
