package com.pivot.test

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.pivot.premium.Premium

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.interstitial_btn).setOnClickListener {
            Premium.showInterstitial(this)
        }

        findViewById<Button>(R.id.premium_btn).setOnClickListener {
            Premium.showPremium()
        }

        findViewById<Button>(R.id.privacy_policy).setOnClickListener {
            Premium.showPrivacyActivity()
        }

        findViewById<Button>(R.id.rating).setOnClickListener {
            Premium.showRateUs(this)
        }
    }
}