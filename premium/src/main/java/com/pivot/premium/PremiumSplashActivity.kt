package com.pivot.premium

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

@SuppressLint("CustomSplashScreen")
open class PremiumSplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_premium_splash)

        findViewById<TextView>(R.id.premium_splash_name).text =
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName,0))

        findViewById<ImageView>(R.id.premium_splash_ic).background = applicationInfo.loadIcon(packageManager)

        CoroutineScope(Dispatchers.Main).launch {
            delay(getConfig("splash_delay", 2500))
            Premium.splashFinished()
            finish()
        }
    }
}