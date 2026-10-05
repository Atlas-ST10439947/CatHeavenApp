package com.example.catheavenapp.domain.booking

import com.example.catheavenapp.domain.model.User
import com.example.catheavenapp.domain.model.UserRole
import com.example.catheavenapp.domain.model.UserSearch
import org.junit.Assert.assertEquals
import org.junit.Test

class UserSearchTest {

    private fun user(name: String, email: String) =
        User(uid = name, fullName = name, email = email, phone = "", role = UserRole.SUPPORTER, dateJoinedMillis = 0L)

    private val users = listOf(
        user("Jane Smith", "jane@example.com"),
        user("Wiehan", "wiehan@gmail.com"),
        user("Courtney", "courtney@uni.ac.za")
    )

    @Test
    fun `empty search shows everyone`() {
        assertEquals(users, UserSearch.filter(users, "   "))
    }

    @Test
    fun `search matches names regardless of case`() {
        assertEquals(listOf("Jane Smith"), UserSearch.filter(users, "SMITH").map { it.fullName })
    }

    @Test
    fun `search matches email addresses`() {
        assertEquals(listOf("Courtney"), UserSearch.filter(users, "uni.ac.za").map { it.fullName })
    }

    @Test
    fun `search with no match returns nobody`() {
        assertEquals(emptyList<User>(), UserSearch.filter(users, "nobody"))
    }
}
