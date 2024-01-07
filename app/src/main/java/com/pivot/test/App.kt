package com.pivot.test

import android.app.Application
import com.google.firebase.FirebaseApp
import com.pivot.premium.Premium
import com.pivot.premium.ads.AdManager

class App: Application() {

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        Premium.initialize(
            this,
            MainActivity::class.java,
            Premium.Configuration(
                bannerAdUnit = "ca-app-pub-3940256099942544/6300978111",
                interstitialAdUnit = "ca-app-pub-3940256099942544/1033173712",
                debug = BuildConfig.DEBUG,
                revenueCatAppId = ""//"goog_ZAZWrJwIprdwFTbMpsqbwWiIUFe"
            )
        )
    }
}