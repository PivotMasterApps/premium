package com.pivot.premium

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.view.animation.AlphaAnimation
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@SuppressLint("CustomSplashScreen")
open class PremiumSplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_premium_splash)
//        startAnimation()

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