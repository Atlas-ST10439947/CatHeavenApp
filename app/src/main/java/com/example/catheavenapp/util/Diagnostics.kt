package com.example.catheavenapp.util

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Reports a handled failure (lost connection, permission denied, ...) to Logcat
 * and to Crashlytics as a non-fatal issue. Crashes are reported automatically;
 * this makes the errors supporters *see* on screen visible in the console too.
 */
object Diagnostics {
    fun warn(tag: String, message: String, error: Throwable) {
        Log.w(tag, message, error)
        runCatching {
            FirebaseCrashlytics.getInstance().apply {
                log("$tag: $message")
                recordException(error)
            }
        }
    }
}
