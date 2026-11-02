package com.example.catheavenapp.domain.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileValidatorTest {

    private fun invalidField(name: String, phone: String = "", location: String = ""): ProfileField? =
        (ProfileValidator.check(name, phone, location) as? ProfileCheck.Invalid)?.field

    @Test
    fun check_trimsAndAcceptsValidDetails() {
        val check = ProfileValidator.check("  Jane   Smith ", " 082 123 4567 ", " Somerset West ")
        assertTrue(check is ProfileCheck.Valid)
        assertEquals(
            ProfileDetails("Jane Smith", "082 123 4567", "Somerset West"),
            (check as ProfileCheck.Valid).details
        )
    }

    @Test
    fun check_rejectsBlankName() {
        assertEquals(ProfileField.NAME, invalidField("   "))
    }

    @Test
    fun check_rejectsNameOverLimit() {
        assertEquals(ProfileField.NAME, invalidField("a".repeat(ProfileValidator.MAX_NAME_LENGTH + 1)))
    }

    @Test
    fun check_allowsEmptyPhone() {
        assertTrue(ProfileValidator.check("Jane", "", "") is ProfileCheck.Valid)
    }

    @Test
    fun check_acceptsInternationalPhoneWithPlus() {
        assertTrue(ProfileValidator.check("Jane", "+27 82 123 4567", "") is ProfileCheck.Valid)
    }

    @Test
    fun check_rejectsPhoneWithLettersOrBrackets() {
        assertEquals(ProfileField.PHONE, invalidField("Jane", phone = "082-CALL-ME"))
        assertEquals(ProfileField.PHONE, invalidField("Jane", phone = "(082) 123 4567"))
    }

    @Test
    fun check_rejectsTooShortPhone() {
        assertEquals(ProfileField.PHONE, invalidField("Jane", phone = "12345"))
    }

    @Test
    fun check_rejectsLocationOverLimit() {
        assertEquals(
            ProfileField.LOCATION,
            invalidField("Jane", location = "x".repeat(ProfileValidator.MAX_LOCATION_LENGTH + 1))
        )
    }
}
