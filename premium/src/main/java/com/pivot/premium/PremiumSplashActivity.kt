package com.pivot.premium

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

@SuppressLint("CustomSplashScreen")
class PremiumSplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_premium_splash)

        findViewById<TextView>(R.id.premium_splash_name).text =
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName,0))

        findViewById<ImageView>(R.id.premium_splash_ic).background = applicationInfo.loadIcon(packageManager)

        CoroutineScope(Dispatchers.Main).launch {
            delay(getConfig("splash_delay", 3000))
            Premium.splashFinished()
            finish()
        }
    }
}