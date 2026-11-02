package com.example.catheavenapp.domain.content

import com.example.catheavenapp.domain.model.Cat
import com.example.catheavenapp.domain.model.MenuItem

/** The form fields an admin can get wrong, so the screen can show the error in the right place. */
enum class ContentField { NAME, BREED, GENDER, PERSONALITY, BIOGRAPHY, PHOTO, DATE_OF_BIRTH, DATE_ARRIVED, PRICE }

sealed interface ContentCheck {
    object Valid : ContentCheck
    data class Invalid(val field: ContentField, val message: String) : ContentCheck
}

/**
 * Checks cats and menu items before an admin saves them. The Firestore rules
 * (validCat / validMenuItem in firestore.rules) enforce the same limits on the
 * server, so bad data is rejected even if it doesn't come from this app.
 * Kept free of Android and Firebase so it can be unit tested.
 */
object ContentValidator {

    const val MAX_CAT_NAME = 40
    const val MAX_BREED = 40
    const val MAX_GENDER = 20
    const val MAX_PERSONALITY = 60
    const val MAX_BIOGRAPHY = 1000
    const val MAX_MENU_NAME = 60
    const val MAX_PRICE = 5000

    /** Photo keys are drawable names such as "amore": lowercase letters, digits and underscores. */
    private val PHOTO_KEY = Regex("^[a-z0-9_]{0,60}$")

    fun checkCat(cat: Cat, nowMillis: Long): ContentCheck {
        val dob = cat.dateOfBirthMillis
        val arrived = cat.dateArrivedMillis
        return when {
            cat.name.isBlank() -> invalid(ContentField.NAME, "Please enter the cat's name.")
            cat.name.length > MAX_CAT_NAME -> invalid(ContentField.NAME, "Names can be up to $MAX_CAT_NAME characters.")
            cat.breed.length > MAX_BREED -> invalid(ContentField.BREED, "Breed can be up to $MAX_BREED characters.")
            cat.gender.length > MAX_GENDER -> invalid(ContentField.GENDER, "Gender can be up to $MAX_GENDER characters.")
            cat.personality.length > MAX_PERSONALITY ->
                invalid(ContentField.PERSONALITY, "Personality can be up to $MAX_PERSONALITY characters.")
            cat.biography.length > MAX_BIOGRAPHY ->
                invalid(ContentField.BIOGRAPHY, "Biographies can be up to $MAX_BIOGRAPHY characters.")
            !PHOTO_KEY.matches(cat.photoUrl) -> invalid(ContentField.PHOTO, "Choose one of the listed photos.")
            dob != null && dob > nowMillis -> invalid(ContentField.DATE_OF_BIRTH, "Date of birth can't be in the future.")
            arrived != null && arrived > nowMillis ->
                invalid(ContentField.DATE_ARRIVED, "Arrival date can't be in the future.")
            dob != null && arrived != null && arrived < dob ->
                invalid(ContentField.DATE_ARRIVED, "A cat can't arrive before it was born.")
            else -> ContentCheck.Valid
        }
    }

    fun checkMenuItem(item: MenuItem): ContentCheck {
        val price = item.price
        return when {
            item.name.isBlank() -> invalid(ContentField.NAME, "Please enter a name.")
            item.name.length > MAX_MENU_NAME -> invalid(ContentField.NAME, "Names can be up to $MAX_MENU_NAME characters.")
            price != null && price !in 0..MAX_PRICE -> invalid(ContentField.PRICE, "Prices must be between R0 and R$MAX_PRICE.")
            else -> ContentCheck.Valid
        }
    }

    private fun invalid(field: ContentField, message: String) = ContentCheck.Invalid(field, message)
}
