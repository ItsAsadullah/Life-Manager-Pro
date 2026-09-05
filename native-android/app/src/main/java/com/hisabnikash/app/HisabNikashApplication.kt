package com.hisabnikash.app

import android.app.Application
import com.google.firebase.FirebaseApp

class HisabNikashApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Returns null harmlessly until google-services.json is added during setup.
        FirebaseApp.initializeApp(this)
    }
}
