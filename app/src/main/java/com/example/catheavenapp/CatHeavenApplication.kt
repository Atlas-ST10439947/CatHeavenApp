package com.example.catheavenapp

import android.app.Application
import com.example.catheavenapp.util.ThemePreference

/** Runs before any screen, so the saved Appearance choice applies from the first frame. */
class CatHeavenApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ThemePreference.applySaved(this)
    }
}
