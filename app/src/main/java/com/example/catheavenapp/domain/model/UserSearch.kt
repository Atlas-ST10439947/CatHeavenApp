package com.example.catheavenapp.domain.model

/** Filters the admin's supporter directory by name or email. */
object UserSearch {
    fun filter(users: List<User>, query: String): List<User> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return users
        return users.filter { it.fullName.lowercase().contains(q) || it.email.lowercase().contains(q) }
    }
}
