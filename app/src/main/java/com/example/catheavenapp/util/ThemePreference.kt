package com.example.catheavenapp.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.example.catheavenapp.domain.settings.ThemeMode

/**
 * Saves the Appearance choice on this phone and applies it to every screen.
 * It's a per-device setting, like the phone's own dark mode, so it isn't stored in Firestore.
 */
object ThemePreference {

    private const val PREFS = "cat_heaven_settings"
    private const val KEY_THEME = "theme_mode"

    fun current(context: Context): ThemeMode =
        ThemeMode.fromValue(prefs(context).getString(KEY_THEME, null))

    /** Saves [mode] and switches the app to it straight away. */
    fun save(context: Context, mode: ThemeMode) {
        prefs(context).edit().putString(KEY_THEME, mode.value).apply()
        apply(mode)
    }

    /** Called once when the app starts, before any screen is shown. */
    fun applySaved(context: Context) = apply(current(context))

    private fun apply(mode: ThemeMode) {
        AppCompatDelegate.setDefaultNightMode(
            when (mode) {
                ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
            }
        )
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
