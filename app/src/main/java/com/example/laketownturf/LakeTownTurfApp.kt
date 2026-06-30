package com.example.laketownturf

import android.app.Application
import com.example.laketownturf.utils.ThemePreference
import com.google.firebase.FirebaseApp

/**
 * Application class for Lake Town Turf.
 * Initializes Firebase and theme preferences on app startup.
 */
class LakeTownTurfApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        ThemePreference.init(this)
    }
}
