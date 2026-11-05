package com.example.catheavenapp.domain.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeModeTest {

    @Test
    fun savedValuesLoadBackAsTheSameChoice() {
        ThemeMode.entries.forEach { mode ->
            assertEquals(mode, ThemeMode.fromValue(mode.value))
        }
    }

    @Test
    fun nothingSavedYetFollowsThePhone() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromValue(null))
    }

    @Test
    fun unknownValueFollowsThePhone() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromValue("purple"))
    }

    @Test
    fun savedValuesNeverChange() {
        // Changing these would silently reset everyone's choice after an update
        assertEquals("system", ThemeMode.SYSTEM.value)
        assertEquals("light", ThemeMode.LIGHT.value)
        assertEquals("dark", ThemeMode.DARK.value)
    }
}
