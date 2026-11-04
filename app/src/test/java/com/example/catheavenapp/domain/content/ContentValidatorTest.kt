package com.example.catheavenapp.domain.content

import com.example.catheavenapp.domain.model.AdoptionStatus
import com.example.catheavenapp.domain.model.Cat
import com.example.catheavenapp.domain.model.MenuCategory
import com.example.catheavenapp.domain.model.MenuItem
import org.junit.Assert.assertEquals
import org.junit.Test

class ContentValidatorTest {

    private val now = 1_800_000_000_000L
    private val day = 24L * 60 * 60 * 1000

    private fun cat(
        name: String = "Amore",
        photo: String = "amore",
        dob: Long? = now - 400 * day,
        arrived: Long? = now - 100 * day,
        biography: String = "Loves sunny windowsills."
    ) = Cat(
        id = "", name = name, dateOfBirthMillis = dob, gender = "female", breed = "Tabby",
        dateArrivedMillis = arrived, personality = "cuddly", biography = biography,
        adoptionStatus = AdoptionStatus.RESIDENT, photoUrl = photo
    )

    private fun fieldOf(check: ContentCheck): ContentField? = (check as? ContentCheck.Invalid)?.field

    @Test
    fun checkCat_acceptsCompleteCat() {
        assertEquals(ContentCheck.Valid, ContentValidator.checkCat(cat(), now))
    }

    @Test
    fun checkCat_acceptsCatWithoutDatesOrPhoto() {
        assertEquals(ContentCheck.Valid, ContentValidator.checkCat(cat(photo = "", dob = null, arrived = null), now))
    }

    @Test
    fun checkCat_rejectsBlankName() {
        assertEquals(ContentField.NAME, fieldOf(ContentValidator.checkCat(cat(name = "  "), now)))
    }

    @Test
    fun checkCat_rejectsBiographyOverLimit() {
        val long = "a".repeat(ContentValidator.MAX_BIOGRAPHY + 1)
        assertEquals(ContentField.BIOGRAPHY, fieldOf(ContentValidator.checkCat(cat(biography = long), now)))
    }

    @Test
    fun checkCat_rejectsPhotoThatIsNotADrawableKey() {
        assertEquals(ContentField.PHOTO, fieldOf(ContentValidator.checkCat(cat(photo = "https://evil.example/x.png"), now)))
    }

    @Test
    fun checkCat_rejectsFutureBirthDate() {
        assertEquals(ContentField.DATE_OF_BIRTH, fieldOf(ContentValidator.checkCat(cat(dob = now + day), now)))
    }

    @Test
    fun checkCat_rejectsArrivalBeforeBirth() {
        val check = ContentValidator.checkCat(cat(dob = now - 10 * day, arrived = now - 20 * day), now)
        assertEquals(ContentField.DATE_ARRIVED, fieldOf(check))
    }

    @Test
    fun checkMenuItem_acceptsItemWithOrWithoutPrice() {
        assertEquals(ContentCheck.Valid, ContentValidator.checkMenuItem(menuItem(price = 30)))
        assertEquals(ContentCheck.Valid, ContentValidator.checkMenuItem(menuItem(price = null)))
    }

    @Test
    fun checkMenuItem_rejectsBlankName() {
        assertEquals(ContentField.NAME, fieldOf(ContentValidator.checkMenuItem(menuItem(name = ""))))
    }

    @Test
    fun checkMenuItem_rejectsNegativeOrHugePrice() {
        assertEquals(ContentField.PRICE, fieldOf(ContentValidator.checkMenuItem(menuItem(price = -1))))
        assertEquals(ContentField.PRICE, fieldOf(ContentValidator.checkMenuItem(menuItem(price = 999_999))))
    }

    private fun menuItem(name: String = "Coffee", price: Int? = 30) =
        MenuItem(id = "", name = name, category = MenuCategory.FOOD_AND_DRINK, price = price, isAvailable = true)
}
