package com.pivot.premium

import android.app.Application
import com.google.firebase.FirebaseApp
import com.pivot.premium.ads.AdManager

class App: Application() {

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        Premium.initialize(this, MainActivity::class.java)
    }
}