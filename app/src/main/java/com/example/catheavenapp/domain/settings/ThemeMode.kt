package com.example.catheavenapp.domain.settings

/**
 * The supporter's choice under Settings > Appearance. [value] is what gets saved on the
 * phone, so it must never change once released; [label] is what the screen shows.
 */
enum class ThemeMode(val value: String, val label: String) {
    SYSTEM("system", "System default"),
    LIGHT("light", "Light"),
    DARK("dark", "Dark");

    companion object {
        /** Anything missing or unknown follows the phone's own setting. */
        fun fromValue(value: String?): ThemeMode =
            entries.firstOrNull { it.value == value } ?: SYSTEM
    }
}
